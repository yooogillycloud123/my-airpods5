from pathlib import Path
import shutil,sys
root,repo=map(Path,sys.argv[1:3])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
for name in ("CaseEntryV143.kt","CaseRfCorrelationV143.kt"):
    shutil.copyfile(repo/"ci/case_protocol_v143"/name,base/name)
p=base/"presentation/screens/AppSettingsScreen.kt"
s=p.read_text()
one="me.kavishdevar.librepods.CaseRadioEvidenceV138.open(it)"
two="me.kavishdevar.librepods.CaseEntryV143.open(it)"
assert s.count(one)==1
s=s.replace(one,two)
one='name = "A3529 케이스 · BLE 신원 검증 v141 · LAB"'
assert s.count(one)==1
p.write_text(s.replace(one,'name = "A3529 케이스 무선 신원 A/B v142 · LAB"'))
g=root/"android/app/build.gradle.kts"
s=g.read_text()
assert s.count("versionCode = 10041")==1
assert s.count("1.0.41-a3529-silent-ack-evidence")==1
g.write_text(s.replace("versionCode = 10041","versionCode = 10042").replace(
 "1.0.41-a3529-silent-ack-evidence","1.0.42-case-rf-ab"))
assert "CaseRadioEvidenceV138.open(activity)" in (base/"CaseEntryV143.kt").read_text()
assert "CaseRfCorrelationV143.open(activity)" in (base/"CaseEntryV143.kt").read_text()
print("V142_OPT_IN_RADIO_AB=PASS")
print("V142_OLD_V141_MENU=PASS")
