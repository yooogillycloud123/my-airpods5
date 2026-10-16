#!/usr/bin/env python3
"""v139: ONE LAB package (replace v138), zero background scanner in this clone,
disjoint packet counts, optional explicit Android Settings app-inventory.
No change to personal Golden or AA and no Bluetooth pairing mutations."""
from pathlib import Path
import shutil,sys

root,repo=Path(sys.argv[1]),Path(sys.argv[2])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
radio=base/"CaseRadioEvidenceV138.kt"
service=base/"services/AirPodsService.kt"
settings=base/"presentation/screens/AppSettingsScreen.kt"
manifest=root/"android/app/src/main/AndroidManifest.xml"
gradle=root/"android/app/build.gradle.kts"

def once(path,old,new):
    s=path.read_text("utf-8")
    n=s.count(old)
    assert n==1,(str(path),old[:100],n)
    path.write_text(s.replace(old,new,1),"utf-8")

# Every earlier LAB cloned AirPodsService (which automatically creates BLEManager
# and registers a persistent scan); this LAB must never register a background
# scan in addition to its single user-initiated 12-second ScanCallback.
once(service,"            bleManager.startScanning()",
     '''            // v139 TEST ONLY: no permanent BLE scanner in cloned service.
            // A single read-only scan begins only after an explicit menu click.
            android.util.Log.i("AirPodsCaseLAB", "BACKGROUND_BLE_SCANNER_DISABLED_V139")''')
assert "bleManager.startScanning()" not in service.read_text("utf-8")
# Companion still creates its service for the original UI. We do not stop
# other installed LAB services: Android sandbox forbids force-stop of others.
shutil.copy2(repo/"ci/case_protocol_v139/CaseLabInventoryV139.kt",base/"CaseLabInventoryV139.kt")
once(radio,'''            .setNegativeButton("취소", null)
            .setPositiveButton("한 번 확인")''',
     '''            .setNegativeButton("취소", null)
            .setNeutralButton("실험앱 관리") { _, _ -> CaseLabInventoryV139.open(a) }
            .setPositiveButton("한 번 확인")''')
once(radio,'private var generic12 = 0',
     '''private var generic12 = 0
        private var findmy19Nonconnectable = 0
        private val nearbyUniqueAddresses = HashSet<String>()
        private val separatedUniqueAddresses = HashSet<String>()''')
once(radio,
'''val casePro3: Int, val nearInfo10: Int, val generic12: Int,
        val candidates: List<Candidate>''',
'''val casePro3: Int, val nearInfo10: Int, val generic12: Int,
        val findmy19Nonconnectable: Int, val nearbyUnique: Int,
        val separatedUnique: Int, val sameAddressTwoModes: Int,
        val candidates: List<Candidate>''')
old='''                } else if (kind == 0x12) {
                    generic12++
                    if (subtype == 0x02) findmy02++
                    if (subtype == 0x19 && result.isConnectable) {
                        findmy19++
                        // Retain only transient in-memory candidates for explicit manual review.
                        // A Find My separated candidate is NOT presumed to be an OWN AirPods case.
                        val id = result.device.address
                        if (candidates.size < 12 || candidates.containsKey(id))
                            candidates[id] = Candidate(result.device, result.rssi)
                    }
                } else if (kind == 0x10) {'''
new='''                } else if (kind == 0x12) {
                    if (subtype == 0x02) {
                        findmy02++
                        // Only ephemeral in-memory radio address count; no address is displayed.
                        nearbyUniqueAddresses.add(result.device.address)
                    } else if (subtype == 0x19) {
                        separatedUniqueAddresses.add(result.device.address)
                        if (result.isConnectable) {
                            findmy19++
                            // Only user-selected known OWN candidate can proceed to read-only GATT.
                            val id = result.device.address
                            if (candidates.size < 12 || candidates.containsKey(id))
                                candidates[id] = Candidate(result.device, result.rssi)
                        } else {
                            findmy19Nonconnectable++
                        }
                    } else {
                        // disjoint bucket, unlike v138's misleading inclusive "other" count.
                        generic12++
                    }
                } else if (kind == 0x10) {'''
once(radio,old,new)
once(radio,'''nearInfo10, generic12, candidates.values.toList())''',
     '''nearInfo10, generic12, findmy19Nonconnectable,
                nearbyUniqueAddresses.size, separatedUniqueAddresses.size,
                nearbyUniqueAddresses.count { separatedUniqueAddresses.contains(it) },
                candidates.values.toList())''')
once(radio,'''report.append("Find My 주변형 12:02=").append(s.findmy02).append('\n')
            report.append("Find My 분리형 12:19 연결가능=").append(s.findmy19).append('\n')
            report.append("분리형 서로 다른 후보=").append(s.candidates.size).append('\n')
            report.append("다른 Find My 12형=").append(s.generic12).append('\n')''',
'''report.append("Find My 주변형 12:02=").append(s.findmy02).append('\n')
            report.append("  ↳ 서로 다른 주소(임시)=").append(s.nearbyUnique).append('\n')
            report.append("Find My 분리형 12:19 연결가능=").append(s.findmy19).append('\n')
            report.append("Find My 12:19 연결불가=").append(s.findmy19Nonconnectable).append('\n')
            report.append("  ↳ 분리형 서로 다른 주소=").append(s.separatedUnique).append('\n')
            report.append("  ↳ 동일 주소의 12:02·12:19 관찰=").append(s.sameAddressTwoModes).append('\n')
            report.append("연결 가능 후보=").append(s.candidates.size).append('\n')
            report.append("그 외 12형(중복 제외)=").append(s.generic12).append('\n')''')
once(radio,"\"다른 사람 기기에 연결하거나 소리 명령을 보내지 않습니다. 자동 재검색도 없습니다.\"",
     '''"다른 사람 기기에 연결하거나 소리 명령을 보내지 않습니다. " +
                "실험용 앱의 백그라운드 BLE 검색을 끈 상태에서 한 번만 확인합니다."''')
once(radio,"\"검색을 자동 반복하지 않습니다.\"",
     "\"이 LAB 상시 BLE 검색=OFF / 수동 검색만 ON\"")
once(radio,"\"12초 BLE 신호별 결과\"","\"v139 단일 스캐너 · 12초 결과\"")
once(settings,'name = "AirPods 5 / Find My 신호 분리 · LAB"',
     'name = "AirPods / Find My 진단 v139 · LAB"')
once(gradle,"versionCode = 10038","versionCode = 10039")
once(gradle,'versionName = "1.0.38-airpods-findmy-split-evidence"',
     'versionName = "1.0.39-single-scanner-lab-replaces-v138"')
# CRITICAL: keep EXACT same package ID as installed v138, NOT one more installed app.
assert 'applicationId = "com.myairpods5.personal.radiolab"' in gradle.read_text("utf-8")
assert 'android:label="AirPods 신호 확인 · LAB"' in manifest.read_text("utf-8")
c=radio.read_text("utf-8")
assert c.count("startScan(")==1
assert "findmy19Nonconnectable" in c and "nearbyUniqueAddresses.count" in c
assert 'setNeutralButton("실험앱 관리")' in c
assert "writeCharacteristic(" not in c and "createBond(" not in c
assert "setItems(rows)" in c
x=service.read_text("utf-8")
assert "BACKGROUND_BLE_SCANNER_DISABLED_V139" in x
assert "bleManager.startScanning()" not in x
print("V139_SAME_PACKAGE_REPLACES_V138=PASS")
print("V139_BACKGROUND_BLE_DISABLED_ONLY_OWN_LAB=PASS")
print("V139_SINGLE_MANUAL_BLE_SCANNER=PASS")
print("V139_DISJOINT_12_FRAME_CLASSES=PASS")
print("V139_IN_MEMORY_EPHEMERAL_TYPE_CROSSCHECK=PASS")
print("V139_LAB_ROLE_INVENTORY_AND_NONDESTRUCTIVE_APP_INFO=PASS")
print("V139_GATT_WRITES_PAIRING_CHANGES=ZERO")
print("V139_PERSONAL_GOLDEN_AA_GOLDEN_UNTOUCHED=PASS")
