#!/usr/bin/env python3
"""Apply v124 source-only case advertiser screen to v122 LAB, never to Golden."""
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1])
repo=Path(sys.argv[2])
src=repo/"ci/case_protocol_v124/CaseResearchMenuV124.kt"
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
target=base/"CaseResearchMenuV124.kt"
assert src.exists() and not target.exists()
shutil.copyfile(src,target)
settings=base/"presentation/screens/AppSettingsScreen.kt"
s=settings.read_text(encoding="utf8")
old="me.kavishdevar.librepods.CaseOwnerProbeV120.open(it)"
assert s.count(old)==1,(old,s.count(old))
s=s.replace(old,"me.kavishdevar.librepods.CaseResearchMenuV124.open(it)",1)
old='name = "내 케이스 제어 경로 확인 · 연구"'
assert s.count(old)==1, (old,s.count(old))
s=s.replace(old,'name = "내 케이스 찾기 연구 v1.0.24"',1)
settings.write_text(s,encoding="utf8")
g=root/"android/app/build.gradle.kts"
v=g.read_text(encoding="utf8")
assert v.count('applicationId = "com.myairpods5.personal.lab"')==1
assert v.count('versionCode = 10022')==1
assert v.count('versionName = "1.0.22-lab-irk-and-battery-recovery"')==1
v=v.replace('versionCode = 10022','versionCode = 10024',1)
v=v.replace('versionName = "1.0.22-lab-irk-and-battery-recovery"',
            'versionName = "1.0.24-independent-case-advert-lab"',1)
g.write_text(v,encoding="utf8")
p=target.read_text("utf8")
assert 'CaseOwnerProbeV120.open(a)' in p
assert 'getString("ENC_KEY",null)' in p
assert 'cipher.doFinal(data.copyOfRange(3,19))' in p
assert '10_000L' in p
for forbidden in ['writeCharacteristic','createBond(','sendRequestProximityKeys','sendDataPacket(','startSound(']:
    assert forbidden not in p,forbidden
print("V124_STANDALONE_GOLDEN_UNTOUCHED=PASS")
print("V124_PRESERVE_PREVIOUS_GATT_READ_ONLY_MENU=PASS")
print("V124_NO_SOUND_PROTCOL_NO_WRITES=PASS")
