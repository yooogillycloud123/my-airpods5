#!/usr/bin/env python3
"""v1.0.6 UI-only rebuild from the exact archived source.

Do not touch the working LibrePods Bluetooth/AACP/ATT/BLE/service engine.
The CI workflow freezes pristine core hashes and verifies exact identity.
GPL-3.0-or-later source-derived personal build; FOSS has existing local feature
switch, this script does not modify or impersonate billing/ownership status.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1])
base = root / "android/app/src/main/java/me/kavishdevar/librepods"
def update_ui(path, content):
    path.write_text(content, encoding="utf-8")
    print("UI_ONLY_PATCH", path.relative_to(root))

p = base / "presentation/screens/AppSettingsScreen.kt"
s = p.read_text(encoding="utf-8")
start = s.index('        StyledList(title = stringResource(R.string.contact)) {')
end = s.index('        Spacer(modifier = Modifier.height(20.dp))', start)
s = s[:start] + s[end:]
start = s.index('    StyledBottomSheet(\n        visible = contactBottomSheet.value,')
s = s[:start] + '}\n'
s = s.replace('                        "Save",', '                        "저장",')
s = s.replace('                        "Cancel",', '                        "취소",')
assert not any(x in s for x in ('kavish.xyz', 'discord.gg', 'librepods/issues/new'))
update_ui(p, s)

# The source already contains values-ko translations. Prioritize missing UI labels
# without touching the AAC protocol, Bluetooth service or model detection.
defaults = root / "android/app/src/main/res/values/strings.xml"
ko = root / "android/app/src/main/res/values-ko/strings.xml"
assert defaults.is_file() and ko.is_file()
default_root = ET.parse(defaults).getroot()
ko_root = ET.parse(ko).getroot()
default_keys = {e.get("name") for e in default_root.findall("string")}
existing = {e.get("name"):e for e in ko_root.findall("string")}
updates = {
    "open_source_licenses": "오픈소스 라이선스",
    "appearance": "화면 설정",
    "contact": "문의",
    "email": "이메일",
    "github_issues": "GitHub 오류 제보",
    "discord": "디스코드",
    "troubleshooting": "문제 해결",
    "noise_control": "청취 모드",
    "off": "끔",
    "transparency": "주변음 허용",
    "adaptive": "적응형",
    "noise_cancellation": "노이즈 캔슬링",
    "press_and_hold_airpods": "에어팟 길게 누르기",
    "left": "왼쪽",
    "right": "오른쪽",
    "name": "이름",
    "battery": "배터리",
    "head_gestures": "머리 동작",
    "connection": "연결",
    "permissions": "권한 설정",
    "not_supported": "기기 호환성 안내",
    "settings": "설정",
    "widget": "위젯",
    "connected": "연결됨",
    "disconnected": "연결 해제됨",
}
for key,val in updates.items():
    if key not in default_keys: continue
    item=existing.get(key)
    if item is None:
        item=ET.SubElement(ko_root,"string",{"name":key})
    item.text=val
ET.indent(ko_root,space="    ")
ko.write_text('<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(ko_root,encoding="unicode") + '\n',encoding="utf-8")
ET.parse(ko)
print("KOREAN_KEYS",sum(x in default_keys for x in updates))

gradle=root / "android/app/build.gradle.kts"
s=gradle.read_text(encoding="utf-8")
assert s.count('versionCode = 10003') == 1
assert s.count('versionName = "1.0.3-ko-personal"') == 1
s=s.replace('versionCode = 10003', 'versionCode = 10006',1)
s=s.replace('versionName = "1.0.3-ko-personal"','versionName = "1.0.6-pristine-control-ko"',1)
update_ui(gradle,s)

# Confirm previous failed experimental patches were NOT integrated.
service=(base/"services/AirPodsService.kt").read_text("utf-8")
main=(base/"MainActivity.kt").read_text("utf-8")
assert "safeVisibleBatteryLevel" not in service
assert "bindingAttempted" not in main
assert "AirPodsService" in main and "getBattery" in service
print("EXPERIMENTAL_SERVICE_REWRITES=ABSENT")
print("PERSONAL_PRISTINE_CONTROL_UI_ONLY=PASS")
