#!/usr/bin/env python3
"""Offline case-sound ATT write DIFFERENCE finder.

Only compares existing LOCAL HCI recordings (ordinary playback vs actual
case sound action). No hardware probing, no GATT writes or Apple account.
A difference is a CANDIDATE, never a verified AirPods case sound opcode.
"""
from __future__ import annotations

import argparse
from collections import Counter
import json
from pathlib import Path
from extract_att_hci import extract

WRITES = {"WRITE_REQ","WRITE_CMD","PREPARE_WRITE_REQ","SIGNED_WRITE_CMD"}

def observed_writes(report: dict) -> Counter:
    count: Counter = Counter()
    for event in report.get("events", []):
        if event.get("direction") != "tx" or event.get("type") not in WRITES:
            continue
        # Prefer observed characteristic UUID; GATT handles are session-local.
        uuid = event.get("observed_uuid", "UNMAPPED")
        # The numeric ATT handle is not a stable cross-session identifier.
        stable = "UUID:"+uuid if uuid != "UNMAPPED" else "UNMAPPED_HANDLE:"+event.get("attribute_handle","?")
        key = (event["type"], uuid, stable,
               event.get("value_length",0))
        count[key] += 1
    return count

def difference(baseline: dict, ring: dict) -> dict:
    a = observed_writes(baseline)
    b = observed_writes(ring)
    result = []
    for (kind,uuid,handle,length),count in sorted(b.items()):
        prev = a[(kind,uuid,handle,length)]
        if count > prev:
            result.append({
                "att_action":kind,
                "observed_characteristic_uuid":uuid,
                "comparison_key":handle,
                "value_length":length,
                "baseline_count":prev,
                "sound_action_capture_count":count,
                "count_increase":count-prev,
                "candidate_only":True,
            })
    return {
        "analysis": "structural difference of case sound action vs baseline",
        "actual_case_identity": "NOT_ATTESTED_BY_THIS_TOOL",
        "authenticated_owner_session": "NOT_ATTESTED_BY_THIS_TOOL",
        "physically_verified_case_beep": "NOT_ATTESTED_BY_THIS_TOOL",
        "false_positive_warning": "Observed writes may be background configuration or unrelated devices.",
        "possible_write_candidates":result
    }

def main() -> None:
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("baseline", type=Path)
    cli.add_argument("sound_action", type=Path)
    cli.add_argument("--output",type=Path,help="optional sanitized local JSON")
    args=cli.parse_args()
    report=difference(extract(args.baseline),extract(args.sound_action))
    data=json.dumps(report,ensure_ascii=False,indent=2)
    if args.output:
        if args.output.exists():raise SystemExit("Refusing to overwrite existing file")
        args.output.write_text(data+"\n", encoding="utf-8")
    else:print(data)

if __name__=="__main__":main()
