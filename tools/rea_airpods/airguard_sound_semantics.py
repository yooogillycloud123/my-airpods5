#!/usr/bin/env python3
"""Derive narrow sound-path facts from pinned upstream open-source Kotlin.

Never initiates BLE or writes to Android devices.  Release artifacts contain
provenance and constant *names* but not private user device data.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
from pathlib import Path

def masked_match(frame: bytes, value: bytes, mask: bytes) -> bool:
    if len(frame) < len(value) or len(value) != len(mask):
        return False
    return all((a & m) == (b & m) for a,b,m in zip(frame,value,mask))

def inspect_sources(airpods: str, findmy: str) -> dict:
    def has(source: str, snippet: str) -> bool:
        return snippet in source

    must = (
       ("AirPods scan is Apple manufacturer id",has(airpods,"setManufacturerData(") and has(airpods,"0x4C")),
       ("AirPods wildcard status mask",has(airpods,"(0x00).toByte(), (0x18).toByte()")),
       ("FindMy first priority",has(airpods,"listOf(SoundProtocol.FINDMY, SoundProtocol.DULT, SoundProtocol.AIRTAG)")),
       ("FindMy FD44",has(findmy,'FINDMY_SOUND_SERVICE = "fd44"')),
       ("FindMy nonowner characteristic",has(findmy,'"4F860003-943B-49EF-BED4-2F730304427A"')),
       ("FindMy nonowner start bytes",has(findmy,"byteArrayOf(0x01, 0x00, 0x03)")),
       ("FindMy nonowner stop bytes",has(findmy,"byteArrayOf(0x01, 0x01, 0x03)")),
       ("FindMy start path GATT",has(findmy,"writeCharacteristic(gatt, characteristic, FINDMY_START_SOUND_OPCODE")),
       ("FindMy per-session attempt flag",has(findmy,"soundStarted = true")),
    )
    if not all(v for _,v in must):
        raise ValueError("official tagged source lacks expected contract elements; refuse to infer protocol")
    mask=bytes((0xff,0x00,0x18));value=bytes((0x12,0x19,0x18))
    assert masked_match(bytes((0x12,0x02,0x18)),value,mask)
    assert masked_match(bytes((0x12,0x19,0x18)),value,mask)
    assert not masked_match(bytes((0x12,0x02,0x00)),value,mask)
    return {
        "schema":"rea-airguard-tagged-source-contract-v1",
        "tag":"seemoo-lab/AirGuard@3.1.3",
        "source_sha256_airpods":hashlib.sha256(airpods.encode()).hexdigest(),
        "source_sha256_applefindmy":hashlib.sha256(findmy.encode()).hexdigest(),
        "checked_contracts":{k:bool(v) for k,v in must},
        "apple_manufacturer_id":"0x004C",
        "airpods_broadcast_filter_value":"12 19 18",
        "airpods_broadcast_filter_mask":"FF 00 18",
        "airpods_filter_can_match_12_02_when_third_byte_18":True,
        "airpods_filter_can_match_12_19_when_third_byte_18":True,
        "third_byte_from_user_v139_counter_available":False,
        "fd44_nonowner_characteristic":"4F860003",
        "fd44_nonowner_start_opcode":"01 00 03",
        "fd44_nonowner_stop_opcode":"01 01 03",
        "fd44_sound_protocol_in_public_source":True,
        "fd44_user_wireless_case_auth_proven":False,
        "fd44_case_sound_observed_by_user":False,
        "gatt_write_ack_implies_physical_case_sound":False,
        "apple_findmy_owner_case_credential_available":False,
        "retest_user_ble":False,
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--airpods",type=Path,required=True)
    p.add_argument("--applefindmy",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    a=p.parse_args()
    result=inspect_sources(a.airpods.read_text(),a.applefindmy.read_text())
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n")
    print("AIRGUARD_PUBLIC_SOURCE_SOUND_CONTRACT=PASS")
    print("AIRPODS_AD_MASK_WILDCARD_SECOND_BYTE=PASS")
    print("FD44_NONOWNER_SOUND_VS_OWN_CASE_AUTH=SEPARATE")
if __name__=="__main__":main()
