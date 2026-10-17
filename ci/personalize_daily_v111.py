#!/usr/bin/env python3
"""v1.0.11 Korean daily driver UI-only patch after verified v1.0.10 r21 pipeline.

Preserves original Bluetooth/AACP/ATT/BLE, the v1.0.10 VideoView crash fix,
and all existing useful AirPods controls. Case speaker "Find My" code is NOT
added; this personal app must not alter the user's Android Auto Golden.
Copyright notices/GPL license must remain available.
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 2:
    raise SystemExit("usage: personalize_daily_v111.py <extracted-app-source>")
root = Path(sys.argv[1])
base = root / "android/app/src/main/java/me/kavishdevar/librepods"
res = root / "android/app/src/main/res"

def replace_once(relative, before, after, gate):
    p = base / relative
    source = p.read_text(encoding="utf-8")
    count = source.count(before)
    if count != 1:
        raise RuntimeError(f"{gate}: expected exact 1 anchor, got {count}")
    p.write_text(source.replace(before, after, 1), encoding="utf-8")
    print("DAILY_UI_PASS", gate)

# Do not pop a mandatory developer forensic dialog on every normal launch.
# Preserve the on-device crash recorder and a *manual* diagnostic path.
replace_once(
    "MainActivity.kt",
    '''        AirPodsCrashDiary.show(this)
        enableEdgeToEdge()''',
    '''        // Daily-use mode: crash recording stays on; view it from app settings.
        enableEdgeToEdge()''',
    "diagnostic_dialog_is_not_forced_on_launch",
)
replace_once(
    "presentation/screens/AppSettingsScreen.kt",
    '''        if (!state.isPremium && state.connectionSuccessful) {''',
    '''        if (false) { // Personal FOSS build: never advertise a paid upgrade.''',
    "no_paywall_button_on_app_settings",
)
replace_once(
    "presentation/screens/AppSettingsScreen.kt",
    '''        DeviceInfoCard()''',
    '''        StyledList {
            StyledListItem(
                name = "내 에어팟 오류 기록 보기",
                onClick = {
                    (context as? android.app.Activity)?.let {
                        me.kavishdevar.librepods.AirPodsCrashDiary.show(it)
                    }
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        DeviceInfoCard()''',
    "manual_local_diagnostics_entry",
)

# The AirPods settings screen independently exposed another billing banner
# and purchase CTA. Suppress only these UI entry points: never spoof
# isPremium, force an unsupported model feature, or change an AACP command.
replace_once(
    "presentation/screens/AirPodsSettingsScreen.kt",
    '''                if (state.timeUntilFOSSPremiumExpiry > 0L) {''',
    '''                if (false) { // Korean personal build: no billing banner.''',
    "no_billing_banner_on_airpods_settings",
)
replace_once(
    "presentation/screens/AirPodsSettingsScreen.kt",
    '''                if (!state.isPremium) {''',
    '''                if (false) { // Korean personal build: no purchase CTA.''',
    "no_purchase_button_on_airpods_settings",
)

# Name only the user-visible APK/application; never change BT alias, MAC,
# device pairing data, service package identity, appId or update cert.
default_xml = res / "values/strings.xml"
xml = ET.parse(default_xml)
node = next((x for x in xml.getroot().findall("string")
             if x.get("name") == "app_name"), None)
if node is None:
    raise RuntimeError("app_name resource missing")
node.text = "주현에어팟5"
xml.write(default_xml, encoding="utf-8", xml_declaration=True)
ET.parse(default_xml)
print("DAILY_UI_PASS", "personal_application_name")

# Existing Korean catalog is excellent; add only missing keys, without
# touching defaults, plurals/format placeholders, or audio protocol logic.
ko_xml = res / "values-ko/strings.xml"
ko_tree = ET.parse(ko_xml)
ko_root = ko_tree.getroot()
default_names = {x.get("name") for x in ET.parse(default_xml).getroot().findall("string")}
existing = {x.get("name"): x for x in ko_root.findall("string")}
korean = {
    "troubleshooting": "진단 및 문제 해결",
    "battery": "배터리",
    "left": "왼쪽",
    "right": "오른쪽",
    "case_alt": "충전 케이스",
    "connected": "연결됨",
    "disconnected": "연결 끊김",
    "app_name": "주현에어팟5",
    "listening_mode": "청취 모드",
    "noise_control": "청취 모드",
    "transparency": "주변음 허용",
    "adaptive": "적응형",
    "noise_cancellation": "노이즈 캔슬링",
    "off": "끔",
    "head_gestures": "머리 동작",
    "settings": "설정",
    "press_and_hold_airpods": "에어팟 길게 누르기",
    "automatically_connect": "자동으로 연결",
    "open_source_licenses": "오픈소스 라이선스",
}
for key, value in korean.items():
    if key not in default_names: continue
    if key == "app_name":
        # app_name is nontranslatable in default resources; do not override.
        continue
    item = existing.get(key)
    if item is None:
        item = ET.SubElement(ko_root, "string", {"name": key})
    item.text = value
ET.indent(ko_tree, space="    ")
ko_tree.write(ko_xml, encoding="utf-8", xml_declaration=True)
ET.parse(ko_xml)
print("DAILY_UI_PASS", "ko_catalog_verified")

# Set a higher code, but DO NOT claim install-in-place when r21 and this
# CI runner's ephemeral debug signing keys differ.
build = root / "android/app/build.gradle.kts"
text = build.read_text("utf-8")
old = 'versionCode = 10010'
oldname = 'versionName = "1.0.10-video-badtoken-fix"'
assert text.count(old) == 1, "r21 version code baseline moved"
assert text.count(oldname) == 1, "r21 version name baseline moved"
text = text.replace(old, 'versionCode = 10011', 1)
text = text.replace(oldname, 'versionName = "1.0.11-juhyeon-ko-daily"', 1)
build.write_text(text, encoding="utf-8")

# Explicit regression source assertions. Do not mutate original transport.
main = (base/"MainActivity.kt").read_text("utf-8")
assert "AirPodsCrashDiary.mark" in main
assert "AirPodsCrashDiary.show(this)" not in main
assert (base/"services/AirPodsService.kt").is_file()
assert "AACPManager" in (base/"presentation/screens/AirPodsSettingsScreen.kt").read_text("utf-8")
assert "setOnErrorListener" in (base/"presentation/overlays/PopupWindow.kt").read_text("utf-8")
assert "setOnErrorListener" in (base/"presentation/overlays/IslandWindow.kt").read_text("utf-8")
print("V111_KO_DAILY_UI_ONLY_NO_CASE_RING_NO_AUDIO_ENGINE_CHANGE=PASS")
