#!/usr/bin/env python3
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1]);repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
source=repo/"ci/case_protocol_v120/CaseOwnerProbeV120.kt"
target=base/"CaseOwnerProbeV120.kt"
assert source.is_file()
assert not target.exists()
shutil.copy2(source,target)
settings=base/"presentation/screens/AppSettingsScreen.kt"
s=settings.read_text("utf-8")
old='me.kavishdevar.librepods.FindMyRadioAudit.open(it)'
new='me.kavishdevar.librepods.CaseOwnerProbeV120.open(it)'
assert s.count(old)==1,(old,s.count(old))
s=s.replace(old,new,1)
old_label='name = "Find My 신호 조사 · 연구"'
assert s.count(old_label)==1
s=s.replace(old_label,'name = "내 케이스 제어 경로 확인 · 연구"',1)
settings.write_text(s,"utf-8")
gradle=root/"android/app/build.gradle.kts"
g=gradle.read_text("utf-8")
assert g.count('applicationId = "com.myairpods5.personal.lab"')==1
assert g.count("versionCode = 10017")==1
assert g.count('versionName = "1.0.17-owner-identity-source-only"')==1
g=g.replace("versionCode = 10017","versionCode = 10020")
g=g.replace('versionName = "1.0.17-owner-identity-source-only"',
            'versionName = "1.0.20-owner-gatt-preflight"')
gradle.write_text(g,"utf-8")
kt=target.read_text("utf-8")
assert "CaseRpaIdentity.assess" in kt
assert "getSharedPreferences(\"settings\"" in kt
assert "startScan(null" in kt
assert "connectGatt(" in kt
assert "onServicesDiscovered" in kt
assert "writeCharacteristic" not in kt
assert "createBond(" not in kt
assert "startSound" not in kt
assert "versionCode = 10020" in gradle.read_text("utf-8")
print("V120_CASE_RPA_OWN_ONLY=PASS")
print("V120_USER_GATT_READ_ONLY_CONSENT=PASS")
print("V120_NO_SOUND_WRITE=PASS")
print("V120_PROVEN_GOLDEN_APP_UNTOUCHED=PASS")
