#!/usr/bin/env python3
"""Apply isolated v1.0.15 Apple Find My advertisement audit after v1.0.14."""
from pathlib import Path
import shutil, sys

root=Path(sys.argv[1])
repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
audit=base/"FindMyRadioAudit.kt"
if audit.exists():
    raise RuntimeError("AUDIT_ALREADY_EXISTS")
shutil.copyfile(repo/"ci/FindMyRadioAudit_v115.kt",audit)
settings=base/"presentation/screens/AppSettingsScreen.kt"
old=settings.read_text(encoding="utf-8")
needle="me.kavishdevar.librepods.CaseBeaconFinder.open(it)"
assert old.count(needle)==1
old=old.replace(needle,"me.kavishdevar.librepods.FindMyRadioAudit.open(it)",1)
label='name = "케이스 소리 시험 · 연구"'
assert old.count(label)==1
old=old.replace(label,'name = "Find My 신호 조사 · 연구"',1)
settings.write_text(old,encoding="utf-8")

gradle=root/"android/app/build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count('versionCode = 10014')==1
assert g.count('versionName = "1.0.14-unfiltered-case-diagnostics"')==1
assert g.count('applicationId = "com.myairpods5.personal.lab"')==1
g=g.replace("versionCode = 10014","versionCode = 10015",1)
g=g.replace('versionName = "1.0.14-unfiltered-case-diagnostics"','versionName = "1.0.15-findmy-radio-audit"',1)
gradle.write_text(g,encoding="utf-8")

a=audit.read_text(encoding="utf-8")
assert "0x12" in a and "0x18" in a and "0000fd44-" in a
assert "connectGatt" not in a and "writeCharacteristic" not in a
assert "getManufacturerSpecificData(76)" in a
assert "getManufacturerSpecificData(76)" in a
assert 'android.permission' not in a # read-only via Android Manifest permissions
print("V115_FINDMY_PASSIVE_RADIO_ONLY=PASS")
print("V115_GOLDEN_PERSONAL_APP_PACKAGE_PRESERVED=PASS")
print("V115_USER_PHYSICAL_BEEP=UNKNOWN")
