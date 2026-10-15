#!/usr/bin/env python3
"""Offline Bluetooth HCI trace -> ATT evidence extractor.

Real format: Android btsnoop H4, linktype 1002. GATT/ATT: L2CAP CID 0x0004.
No radio, root, device scans, remote writes, or Apple credentials.
NEVER imply an ATT write succeeded in ringing the user's AirPods 5 case.
Source semantics: Android btsnoop.cc and Bluetooth Core ATT specification.
"""
from __future__ import annotations

import argparse
import json
import os
from collections import Counter
from pathlib import Path
import struct
from typing import Iterator

MAGIC = b"btsnoop\0"
HEADER = struct.Struct(">IIIIQ")
BOUNDS = 32 * 1024 * 1024
MAX_RECORDS = 2_000_000
ATT_NAMES = {
    0x01: "ERROR_RESPONSE", 0x04: "FIND_INFORMATION_REQ",
    0x05: "FIND_INFORMATION_RSP", 0x08: "READ_BY_TYPE_REQ",
    0x09: "READ_BY_TYPE_RSP", 0x0A: "READ_REQ",
    0x0B: "READ_RSP", 0x10: "READ_BY_GROUP_REQ",
    0x11: "READ_BY_GROUP_RSP", 0x12: "WRITE_REQ",
    0x13: "WRITE_RSP", 0x16: "PREPARE_WRITE_REQ",
    0x17: "PREPARE_WRITE_RSP", 0x18: "EXECUTE_WRITE_REQ",
    0x19: "EXECUTE_WRITE_RSP", 0x1B: "NOTIFY",
    0x1D: "INDICATE", 0x1E: "CONFIRM", 0x52: "WRITE_CMD",
    0xD2: "SIGNED_WRITE_CMD",
}

def records(path: Path) -> Iterator[tuple[int, int, bytes]]:
    with path.open("rb") as fp:
        header = fp.read(16)
        if len(header) != 16 or header[:8] != MAGIC:
            raise ValueError("Not a btsnoop H4 log")
        version, link_type = struct.unpack(">II", header[8:])
        if version != 1 or link_type != 1002:
            raise ValueError("Only version 1 H4 (link type 1002) supported")
        for frame in range(1, MAX_RECORDS + 1):
            rh = fp.read(24)
            if not rh:
                return
            if len(rh) != 24:
                raise ValueError("Truncated btsnoop record header")
            original, captured, flags, _drops, _timestamp = HEADER.unpack(rh)
            if captured > BOUNDS or captured > original:
                raise ValueError("Corrupt btsnoop record length")
            data = fp.read(captured)
            if len(data) != captured:
                raise ValueError("Truncated btsnoop record")
            yield frame, flags, data
        raise ValueError("Record count above safe limit")


class AttExtractor:
    def __init__(self, include_private_values: bool = False):
        self.include_private_values = include_private_values
        self.pending: dict[tuple[int, str], tuple[int, int, bytes]] = {}
        self.counter = Counter()
        self.events: list[dict] = []
        self.uuid_by_handle: dict[tuple[int,int],str] = {}
        self.pending_read_type: dict[int,int] = {}

    def add_event(self, frame: int, handle: int, direction: str,
                  event: str, **fields: object) -> None:
        entry = {"frame": frame, "connection": "hci_%04X" % handle,
                 "direction": direction, "type": event}
        entry.update(fields)
        self.events.append(entry)
        self.counter[event] += 1

    def process(self, frame: int, flags: int, raw: bytes) -> None:
        if not raw:
            return
        typ = raw[0]
        if typ == 4 and len(raw) >= 3:  # HCI events, not ATT
            code, length = raw[1], raw[2]
            if len(raw) < 3 + length:
                self.counter["truncated_HCI_event"] += 1
                return
            p = raw[3:3 + length]
            if code == 0x08 and len(p) >= 4:  # Encryption Change
                handle = struct.unpack_from("<H", p, 1)[0]
                self.add_event(frame, handle, "controller", "ENCRYPTION_CHANGE",
                               status=p[0], enabled=p[3] == 1)
            elif code == 0x30 and len(p) >= 3:
                handle = struct.unpack_from("<H", p, 1)[0]
                self.add_event(frame, handle, "controller", "ENCRYPTION_KEY_REFRESH",
                               status=p[0])
            # Do not expose LE address/identity or key material in trace reports.
            return
        if typ != 2 or len(raw) < 5:
            return
        acl_handle, acl_len = struct.unpack_from("<HH", raw, 1)
        handle, pb = acl_handle & 0x0FFF, (acl_handle >> 12) & 3
        payload = raw[5:]
        if len(payload) != acl_len:
            self.counter["truncated_ACL"] += 1
            return
        direction = "rx" if (flags & 1) else "tx"
        key = (handle, direction)
        if pb == 1:
            state = self.pending.get(key)
            if state is None:
                self.counter["orphan_ACL_fragment"] += 1
                return
            total, cid, data = state
            new = data + payload
            if len(new) > BOUNDS:
                del self.pending[key]
                self.counter["ACL_oversize"] += 1
                return
            if len(new) >= total:
                del self.pending[key]
                self.l2cap(frame, handle, direction, cid, new[:total])
            else:
                self.pending[key] = (total, cid, new)
            return
        if pb not in (0, 2, 3):
            self.counter["unknown_packet_boundary"] += 1
            return
        if len(payload) < 4:
            self.counter["short_L2CAP"] += 1
            return
        total, cid = struct.unpack_from("<HH", payload)
        if total > BOUNDS:
            self.counter["oversize_L2CAP"] += 1
            return
        data = payload[4:]
        if len(data) < total:
            self.pending[key] = (total, cid, data)
        else:
            self.l2cap(frame, handle, direction, cid, data[:total])

    @staticmethod
    def parse_uuid(raw: bytes) -> str:
        if len(raw) == 2:
            return "0x%04X" % int.from_bytes(raw, "little")
        if len(raw) == 16:
            h = raw[::-1].hex()
            return "-".join((h[:8], h[8:12], h[12:16], h[16:20], h[20:]))
        return "unknown_uuid"

    def l2cap(self, frame: int, handle: int, direction: str,
              cid: int, data: bytes) -> None:
        if cid != 4 or not data:
            return  # Ignore SMP (CID 6), dynamic/private channels, audio.
        opcode = data[0]
        name = ATT_NAMES.get(opcode, "ATT_UNKNOWN_%02X" % opcode)
        fields = {"opcode": "0x%02X" % opcode}
        if opcode in (0x12, 0x52, 0xD2, 0x16, 0x17, 0x0A, 0x1B, 0x1D):
            if len(data) < 3:
                self.counter["short_ATT_value"] += 1
                return
            attribute = int.from_bytes(data[1:3], "little")
            fields["attribute_handle"] = "0x%04X" % attribute
            if (handle, attribute) in self.uuid_by_handle:
                fields["observed_uuid"] = self.uuid_by_handle[(handle,attribute)]
            value = data[5:] if opcode in (0x16, 0x17) else data[3:]
            fields["value_length"] = len(value)
            if self.include_private_values:
                fields["private_value_hex"] = value.hex()
        elif opcode in (0x08, 0x10):
            if len(data) >= 7:
                fields["range"] = [
                    "0x%04X" % int.from_bytes(data[1:3], "little"),
                    "0x%04X" % int.from_bytes(data[3:5], "little")
                ]
                fields["requested_type_uuid16"] = "0x%04X" % int.from_bytes(data[5:7], "little")
                if opcode == 0x08:
                    self.pending_read_type[handle] = int.from_bytes(data[5:7], "little")
        elif opcode == 0x01 and len(data) >= 5:
            fields["error_for_opcode"] = "0x%02X" % data[1]
            fields["attribute_handle"] = "0x%04X" % int.from_bytes(data[2:4], "little")
            fields["att_error"] = "0x%02X" % data[4]
        elif opcode == 0x05 and len(data) >= 2:
            # Do not assume the returned handles belong to an AirPods case.
            fields["format"] = data[1]
            fields["records_length"] = len(data) - 2
            row = {1: 4, 2: 18}.get(data[1], 0)
            if row and (len(data)-2) % row == 0:
                for i in range(2, len(data), row):
                    att = int.from_bytes(data[i:i+2],"little")
                    self.uuid_by_handle[(handle,att)] = self.parse_uuid(data[i+2:i+row])
        elif opcode == 0x09 and len(data) >= 2:
            fields["attribute_row_size"] = data[1]
            fields["records_length"] = len(data) - 2
            row = data[1]
            if row in (7,21) and self.pending_read_type.pop(handle,None) == 0x2803 and (len(data)-2) % row == 0:
                for i in range(2,len(data),row):
                    att=int.from_bytes(data[i+3:i+5],"little")
                    self.uuid_by_handle[(handle,att)] = self.parse_uuid(data[i+5:i+row])
        elif opcode == 0x11 and len(data) >= 2:
            fields["service_row_size"] = data[1]
            fields["records_length"] = len(data) - 2
            row = data[1]
            if row in (6,20) and (len(data)-2) % row == 0:
                fields["observed_services"]=[]
                for i in range(2,len(data),row):
                    fields["observed_services"].append({
                        "start":"0x%04X" % int.from_bytes(data[i:i+2],"little"),
                        "end":"0x%04X" % int.from_bytes(data[i+2:i+4],"little"),
                        "uuid":self.parse_uuid(data[i+4:i+row])
                    })
        self.add_event(frame, handle, direction, name, **fields)

    def result(self) -> dict:
        return {
            "origin": "HCI log; device identity NOT verified",
            "case_identity": "UNKNOWN",
            "case_owner_authentication": "UNKNOWN",
            "case_sound_effect": "NOT_OBSERVED",
            "raw_values_exported": self.include_private_values,
            "counters": dict(self.counter),
            "discovered_handle_uuid_map_not_verified_case": [
                {"connection":"hci_%04X" % conn, "handle":"0x%04X" % h, "uuid":uuid}
                for (conn,h),uuid in sorted(self.uuid_by_handle.items())
            ],
            "events": self.events,
        }


def extract(path: Path, private: bool = False) -> dict:
    parser = AttExtractor(private)
    for frame, flags, data in records(path):
        parser.process(frame, flags, data)
    parser.counter["incomplete_L2CAP_at_end"] = len(parser.pending)
    return parser.result()


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("input", type=Path, help="existing LOCAL btsnoop_hci.log; not uploaded")
    ap.add_argument("--output", type=Path, default=None)
    ap.add_argument("--private-values", action="store_true",
                    help="DANGER: include ATT values locally; NEVER post results")
    args = ap.parse_args()
    if args.private_values and args.output is None:
        ap.error("--private-values requires --output for local storage")
    if args.private_values:
        print("Warning: private ATT values may contain secrets. Keep the output offline.", flush=True)
    result = extract(args.input, args.private_values)
    rendered = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output is None:
        print(rendered)
    else:
        if args.output.exists():
            raise SystemExit("Refusing to overwrite existing output file")
        # Local output, no network calls; private data restricted to file owner.
        fd = os.open(args.output,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,"w",encoding="utf-8") as stream:
            stream.write(rendered + "\n")
        print("Stored LOCAL diagnostic output; this does not establish case ownership or sound playback.")


if __name__ == "__main__":
    main()
