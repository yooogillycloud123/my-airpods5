#!/usr/bin/env python3
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1]); repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
assert not (base/"CaseSpeakerLab.kt").exists()
shutil.copyfile(repo/"ci/CaseSpeakerLab_v112.kt",base/"CaseSpeakerLab.kt")
f=base/"presentation/screens/AppSettingsScreen.kt"
s=f.read_text("utf-8")
anchor='''            StyledListItem(
                name = "오류 기록",
                onClick = {
                    (context as? android.app.Activity)?.let {
                        me.kavishdevar.librepods.AirPodsCrashDiary.show(it)
                    }
                }
            )'''
extra='''            StyledListItem(
                name = "케이스 소리 시험 · 연구",
                onClick = {
                    (context as? android.app.Activity)?.let {
                        me.kavishdevar.librepods.CaseSpeakerLab.open(it)
                    }
                }
            )'''
assert s.count(anchor)==1
f.write_text(s.replace(anchor,anchor+"\n"+extra,1),"utf-8")
g=root/"android/app/build.gradle.kts"
s=g.read_text("utf-8")
for old,new in [("com.myairpods5.personal\"","com.myairpods5.personal.lab\""),
                ("versionCode = 10011","versionCode = 10012"),
                ('versionName = "1.0.11-safe-update-logs"','versionName = "1.0.12-case-research"')]:
    assert s.count(old)==1,(old,s.count(old))
    s=s.replace(old,new,1)
g.write_text(s,"utf-8")
import re
for directory in ("values", "values-ko"):
    f=root/"android/app/src/main/res"/directory/"strings.xml"
    if f.exists():
        text=f.read_text("utf-8")
        text,n=re.subn(r'(<string name="app_name">)[^<]*(</string>)',r'\1내 에어팟 5 실험실\2',text,count=1)
        if n: f.write_text(text,"utf-8")
print("V112_RESEARCH_GATT_AND_DISTINCT_LAB_PACKAGE=PASS")
