#!/usr/bin/env python3
"""Run real REA+JADX static comparison on a public open-source tracking app.

Only SHA pinned official APKs are accepted. No Android connections, no GATT
writes, no guessed ownership, no collection of local phone identifiers.
The complete decompiler return is never uploaded; output is allowlisted.
"""
import argparse
import json
import re
import time
from pathlib import Path
from run_owned_apk_audit import cli_json, preflight_apk

TARGET = "seemoo-lab/AirGuard"
TARGET_RELEASE = "3.1.3"
CLASS = "de.seemoo.at_tracking_detection.database.models.device.types.AppleFindMy"
CLASS_PODS = "de.seemoo.at_tracking_detection.database.models.device.types.AirPods"
METHODS = ("tryStartProtocol", "stopSound", "writeCharacteristicRaw")
MODEL = "AirPods 5 A3530 wireless case, not earbud"

def classify_case_proof() -> dict:
    # Session evidence is the user's actual v139 screenshot, not inference.
    # BLE 12:02 13 packets from five ephemeral addresses is UNBOUND, not
    # proof of five distinct physical devices or even a case advertisement.
    return {
        "user_v139_ble_callbacks":307,
        "user_v139_apple_callbacks":29,
        "user_v139_findmy_nearby_1202":13,
        "user_v139_temp_addresses_observed":5,
        "user_v139_findmy_separated_1219_connectable":0,
        "user_v139_findmy_separated_1219_nonconnectable":0,
        "user_v139_airpods_0719":0,
        "user_v136_one_off_fd44_nonowner":True,
        "user_v136_candidate_bound_to_own_a3530":False,
        "case_ownership_and_gatt_auth_proven":False,
        "closed_case_beep_proven":False,
        "reason":"Anonymous separated-tracker GATT is not verified as owned A3530 case",
    }

def analyze(apk:Path, digest:str, out:Path)->dict:
    p=preflight_apk(apk,digest)
    start=time.monotonic()
    pkg=cli_json(["rea","inspect-android-package",str(apk),"--json"])
    text=json.dumps(pkg,sort_keys=True)
    if not ("jadx" in text.lower()):
        raise RuntimeError("REA did not expose JADX provider identity")
    names = {}
    for token in ("AppleFindMy", "AirPods"):
        result=cli_json(["rea","search-android-classes",str(apk),token,"--json"])
        raw=json.dumps(result)
        names[token] = token in raw
    method_results={}
    if names.get("AppleFindMy"):
        for method in METHODS:
            try:
                reply=cli_json(["rea","inspect-android-method",str(apk),CLASS,method,"--json"],timeout=520)
                raw=json.dumps(reply)
                method_results[method]="CODE_OBSERVED" if method in raw else "METHOD_OUTPUT_UNKNOWN"
            except (RuntimeError,TimeoutError) as exc:
                method_results[method]="NOT_VERIFIABLE_WITH_THIS_APK"
    else:
        method_results={k:"PROGUARD_OR_MISSING_CLASS_UNRESOLVED" for k in METHODS}
    report={
        "schema":"rea-airpods-open-source-comparative-v1",
        "target_repository":TARGET,
        "target_public_release":TARGET_RELEASE,
        "apk_sha256":p["sha256"],
        "apk_size":p["apk_bytes"],
        "dex_files":p["dex_file_count"],
        "rea_jadx_package_inspection":"PASS",
        "class_applefindmy_search":"FOUND" if names["AppleFindMy"] else "NOT_OBSERVED",
        "class_airpods_search":"FOUND" if names["AirPods"] else "NOT_OBSERVED",
        "methods":method_results,
        "user_ble_evidence":classify_case_proof(),
        "documented_nonowner_path":"AirGuard AppleFindMy sound protocols DULT, FD44 and AirTag; this is NOT Apple own-case owner auth.",
        "model_scope":MODEL,
        "claim_authenticated_case_findmy_start_stop":False,
        "claim_user_closed_a3530_beep":False,
        "live_phone_or_bluetooth_operation":False,
        "compiled_code_copied_to_result":False,
        "raw_decompilation_released":False,
        "elapsed_s":round(time.monotonic()-start,1),
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
    return report

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--apk",type=Path,required=True)
    p.add_argument("--sha256",required=True)
    p.add_argument("--output",type=Path,required=True)
    a=p.parse_args()
    r=analyze(a.apk,a.sha256,a.output)
    print("REA_FINDMY_COMPARATIVE_STATIC_AUDIT=PASS")
    print("APK_SHA256="+r["apk_sha256"])
    print("APK_DEX_COUNT="+str(r["dex_files"]))
    print("OWN_A3530_CASE_BEEP=NOT_PROVEN")
if __name__=="__main__":main()
