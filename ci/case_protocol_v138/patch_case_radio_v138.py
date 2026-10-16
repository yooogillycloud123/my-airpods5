#!/usr/bin/env python3
"""v138: replace over-narrow 12:19-only menu with two-family passive evidence.
Does not modify the original LibrePods Bluetooth/audio service, prefs or pairing."""
from pathlib import Path
import shutil
import sys
root, repo = Path(sys.argv[1]), Path(sys.argv[2])
base = root/"android/app/src/main/java/me/kavishdevar/librepods"
settings = base/"presentation/screens/AppSettingsScreen.kt"
manifest = root/"android/app/src/main/AndroidManifest.xml"
gradle = root/"android/app/build.gradle.kts"
radio = base/"CaseRadioEvidenceV138.kt"
shutil.copy2(repo/"ci/case_protocol_v138/CaseRadioEvidenceV138.kt", radio)

def replace_exact(p, old, new):
    s = p.read_text(encoding="utf-8")
    assert s.count(old) == 1, (p, old, s.count(old))
    p.write_text(s.replace(old, new, 1), encoding="utf-8")

replace_exact(settings, "me.kavishdevar.librepods.CasePathV134.open(it)",
              "me.kavishdevar.librepods.CaseRadioEvidenceV138.open(it)")
replace_exact(settings, 'name = "내 케이스 소리 경로 확인 · LAB"',
              'name = "AirPods 5 / Find My 신호 분리 · LAB"')
replace_exact(manifest, 'android:label="케이스 신원 확인 · LAB"',
              'android:label="AirPods 신호 확인 · LAB"')
replace_exact(gradle, 'applicationId = "com.myairpods5.personal.identityprobe"',
              'applicationId = "com.myairpods5.personal.radiolab"')
replace_exact(gradle, "versionCode = 10037", "versionCode = 10038")
replace_exact(gradle, 'versionName = "1.0.37-readonly-case-identity"',
              'versionName = "1.0.38-airpods-findmy-split-evidence"')

code = radio.read_text(encoding="utf-8")
for a in ('0x3020', '0x3620', 'proximity07', 'findmy19', 'findmy02',
          'casePro3', '0x004c', '12:19 후보 ≠ 본인 에어팟 인증',
          'setItems(rows)', 'CaseIdentityReadV137(g)', 'onScanFailed'):
    assert a in code, a
assert '.setMessage(' not in code[code.index('AlertDialog.Builder(a).setTitle("본인 소유를 확인할 수 있을 때만 선택")'):
                                   code.index('.setItems(rows)')], "Chooser regression"
for forbidden in ('writeCharacteristic(', 'writeDescriptor(', 'createBond(', 'removeBond(', 'startSound('):
    assert forbidden not in code, forbidden
assert code.count('startScan(') == 1
assert '12000L' in code
assert 'onBatchScanResults(' in code
assert 'setReportDelay(0L)' in code
assert 'getManufacturerSpecificData(0x004c)' in code
assert 'subtype == 25' in code and 'kind == 0x12' in code
assert 'data[2].toInt() and 255) == 1' in code
assert "model == 0x3020 || model == 0x3620" in code
assert 'casePro3++' in code and 'subtype == 0x11' in code
assert 'android:authorities="com.myairpods5.personal.identityprobe' not in manifest.read_text(encoding="utf-8")
print("V138_AIRPODS07_19_VS_SEPARATED12_19_INDEPENDENT=PASS")
print("V138_MODEL_3020_3620_PACKET_GATES=PASS")
print("V138_NO_BLIND_SCAN_REPEATS_OR_GATT_WRITES=PASS")
print("V138_FROZEN_ENGINE_AND_EXISTING_PACKAGES_PRESERVED=PASS")
print("V138_CANDIDATE_DIALOG_NO_HIDDEN_ROWS=PASS")
