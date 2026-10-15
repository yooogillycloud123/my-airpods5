#!/usr/bin/env python3
"""OFFLINE btsnoop H4 ATT provenance audit. NEVER proves AirPods case ownership/sound.

Unlike the prior v119 cross-capture diff, clears characteristic UUID mappings and
encryption evidence when HCI handle is reused. A successful encrypted ATT WRITE
is still NOT a case-owner authentication or audible Find My case-beep.
No radio/network/Bluetooth operations. Raw ATT values and addresses are omitted.
"""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "case_protocol_v119"))
from extract_att_hci import AttExtractor, records

WRITES = {"WRITE_REQ", "WRITE_CMD", "PREPARE_WRITE_REQ", "SIGNED_WRITE_CMD"}

def control(packet: bytes):
    if len(packet) < 3 or packet[0] != 4 or len(packet) < packet[2] + 3:
        return None
    code, p = packet[1], packet[3:3 + packet[2]]
    if code == 0x05 and len(p) >= 4:
        return ("disconnect", int.from_bytes(p[1:3], "little") & 0xFFF, p[0] == 0)
    if code == 0x3E and len(p) >= 4 and p[0] in (1, 10):
        return ("connect", int.from_bytes(p[2:4], "little") & 0xFFF, p[1] == 0)
    if code == 0x08 and len(p) >= 4:
        return ("encrypted", int.from_bytes(p[1:3], "little") & 0xFFF, p[0] == 0 and p[3] == 1)
    if code == 0x30 and len(p) >= 3:
        return ("key_refresh", int.from_bytes(p[1:3], "little") & 0xFFF, p[0] == 0)
    return None

def clear_session(parser: AttExtractor, handle: int):
    # A 12-bit HCI handle can refer to an unrelated device after disconnect.
    for table in (parser.pending, parser.uuid_by_handle):
        for key in list(table):
            if key[0] == handle:
                table.pop(key, None)
    parser.pending_read_type.pop(handle, None)

def audit_file(path: Path) -> dict:
    parser = AttExtractor(include_private_values=False)
    epochs, encrypted, connected = defaultdict(int), defaultdict(bool), defaultdict(bool)
    writes = []
    controls = Counter()
    for frame, flags, packet in records(path):
        change = control(packet)
        if change:
            kind, handle, success = change
            controls[kind] += 1
            if kind in ("connect", "disconnect") and success:
                clear_session(parser, handle)
                epochs[handle] += 1
                encrypted[handle] = False
                connected[handle] = kind == "connect"
            elif kind == "encrypted":
                encrypted[handle] = success
            elif kind == "key_refresh" and not success:
                encrypted[handle] = False
        start = len(parser.events)
        parser.process(frame, flags, packet)
        for e in parser.events[start:]:
            if e["type"] not in WRITES or not e["connection"].startswith("hci_"):
                continue
            handle = int(e["connection"][4:], 16)
            writes.append({
                "frame": frame, "connection": e["connection"],
                "session_epoch": epochs[handle],
                "hci_connection_event_seen": connected[handle],
                "hci_link_encryption_observed": encrypted[handle],
                "action": e["type"],
                "characteristic_uuid_this_session": e.get("observed_uuid"),
                "payload_length": e.get("value_length"),
                "case_identity_verified": False,
                "owner_auth_verified": False,
                "case_speaker_beep_verified": False,
                "candidate_only": True
            })
    return {
        "classification": "OFFLINE_TRANSPORT_OBSERVATION_NOT_CASE_SOUND_PROOF",
        "case_identity": "UNKNOWN", "owner_authentication": "NOT_ESTABLISHED",
        "case_speaker_sound": "NOT_ESTABLISHED",
        "encrypted_transport_is_not_owner_auth": True,
        "contains_keys_addresses_or_raw_values": False,
        "hci_counts": dict(controls), "writes": writes,
    }

def difference(baseline: dict, attempted_sound: dict) -> dict:
    # Only compare characteristic UUIDs actually resolved in that SAME session.
    # Unmapped handles are NOT portable identifiers across different captures.
    def count(report):
        c = Counter()
        for e in report["writes"]:
            if (e["hci_connection_event_seen"] and
                e["hci_link_encryption_observed"] and
                e["characteristic_uuid_this_session"]):
                c[e["action"], e["characteristic_uuid_this_session"], e["payload_length"]] += 1
        return c
    before, after = count(baseline), count(attempted_sound)
    return {"case_beep_proof": False, "encrypted_link_not_owner_proof": True,
            "candidates_only": [
                {"action": key[0], "uuid": key[1], "payload_length": key[2],
                 "additional_writes": val - before[key]}
                for key, val in sorted(after.items()) if val > before[key]]}

def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("hci_btsnoop", type=Path)
    cli.add_argument("--baseline", type=Path)
    cli.add_argument("--output", type=Path)
    a = cli.parse_args()
    result = audit_file(a.hci_btsnoop)
    if a.baseline:
        result["comparison"] = difference(audit_file(a.baseline), result)
    report = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if a.output:
        with a.output.open("x", encoding="utf-8") as fp:
            fp.write(report)
    else:
        print(report)

if __name__ == "__main__":
    main()
