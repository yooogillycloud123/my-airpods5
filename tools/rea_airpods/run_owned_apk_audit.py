#!/usr/bin/env python3
"""REA/JADX static audit of project-owned v139 LAB APK. No Android execution.

Raw REA Evidence remains in isolated GitHub runner temp dir. Only allowlisted
counters and provenance enter the public repository artifact/Action logs.
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import tempfile
import time
import zipfile
from pathlib import Path

OWN_PACKAGE = "com.myairpods5.personal.radiolab"
BASELINE_TAG = "my-airpods5-v139-one-lab-replace-r11"
REA_VERSION = "6.5.0"
JADX_VERSION = "0.7.1"
CLASSES = ("AirPodsService", "CaseRadioEvidenceV138", "CaseLabInventoryV139")

def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1048576), b""):
            digest.update(block)
    return digest.hexdigest()

def preflight_apk(apk: Path, digest: str) -> dict:
    if not re.fullmatch(r"[a-f0-9]{64}", digest):
        raise ValueError("expected SHA-256 must be lowercase hex")
    actual = sha256_file(apk)
    if actual != digest:
        raise ValueError("owned APK digest mismatch; analysis is denied")
    with zipfile.ZipFile(apk) as zf:
        names = zf.namelist()
        if not ("AndroidManifest.xml" in names and "classes.dex" in names):
            raise ValueError("owned file does not have a root APK manifest and DEX")
        dex_count = sum(re.fullmatch(r"classes\d*\.dex", n) is not None for n in names)
        if dex_count < 1:
            raise ValueError("APK DEX inventory missing")
    return {"sha256": actual, "dex_file_count": dex_count, "apk_bytes": apk.stat().st_size}

def cli_json(parts: list[str], timeout: int = 780) -> dict:
    # No shell, no target execution. Provider runs external JADX/JDK on owned APK.
    # Never reveal stdout/stderr if parsing fails: raw code can contain identifiers.
    r = subprocess.run(parts, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                       check=False, timeout=timeout, text=True)
    if r.returncode != 0:
        raise RuntimeError("REA CLI stage returned exit status " + str(r.returncode))
    try:
        obj = json.loads(r.stdout)
    except json.JSONDecodeError as exc:
        raise RuntimeError("REA JSON output invalid; raw report not printed") from exc
    if not isinstance(obj, dict):
        raise RuntimeError("REA tool returned unexpected JSON structure")
    # Some CLI replies may embed a domain-level error despite shell exit 0.
    if obj.get("isError") is True or obj.get("success") is False:
        raise RuntimeError("REA reported unsuccessful evidence")
    return obj

def run(apk: Path, expected: str, output: Path) -> dict:
    proof = preflight_apk(apk, expected)
    started = time.monotonic()
    # A real upstream REA/JADX analysis is required, not string/ZIP matching.
    package = cli_json(["rea", "inspect-android-package", str(apk), "--json"])
    encoded = json.dumps(package, sort_keys=True)
    if not (OWN_PACKAGE in encoded and "jadx" in encoded.lower()):
        raise RuntimeError("REA provider identity or exact owned package is unverified")

    found = {}
    for token in CLASSES:
        reply = cli_json(["rea", "search-android-classes", str(apk), token, "--json"])
        body = json.dumps(reply, sort_keys=True)
        found[token] = token in body
    if not found["AirPodsService"]:
        raise RuntimeError("Protected original service not present in REA class results")
    # Inspect a REAL DEX method, not only ZIP/Dex class names. The report below
    # records observed static facts and NEVER asserts case owner GATT support.
    oncreate = cli_json([
        "rea", "inspect-android-method", str(apk),
        "me.kavishdevar.librepods.services.AirPodsService", "onCreate", "--json",
    ])
    decompiled = json.dumps(oncreate, sort_keys=True)
    if "onCreate" not in decompiled:
        raise RuntimeError("REA did not verify expected source method identity")
    marker = "BACKGROUND_BLE_SCANNER_DISABLED_V139" in decompiled
    # Search false negatives remain unknown, not inferred absence.

    report = {
        "schema": "airpods-rea-static-v1",
        "analyzer": "morluto/rea",
        "rea_version_pinned": REA_VERSION,
        "jadx_headless_version_pinned": JADX_VERSION,
        "target_release": BASELINE_TAG,
        "target_package": OWN_PACKAGE,
        "target_sha256": proof["sha256"],
        "target_bytes": proof["apk_bytes"],
        "dex_file_count": proof["dex_file_count"],
        "rea_inspect_package": "PASS",
        "rea_airpods_service_search": "PASS",
        "rea_service_oncreate_static_method": "PASS",
        "rea_service_own_ble_scanner_disabled_marker": "OBSERVED_IN_STATIC" if marker else "NOT_OBSERVED",
        "rea_case_radio_search": "FOUND" if found["CaseRadioEvidenceV138"] else "NOT_OBSERVED",
        "rea_lab_inventory_search": "FOUND" if found["CaseLabInventoryV139"] else "NOT_OBSERVED",
        "apk_executed": False,
        "ble_scanned": False,
        "gatt_write_attempted": False,
        "case_owner_auth_verified": False,
        "closed_case_speaker_sound_verified": False,
        "case_speaker_command_recovered": False,
        "analysis_scope": "STATIC_JADX_ONLY_NO_RUNTIME_FIND_MY_PROOF",
        "elapsed_s": round(time.monotonic() - started, 1),
        "raw_evidence_uploaded": False,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    # GitHub artifact contains only explicit allowlisted fields, not entire REA JSON.
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                      encoding="utf-8")
    return report

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--expected-sha256", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = run(args.apk, args.expected_sha256, args.output)
    print("REA_AIRPODS_SOURCE_AUDIT=PASS")
    print("REA_OWN_APK_SHA256=" + result["target_sha256"])
    print("CASE_SPEAKER_BEEP=NOT_IMPLEMENTED")

if __name__ == "__main__":
    main()
