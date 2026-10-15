#!/usr/bin/env python3
"""Integrate the reviewed pure Kotlin case RPA primitive into LAB ONLY.

This introduces no UI, runtime Bluetooth scan, GATT connection, or command.
The verified v1.0.10 product and all runtime functions remain immutable.
"""
from pathlib import Path
import shutil
import sys

root = Path(sys.argv[1])
repo = Path(sys.argv[2])
src = repo / "ci/case_protocol_v117/CaseRpaIdentity.kt"
dst = root / "android/app/src/main/java/caseproto/CaseRpaIdentity.kt"
assert src.is_file(), "MISSING_CASE_IDENTITY_REFERENCE"
assert not dst.exists(), "DO_NOT_OVERWRITE_EXISTING_SOURCE"
dst.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(src, dst)
code = dst.read_text("utf-8")
assert "MATCHES_KEY_BUT_CASE_IDENTITY_UNPROVEN" in code
assert "CASE_OWNER_HANDSHAKE_VERIFIED" in code
assert "connectGatt" not in code and "startScan" not in code
gradle = root/"android/app/build.gradle.kts"
s=gradle.read_text("utf-8")
assert s.count('applicationId = "com.myairpods5.personal.lab"') == 1
assert s.count("versionCode = 10016") == 1
assert s.count('versionName = "1.0.16-findmy-rssi"') == 1
s=s.replace("versionCode = 10016", "versionCode = 10017")
s=s.replace('versionName = "1.0.16-findmy-rssi"', 'versionName = "1.0.17-owner-identity-source-only"')
gradle.write_text(s,"utf-8")
print("CASE_RPA_ANDROID_LAB_MODULE_COPIED=PASS")
print("OWNER_SOUND_PROTOCOL_ENABLED=NO")
print("GOLDEN_PACKAGE_UNMODIFIED=PASS")
