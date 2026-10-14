#!/usr/bin/env python3
"""v1.0.11 safe UX: one-tap updater + diagnostic logs under Settings.
Apply AFTER all known-good v1.0.10 GOLDEN source patch scripts.
No changes to service, BLE, AACP, ANC, battery, or VideoView bugfixes.
"""
from pathlib import Path
import sys, shutil, xml.etree.ElementTree as ET

root=Path(sys.argv[1])
repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"

def once(p:Path, before:str, after:str, desc:str):
    txt=p.read_text(encoding="utf-8")
    count=txt.count(before)
    if count != 1:
        raise RuntimeError(f"V111_PATCH_STOP {desc} expected one, got {count}")
    p.write_text(txt.replace(before,after,1),encoding="utf-8")
    print("V111_PATCH_PASS",desc)

manager=base/"PersonalUpdateManager.kt"
assert not manager.exists()
shutil.copyfile(repo/"ci/PersonalUpdateManager_v111.kt",manager)
assert manager.stat().st_size>4000
print("V111_UPDATER_MODULE=PASS")

act=base/"MainActivity.kt"
once(act,
'        AirPodsCrashDiary.show(this)\n        enableEdgeToEdge()',
'        // Diagnostics live under Settings > 오류 기록, not on every startup.\n        enableEdgeToEdge()',
"remove_forced_dialog_without_disabling_recorder")

settings=base/"presentation/screens/AppSettingsScreen.kt"
once(settings,
'        AppInfoCard(navigateToReleaseNotesScreen)\n\n        Spacer(modifier = Modifier.height(16.dp))',
'''        AppInfoCard(navigateToReleaseNotesScreen)

        Spacer(modifier = Modifier.height(16.dp))
        StyledList(title = "내 에어팟 5 관리") {
            StyledListItem(
                name = "앱 업데이트 확인",
                onClick = {
                    (context as? android.app.Activity)?.let {
                        me.kavishdevar.librepods.PersonalUpdateManager.check(it)
                    }
                }
            )
            StyledListItem(
                name = "오류 기록",
                onClick = {
                    (context as? android.app.Activity)?.let {
                        me.kavishdevar.librepods.AirPodsCrashDiary.show(it)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))''',
"two_settings_actions_for_update_and_logs")

diary=base/"AirPodsCrashDiary.kt"
once(diary,
'return ("My AirPods 5 1.0.9 local diagnostics\\n" +',
'return ("My AirPods 5 " + BuildConfig.VERSION_NAME + " local diagnostics\\n" +',
"diagnostic_version_matches_installed_app")

manifest=root/"android/app/src/main/AndroidManifest.xml"
once(manifest,
'    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"',
'    <uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"',
"permissions_https_and_manual_apk_install")
paths=root/"android/app/src/main/res/xml/file_paths.xml"
once(paths,'    <files-path name="logs" path="logs/"/>','    <files-path name="logs" path="logs/"/>\n    <cache-path name="app_updates" path="updates/"/>',
     "fileprovider_private_update_cache")
ET.parse(paths);ET.parse(manifest)

gradle=root/"android/app/build.gradle.kts"
once(gradle,'versionCode = 10010','versionCode = 10011',"new_version_code")
once(gradle,'versionName = "1.0.10-video-badtoken-fix"',
            'versionName = "1.0.11-safe-update-logs"',"new_version_name")

assert "AirPodsCrashDiary.show(this)" not in act.read_text("utf-8")
assert "onServiceConnected" in act.read_text("utf-8")
assert "android.resource://me.kavishdevar.librepods/" not in (base/"presentation/overlays/PopupWindow.kt").read_text("utf-8")
assert "android.resource://me.kavishdevar.librepods/" not in (base/"presentation/overlays/IslandWindow.kt").read_text("utf-8")
print("V111_GOLDEN_PROTOCOL_AND_MEDIA_PATCH_PRESERVED=PASS")
