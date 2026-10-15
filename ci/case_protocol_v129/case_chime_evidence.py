#!/usr/bin/env python3
"""v129 Charging CASE chime / Find My command evidence classifier.

Scope: classify *documented* Apple AAP/AACP bytes, ATT control-point
metadata and a local physical USB power-edge report.
Does not connect, pair, scan, alter chime settings or play sounds.

Primary sources:
- LibrePods docs/AAP Definitions.md: 12 3A 00 01 00 08 [00/01]
  documented as enable/disable charging CASE chime; tested Pro 2 only.
  Maintainer issue #102: the real operation is CASE GATT and needs owner auth;
  reported AAP writes to buds did NOTHING.
- CAPod DefaultAapDeviceProfile.kt: 0x31 "IN_CASE_TONE" is NOT case
  charging-sound control; effect unknown; actual case tones use ATT.
- OpenTagViewer #201: 4F860002 owner CP, 4F860003 non-owner CP applies
  to a THIRD-PARTY Find My accessory, not proven for AirPods 5.
- AirGuard AppleFindMy.kt: nonowner sound only suitable tracker state.
- User A3530 physical A/B: USB replug and stationary USB with wall
  power reset generate 'ding' once light fully dark. Not a BT command.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path

CLASSIFICATION={
    "CASE_CHIME_SETTING_HISTORIC":{
        "effect":"enable_or_disable_future_charging_tones_NOT_PLAY_NOW",
        "transport":"case GATT expected by LibrePods maintainer; historical document not directly wire-ready",
        "model_validation":"AirPods Pro 2 documentation only; AirPods 5 A3530 unproven",
        "can_emit_unplugged_case_beep":False,
    },
    "EARBUD_IN_CASE_TONE":{
        "effect":"unknown AACP setting; CAPod explicitly denies equivalence to charging case sound",
        "transport":"earbud AAP/AACP",
        "model_validation":"not an A3530 case speaker command",
        "can_emit_unplugged_case_beep":False,
    },
    "OTHER_ACCESSORY_FINDMY_CP":{
        "effect":"potential Find My sound in correct non-owner/owner context",
        "transport":"BLE GATT",
        "model_validation":"third party accessory or separated device only; NO AirPods5 A3530 case proof",
        "can_emit_unplugged_case_beep":False,
    },
    "USB_VBUS_RECONNECT_PHYSICAL":{
        "effect":"physical charge start chime observed by user",
        "transport":"real USB-C power edge, not a Bluetooth protocol packet",
        "model_validation":"direct A3530 hardware report: A/B; only once indicator fully dark",
        "can_emit_unplugged_case_beep":False,
    },
    "UNKNOWN":{
        "effect":"unknown",
        "transport":"unknown",
        "model_validation":"none",
        "can_emit_unplugged_case_beep":False,
    },
}

def classify_aap(frame:bytes)->dict:
    category="UNKNOWN"
    detail=""
    if len(frame)==7 and frame[:6]==bytes.fromhex("12 3A 00 01 00 08") and frame[6] in (0,1):
        category="CASE_CHIME_SETTING_HISTORIC"
        detail="historical setting ON" if frame[6]==0 else "historical setting OFF"
    elif len(frame)>=8 and frame[:7]==bytes.fromhex("04 00 04 00 09 00 31"):
        category="EARBUD_IN_CASE_TONE"
        detail="control ID 0x31; implementation effect unknown"
    return {"category":category,"detail":detail,
            "len":len(frame),**CLASSIFICATION[category],
            "case_identity_verified":False,"case_owner_auth_verified":False,
            "physical_case_beep_from_this_frame_verified":False,
            "can_be_used_as_case_speaker_play_command":False}

def classify_uuid(uuid:str)->dict:
    u=uuid.lower().replace("-","")
    code="OTHER_ACCESSORY_FINDMY_CP" if u in (
        "4f860002943b49efbed42f730304427a",
        "4f860003943b49efbed42f730304427a") else "UNKNOWN"
    detail="owner CP (other accessory)" if u.startswith("4f860002") else (
        "non-owner CP (other accessory)" if u.startswith("4f860003") else "unknown")
    return {"category":code,"detail":detail,**CLASSIFICATION[code],
            "case_identity_verified":False,"case_owner_auth_verified":False,
            "physical_case_beep_from_this_command_verified":False,
            "can_be_used_as_case_speaker_play_command":False}

def physical_usb_event()->dict:
    cat="USB_VBUS_RECONNECT_PHYSICAL"
    return {"category":cat,**CLASSIFICATION[cat],
        "direct_user_observation":True,
        "case_has_charger_attached":True,
        "speaker_sound_confirmed_only_during_real_power_event":True,
        "condition":"OFF until the case indicator becomes fully dark, then ON",
        "estimated_fade_s":"1-2 observed but use actual LED rather than a guessed delay",
        "does_not_establish_BLE_trigger":True}

def safe_report(aap_hexes:list[str], uuids:list[str])->dict:
    result={
      "acceptance_target":"Galaxy Android without charger triggers AirPods5 A3530 CASE sound",
      "target_status":"NO_VALIDATED_BLUETOOTH_PLAYBACK_COMMAND",
      "charging_chime_setting_is_not_immediate_play":True,
      "aap_frames":[classify_aap(bytes.fromhex(s)) for s in aap_hexes],
      "gatt_uuids":[classify_uuid(s) for s in uuids],
      "physical_charging_observation":physical_usb_event(),
      "case_speaker_play_implemented":False,
      "no_bluetooth_transmission_or_settings_write":True,
    }
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--aap",action="append",default=[],help="Public AAP frame hex (not keys)")
    p.add_argument("--uuid",action="append",default=[],help="Public GATT characteristic UUID")
    p.add_argument("--output",type=Path,help="Local new JSON filename (never overwritten)")
    a=p.parse_args()
    report=safe_report(a.aap,a.uuid)
    value=json.dumps(report,ensure_ascii=False,indent=2)+"\n"
    if a.output:a.output.write_text(value,encoding="utf8") if not a.output.exists() else p.error("Refuse to overwrite")
    else:print(value)

if __name__=="__main__":main()
