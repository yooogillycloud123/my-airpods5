#!/usr/bin/env python3
"""Fix Android 12+ Lab runtime permission dead end without touching GOLDEN.

V120 showed a denial message but never requested SCAN / CONNECT / precise location.
Android 12+ requires FINE and COARSE to be requested together.
Owner IRK check now precedes location request, to avoid unnecessary permissions.
"""
from pathlib import Path
import sys
root = Path(sys.argv[1])
base = root / "android/app/src/main/java/me/kavishdevar/librepods"
src = base / "CaseOwnerProbeV120.kt"
m = root / "android/app/src/main/AndroidManifest.xml"
g = root / "android/app/build.gradle.kts"
s=src.read_text("utf-8")
def replace_once(old,new,name):
    global s
    n=s.count(old)
    if n != 1:
        raise RuntimeError(f"V121_ABORT {name} expected 1 anchor got {n}")
    s=s.replace(old,new,1)
    print("V121_PASS", name)

replace_once("import android.content.Context\n",
"""import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
""","intent_for_settings_permissions")
replace_once("""    fun open(a: Activity) {
        val scan = a.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        val connect = a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        val location = a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!scan || !connect || !location) {
            show(a, "검색 권한 부족", "실험실 앱의 주변 기기 검색/연결 및 정확한 위치 권한을 허용해야 BLE 주소를 비교할 수 있습니다. GPS 좌표는 수집하지 않습니다.")
            return
        }
        val raw = a.getSharedPreferences("settings",Context.MODE_PRIVATE)""",
"""    fun open(a: Activity) {
        // Check source AACP IRK *before* requesting sensitive permissions.
        // If Lab was reinstalled, the separate Golden key store is not shared.
        val raw = a.getSharedPreferences("settings",Context.MODE_PRIVATE)""",
"do_not_prompt_location_when_irk_missing")
replace_once("""            return
        }
        AlertDialog.Builder(a).setTitle("케이스 제어 경로 · 한 번만 확인")""",
"""            return
        }
        val missing = missingPermissions(a)
        if (missing.isNotEmpty()) {
            val nearbyMissing = missing.contains(Manifest.permission.BLUETOOTH_SCAN) ||
                missing.contains(Manifest.permission.BLUETOOTH_CONNECT)
            val preciseMissing = missing.contains(Manifest.permission.ACCESS_FINE_LOCATION)
            val message = "본인 IRK 키는 확인됐습니다.\\n\\n" +
                (if(nearbyMissing) "• 주변 기기: 검색·연결 권한 필요\\n" else "") +
                (if(preciseMissing) "• 위치: '앱 사용 중 허용' + '정확한 위치' 필요\\n" else "") +
                "\\n광고 신호 누락을 피하기 위한 실험실 검색 권한입니다. " +
                "GPS 좌표나 위치 기록은 수집·전송하지 않습니다. " +
                "소리 명령이나 자동 GATT 접속은 실행하지 않습니다."
            AlertDialog.Builder(a).setTitle("BLE 조사 권한 설정")
                .setMessage(message)
                .setNegativeButton("취소",null)
                .setNeutralButton("앱 권한 설정"){_,_->openAppSettings(a)}
                .setPositiveButton("필요한 권한 허용"){_,_->
                    try {
                        a.requestPermissions(missing.toTypedArray(),121)
                        // No implicit scan or connect. Re-open only the
                        // explicit 12-second scan confirmation once granted.
                        waitForGrants(a,0)
                    } catch (_:Exception) {
                        show(a,"권한 요청 실패","설정→애플리케이션→내 에어팟 5 실험실→권한에서 주변 기기와 정확한 위치를 허용해주세요.")
                    }
                }.show()
            return
        }
        AlertDialog.Builder(a).setTitle("케이스 제어 경로 · 한 번만 확인")""",
"request_missing_permissions_and_reopen_confirmation")
replace_once("""    private fun show(a:Activity, title:String,body:String) {""",
"""    private fun missingPermissions(a:Activity):List<String>{
        val p=mutableListOf<String>()
        if (a.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)
            p.add(Manifest.permission.BLUETOOTH_SCAN)
        if (a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            p.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            // Android 12+ can ignore a FINE-only permission request.
            // The COARSE+FINE pair gives the user the precise/approximate choice.
            p.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            p.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return p.distinct()
    }

    private fun waitForGrants(a:Activity,attempt:Int) {
        if(a.isFinishing || a.isDestroyed) return
        if(missingPermissions(a).isEmpty()) {
            open(a) // Still requires user's explicit 12-second scan confirmation.
            return
        }
        // If denied, don't continually request permissions or trigger a scan.
        if(attempt<35) ui.postDelayed({waitForGrants(a,attempt+1)},1000L)
    }

    private fun openAppSettings(a:Activity) {
        try{
            a.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:"+a.packageName)))
        }catch (_:Exception) {
            show(a,"권한 설정 열기 실패","설정→애플리케이션→내 에어팟 5 실험실→권한에서 주변 기기와 정확한 위치를 허용해주세요.")
        }
    }

    private fun show(a:Activity, title:String,body:String) {""",
"android12_coarse_and_fine_together_with_settings_fallback")
src.write_text(s,"utf-8")
mm=m.read_text("utf-8")
tag='<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />'
assert mm.count(tag)==1,("Manifest fine permission anchor",mm.count(tag))
mm=mm.replace(tag, '<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />\n    '+tag,1)
m.write_text(mm,"utf-8")
print("V121_PASS_COARSE_FINE_DECLARED")
gg=g.read_text("utf-8")
assert gg.count('versionCode = 10020') == 1
assert gg.count('versionName = "1.0.20-owner-gatt-preflight"') == 1
gg=gg.replace('versionCode = 10020','versionCode = 10021',1)
gg=gg.replace('versionName = "1.0.20-owner-gatt-preflight"','versionName = "1.0.21-owner-gatt-permission-fix"',1)
g.write_text(gg,"utf-8")
assert 'applicationId = "com.myairpods5.personal.lab"' in gg
assert 'writeCharacteristic' not in s
assert 'getString("IRK",null)' in s
assert 'requestPermissions(missing.toTypedArray(),121)' in s
assert 'ACCESS_COARSE_LOCATION' in s
assert 'openAppSettings' in s
print("V121_PERMISSION_FLOW_AUTO_RECOVERY=PASS")
print("V121_USER_PERMISSION_CHOICE_REQUIRED=TRUE")
print("V121_NO_GATT_WRITE_NO_SOUND_CONTROL=PASS")
print("V121_GOLDEN_PACKAGE_UNTOUCHED=PASS")
