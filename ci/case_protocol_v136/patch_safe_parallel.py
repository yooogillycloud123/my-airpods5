#!/usr/bin/env python3
"""v136 additive LAB install: keep v134 data safe despite incompatible debug signer.
Do not alter the original AirPods audio engine or v135 candidate picker."""
from pathlib import Path
import re, sys

root = Path(sys.argv[1])
gradle = root / "android/app/build.gradle.kts"
manifest = root / "android/app/src/main/AndroidManifest.xml"
picker = root / "android/app/src/main/java/me/kavishdevar/librepods/CasePathV134.kt"

s = gradle.read_text(encoding="utf-8")
checks = {
    'applicationId = "com.myairpods5.personal.lab"': 'applicationId = "com.myairpods5.personal.caseprobe"',
    "versionCode = 10035": "versionCode = 10036",
    'versionName = "1.0.35-candidate-selection-fixed"': 'versionName = "1.0.36-candidate-picker-parallel-safe"',
}
for old, new in checks.items():
    assert s.count(old) == 1, ("GRADLE_ANCHOR_FAILURE", old, s.count(old))
    s = s.replace(old, new, 1)
gradle.write_text(s, encoding="utf-8")

m = manifest.read_text(encoding="utf-8")
match = re.search(r'<application\b[^>]*>', m, re.S)
assert match, "MANIFEST_APPLICATION_NOT_FOUND"
tag = match.group()
if re.search(r'\bandroid:label\s*=\s*["\x27][^"\x27]*["\x27]', tag):
    tag = re.sub(r'\bandroid:label\s*=\s*["\x27][^"\x27]*["\x27]',
                 'android:label="케이스 후보 확인 · LAB"', tag, count=1)
else:
    tag = tag.replace('<application', '<application android:label="케이스 후보 확인 · LAB"', 1)
m = m[:match.start()] + tag + m[match.end():]
for p in re.findall(r'<provider\b[^>]*>', m, re.S):
    if 'android:authorities' in p:
        assert '${applicationId}' in p or 'com.myairpods5.personal.caseprobe' in p, (
            "STATIC_PROVIDER_AUTHORITY_MAY_COLLIDE", p)
manifest.write_text(m, encoding="utf-8")

code = picker.read_text(encoding="utf-8")
a = code.index('AlertDialog.Builder(a).setTitle("후보 ')
b = code.index('.setItems(labels)', a)
assert '.setMessage(' not in code[a:b], "CANDIDATE_ROWS_HIDDEN"
assert 'val labels = items.mapIndexed' in code
assert 'setPositiveButton("내 기기 서비스 확인")' in code
assert 'g.discoverServices()' in code
assert 'writeCharacteristic(' not in code
assert 'createBond(' not in code
assert '0x12' in code and '0x19' in code
print("V136_PARALLEL_APP_ID=PASS com.myairpods5.personal.caseprobe")
print("V136_OLD_V134_LAB_PRESERVED=PASS no_replace_no_uninstall")
print("V136_CANDIDATE_LIST_TAPPABLE=PASS")
print("V136_GATT_READ_ONLY=PASS no_sound_commands")
