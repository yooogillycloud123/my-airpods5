#!/usr/bin/env python3
"""v1.0.14: source-evidenced case BLE scan diagnostics, no unsupported beep claims.
Runs AFTER v113 source patch on isolated .lab only.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
scan=base/"CaseBeaconFinder.kt"
manifest=root/"android/app/src/main/AndroidManifest.xml"
g=root/"android/app/build.gradle.kts"

def replace(file,old,new,label):
    s=file.read_text("utf-8")
    n=s.count(old)
    if n!=1: raise RuntimeError(f"V114_GUARD_{label}: expected1 got {n}")
    file.write_text(s.replace(old,new,1),"utf-8")
    print("V114_PASS",label)

replace(manifest,
        'android:name="android.permission.BLUETOOTH_SCAN"\n        android:usesPermissionFlags="neverForLocation"\n        tools:ignore="UnusedAttribute"',
        'android:name="android.permission.BLUETOOTH_SCAN"\n        tools:ignore="UnusedAttribute"',
        "remove_beacon_filtering_neverForLocation")
replace(manifest,
        '<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />',
        '<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />\n    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />',
        "explicit_precise_location_for_lab_scan")

replace(scan,
'''        val pref=activity.getSharedPreferences("settings",Context.MODE_PRIVATE)''',
'''        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(activity).setTitle("케이스 신호 검색 권한")
                .setMessage("정확한 BLE 진단을 위해 Android의 '정확한 위치' 권한이 필요합니다. " +
                    "이전 '위치 추정에 사용하지 않음' 옵션이 일부 BLE 신호를 차단할 수 있어 제거했습니다. " +
                    "이 앱은 위치를 저장하거나 전송하지 않고, 검색 결과도 주소 없이 통계만 보여줍니다.")
                .setNegativeButton("취소",null)
                .setPositiveButton("권한 허용") {_,_->
                    activity.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 114)
                }.show()
            return
        }
        val pref=activity.getSharedPreferences("settings",Context.MODE_PRIVATE)''',
       "precise_location_permission_demand_user_consent")

replace(scan,
'''        private var total=0
        private var candidateFrames=0''',
'''        private var total=0
        private var appleFrames=0
        private var proximity07Frames=0
        private var caseFormattedFrames=0
        private var candidateFrames=0''',
       "multi_stage_scan_counters")

replace(scan,
'''                val mask=byteArrayOf(0xff.toByte(),0xff.toByte(),0xff.toByte())
                val filter=ScanFilter.Builder()
                    .setManufacturerData(APPLE, byteArrayOf(0x07,0x11,0x06),mask)
                    .build()
                scanner?.startScan(listOf(filter),ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),this)''',
'''                // No controller-side manufacturer filter! We first need to
                // measure whether Galaxy can observe ANY BLE / Apple signal.
                // Restrict identity and any subsequent GATT to decrypted own case.
                scanner?.startScan(null,ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0).build(),this)''',
       "unfiltered_radio_advert_scan")

replace(scan,
'''            val payload=result.scanRecord?.getManufacturerSpecificData(APPLE) ?:return
            total++
            if(payload.size!=19 || payload[0]!=0x07.toByte() ||
                payload[1]!=0x11.toByte()|| payload[2]!=0x06.toByte())return
            candidateFrames++''',
'''            total++ // ALL BLE callbacks, unlike prior Apple-filtered count.
            val payload=result.scanRecord?.getManufacturerSpecificData(APPLE) ?:return
            appleFrames++
            if(payload.isNotEmpty() && payload[0]==0x07.toByte()) proximity07Frames++
            if(payload.size!=19 || payload[0]!=0x07.toByte() ||
                payload[1]!=0x11.toByte()|| payload[2]!=0x06.toByte())return
            caseFormattedFrames++
            candidateFrames++''',
       "separate_ble_apple_proximity_case_counters")

replace(scan,
'''            val summary="전체 Apple 케이스형 신호: $candidateFrames / " +
                "내 암호화키 일치: $verified / 동일 주소 2회 확인: ${found!=null}\\n" +
                "RSSI: $lastRssi dBm / 연결 가능 광고: $foundConnectable\\n" +
                "주소는 표시·저장하지 않습니다."''',
'''            val summary="전체 BLE 수신: $total / Apple 광고: $appleFrames / " +
                "07형 Apple 광고: $proximity07Frames\\n" +
                "기존 케이스형(07 11 06): $caseFormattedFrames / " +
                "내 암호화키 구조 일치: $verified\\n" +
                "내 케이스 동일 주소 2회: ${found!=null} / " +
                "연결 가능: ${if(found!=null) foundConnectable else "미확인"}\\n" +
                "RSSI: ${if(found!=null) "$lastRssi dBm" else "측정 불가"}\\n" +
                "개별 장치 주소는 기록·공유하지 않습니다."''',
       "honest_all_ble_scan_report")

replace(scan,
'''                        summary+"\\n\\n오류 147을 재시도하지 않았습니다. " +
                        "케이스가 광고를 보내는 시점·폰 BLE 검색 조건·암호화키를 확인해야 합니다.")''',
'''                        summary+"\\n\\n0건이 나왔다면 위 숫자로 어느 단계에서 멈췄는지 판단할 수 있습니다. " +
                        "이 버전은 확인되지 않은 케이스에 GATT 연결·소리 명령을 보내지 않습니다. " +
                        "진단 복사 결과만 전달해주세요.")''',
       "stage_specific_error_instead_of_guess")

replace(scan,
'''            val summary="전체 BLE 수신''',
'''            val summary="전체 BLE 수신''',
       "noop_exact_status_check") if False else None

replace(g,'versionCode = 10013','versionCode = 10014',"new_version_code")
replace(g,'versionName = "1.0.13-case-radio-fix"','versionName = "1.0.14-unfiltered-case-diagnostics"',"new_version_name")

assert "neverForLocation" not in manifest.read_text("utf-8") or 'android:usesPermissionFlags="neverForLocation"' not in manifest.read_text("utf-8")
assert 'setReportDelay(0)' in scan.read_text("utf-8")
assert "com.myairpods5.personal.lab" in g.read_text("utf-8")
print("V114_SAFE_OBSERVATION_ONLY=PASS")
