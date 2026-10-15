#!/usr/bin/env python3
from pathlib import Path
import shutil,sys
root=Path(sys.argv[1]);repo=Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
shutil.copyfile(repo/"ci/CaseBeaconFinder_v113.kt",base/"CaseBeaconFinder.kt")
case=base/"CaseSpeakerLab.kt"
s=case.read_text(encoding="utf-8")
anchor="object CaseSpeakerLab {"
addition="""object CaseSpeakerLab {
    // Only the encrypted-case-advert finder may provide this BLE device.
    // Do NOT use the paired earbud Bluetooth Classic address (causes timeout 147).
    fun openVerifiedCase(activity: Activity, caseDevice: BluetoothDevice) {
        active?.close()
        val probe=Probe(activity, caseDevice)
        active=probe
        probe.start()
    }
"""
assert s.count(anchor)==1
s=s.replace(anchor,addition,1)
old='''finish("BLE 연결 종료", "상태코드: $status (GATT 연결이 허용되지 않았을 수 있음)")'''
new='''finish("BLE 연결 종료", if (status == 147)
                    "Android GATT_CONNECTION_TIMEOUT (147): 케이스 신호가 연결 가능 상태가 아니거나 주소가 변경됐습니다."
                    else "BLE GATT 상태 코드: $status")'''
assert s.count(old)==1
s=s.replace(old,new,1)
case.write_text(s,encoding="utf-8")
settings=base/"presentation/screens/AppSettingsScreen.kt"
s=settings.read_text(encoding="utf-8")
old="me.kavishdevar.librepods.CaseSpeakerLab.open(it)"
new="me.kavishdevar.librepods.CaseBeaconFinder.open(it)"
assert s.count(old)==1
settings.write_text(s.replace(old,new,1),encoding="utf-8")
gradle=root/"android/app/build.gradle.kts"
s=gradle.read_text(encoding="utf-8")
assert s.count("versionCode = 10012")==1
s=s.replace("versionCode = 10012","versionCode = 10013",1)
assert s.count('versionName = "1.0.12-case-research"')==1
s=s.replace('versionName = "1.0.12-case-research"', 'versionName = "1.0.13-case-radio-fix"', 1)
gradle.write_text(s,encoding="utf-8")
assert "com.myairpods5.personal.lab" in s
assert "openVerifiedCase" in case.read_text(encoding="utf-8")
assert "CaseBeaconFinder.open(it)" in settings.read_text(encoding="utf-8")
print("V113_CRYPTOVERIFIED_CASE_ADDRESS_GATT_147_FIX_SOURCE_GATE=PASS")
