#!/usr/bin/env python3
"""V125: evidence-based fix for 0% displayed from LAB's all-zero/unknown battery.
This touches only Compose/overlay presentation, *not* Bluetooth/AAP/ANC transport.
The ONLY available presentation source has no freshness/provenance flag, so
zero cannot be asserted to be real when ALL components contain zero.
"""
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1]); repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
battery=base/"presentation/components/BatteryView.kt"
popup=base/"presentation/overlays/PopupWindow.kt"
settings=base/"presentation/screens/AppSettingsScreen.kt"
g=root/"android/app/build.gradle.kts"
def change(p,old,new,gate):
    s=p.read_text("utf8")
    n=s.count(old)
    assert n==1,f"{gate}_ANCHOR_MISSING count={n}"
    p.write_text(s.replace(old,new,1),"utf8")
    print(gate+"=PASS")

change(battery,"    val caseLevel = case?.level ?: 0",
"""    val caseLevel = case?.level ?: 0
    // Preserve legitimate 0% only when ANOTHER component supplied a
    // trustworthy nonzero reading. Without raw packet provenance, an
    // all-zero list is ambiguous; report unknown instead of fake 0%.
    // Source: actual v124 user observation, not a speculative BLE scan.
    val hasNonzeroMeasuredBattery = listOfNotNull(left, right, case)
        .any { it.level in 1..100 }""","V125_NONZERO_EVIDENCE_GUARD")
change(battery,"if (left == null && right == null) {",
"if ((left == null && right == null) || !hasNonzeroMeasuredBattery) {",
"V125_ALL_ZERO_BATTERY_UNKNOWN")
change(battery,"if (left != null && (leftLevel > 0 || left.status != BatteryStatus.DISCONNECTED)) {",
"if (hasNonzeroMeasuredBattery && left != null && (leftLevel > 0 || left.status != BatteryStatus.DISCONNECTED)) {",
"V125_NO_LEFT_FAKE_RING")
change(battery,"if (right != null && (rightLevel > 0 || right.status != BatteryStatus.DISCONNECTED)) {",
"if (hasNonzeroMeasuredBattery && right != null && (rightLevel > 0 || right.status != BatteryStatus.DISCONNECTED)) {",
"V125_NO_RIGHT_FAKE_RING")
change(battery,"if (case != null && (caseLevel > 0 || case.status != BatteryStatus.DISCONNECTED)) {",
"if (hasNonzeroMeasuredBattery && case != null && (caseLevel > 0 || case.status != BatteryStatus.DISCONNECTED)) {",
"V125_NO_CASE_FAKE_RING")
change(battery,'text = "배터리 데이터 미수신",',
               'text = "배터리 정보 확인 안 됨 (0%로 단정하지 않음)",',
               "V125_UNKNOWN_NOT_ZERO_TEXT")
change(popup,"    private fun updateBatteryStatusFromList(batteryList: List<Battery>) {",
"""    private fun updateBatteryStatusFromList(batteryList: List<Battery>) {
        // Zero-filled initial/invalid packets must not masquerade as a real
        // 0% battery state in the overlay while other components are unknown.
        val hasNonzeroMeasuredBattery = batteryList.any { it.level in 1..100 }""",
"V125_OVERLAY_SOURCE_EVIDENCE")
s=popup.read_text("utf8")
a='if (it.status != BatteryStatus.DISCONNECTED) {'
assert s.count(a)==3,("POPUP_COMPONENT_BRANCHES",s.count(a))
popup.write_text(s.replace(a,'if (hasNonzeroMeasuredBattery && it.status != BatteryStatus.DISCONNECTED) {'),"utf8")
print("V125_OVERLAY_3_COMPONENT_ZERO_GUARDS=PASS")

shutil.copy2(repo/"ci/case_protocol_v125/CaseSoundStatusV125.kt",base/"CaseSoundStatusV125.kt")
change(settings,"me.kavishdevar.librepods.CaseResearchMenuV124.open(it)",
                "me.kavishdevar.librepods.CaseSoundStatusV125.open(it)",
                "V125_DEACTIVATE_REFUTED_PRO3_SCAN")
change(settings,'name = "내 케이스 찾기 연구 v1.0.24"',
                'name = "내 AirPods 5 케이스 찾기 · 개발 상태"',
                "V125_STATUS_MENU")
change(g,'versionCode = 10024','versionCode = 10025',"V125_VERSIONCODE")
change(g,'versionName = "1.0.24-independent-case-advert-lab"',
         'versionName = "1.0.25-battery-provenance-case-gate"',
         "V125_VERSIONNAME")
now=battery.read_text("utf8")
assert "if ((left == null && right == null) || !hasNonzeroMeasuredBattery)" in now
assert now.count("hasNonzeroMeasuredBattery &&") >= 3
assert "CaseSoundStatusV125.open(it)" in settings.read_text("utf8")
assert "CaseResearchMenuV124.open(it)" not in settings.read_text("utf8")
assert 'applicationId = "com.myairpods5.personal.lab"' in g.read_text("utf8")
assert "writeCharacteristic" not in (base/"CaseSoundStatusV125.kt").read_text("utf8")
print("V125_NO_BLE_SCAN_OR_NEW_CASE_SOUND_COMMAND=PASS")
print("V125_MAIN_APP_GOLDEN_SOURCE_UNTOUCHED=PASS")
