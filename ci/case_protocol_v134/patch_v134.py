#!/usr/bin/env python3
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1]); repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
settings=base/"presentation/screens/AppSettingsScreen.kt"
build=root/"android/app/build.gradle.kts"
def only(f,before,after):
    s=f.read_text("utf-8")
    assert s.count(before)==1,(f,s.count(before))
    f.write_text(s.replace(before,after,1),"utf-8")
shutil.copy2(repo/"ci/case_protocol_v134/CasePathV134.kt",base/"CasePathV134.kt")
only(settings,"me.kavishdevar.librepods.CaseSoundStatusV125.open(it)","me.kavishdevar.librepods.CasePathV134.open(it)")
only(settings,'name = "내 AirPods 5 케이스 찾기 · 개발 상태"','name = "내 케이스 소리 경로 확인 · LAB"')
only(build,"versionCode = 10026","versionCode = 10034")
only(build,'versionName = "1.0.26-capod-model-battery-gate"','versionName = "1.0.34-case-preflight-lab"')
print("V134_LAB_MENU_INTEGRATED=PASS")
