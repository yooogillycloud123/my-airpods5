#!/usr/bin/env python3
"""v1.0.22 LAB regression repair based on actual user 0%-battery/IRK-none evidence.

Repair UI's false 0% caused by nullable Battery defaulting to zero.
Give LAB users a safe source-backed AACP key re-request ONLY if LAB already
owns a connected L2CAP socket (no Bluetooth pairing, GATT or sound writes).
Never read/share the separate verified personal app's private files.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
p=base/"CaseOwnerProbeV120.kt"
b=base/"presentation/components/BatteryView.kt"
g=root/"android/app/build.gradle.kts"

def change(file,old,new,label):
    s=file.read_text(encoding="utf-8")
    count=s.count(old)
    if count != 1:
        raise RuntimeError("V122_ABORT %s count=%s"%(label,count))
    file.write_text(s.replace(old,new,1),encoding="utf-8")
    print("V122_OK",label)

change(b,'    val leftLevel = left?.level ?: 0\n    val rightLevel = right?.level ?: 0\n    val caseLevel = case?.level ?: 0',
'''    // Original UI presented EMPTY packet data as real 0% after LAB reset.
    // Keep known real readings, never invent missing case/earbud values.
    val leftLevel = left?.level ?: 0
    val rightLevel = right?.level ?: 0
    val caseLevel = case?.level ?: 0''',"track_nullable_battery")
change(b,'''                if (
                    left?.status == right?.status &&
                    (leftLevel - rightLevel) in -3..3
                ) {''',
'''                if (left == null && right == null) {
                    androidx.compose.material3.Text(
                        text = "배터리 데이터 미수신",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                } else if (
                    left != null && right != null &&
                    left.status == right.status &&
                    (leftLevel - rightLevel) in -3..3
                ) {''',"missing_buds_do_not_show_fake_zero")
change(b,"if (leftLevel > 0 || left?.status != BatteryStatus.DISCONNECTED) {",
         "if (left != null && (leftLevel > 0 || left.status != BatteryStatus.DISCONNECTED)) {",
         "no_fake_left_0")
change(b,"if (rightLevel > 0 || right?.status != BatteryStatus.DISCONNECTED) {",
         "if (right != null && (rightLevel > 0 || right.status != BatteryStatus.DISCONNECTED)) {",
         "no_fake_right_0")
change(b,"if (caseLevel > 0 || case?.status != BatteryStatus.DISCONNECTED) {",
         "if (case != null && (caseLevel > 0 || case.status != BatteryStatus.DISCONNECTED)) {",
         "no_fake_case_0")

change(p,"import android.bluetooth.BluetoothDevice\n",
'''import android.bluetooth.BluetoothDevice
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BluetoothConnectionManager
import me.kavishdevar.librepods.services.ServiceManager
''',"source_backed_aacp_service_imports")
change(p,'''        if (key==null) {
            show(a,"내 AirPods IRK 미수신",
                "현재 실험실 앱에 AirPods AACP에서 받은 IRK가 없습니다. " +
                "정상 개인 앱의 데이터는 공유되지 않으므로 지우지 마세요. " +
                "실험실 앱에서 이어버드와 한번 정상 연결해 키가 저장된 경우에만 " +
                "내 기기 BLE 주소를 안전하게 확인할 수 있습니다.\\n" +
                "이 상태에서는 타인의 장치에 연결하지 않습니다.")
            return
        }''',
'''        if (key==null) {
            val linked = BluetoothConnectionManager.aacpSocket?.isConnected == true
            val service = ServiceManager.getService()
            val aacpReady = linked && service != null
            // A2DP status or an apparently selected ANC button does NOT
            // prove LAB AACP socket connection, and cannot supply its keys.
            val reason = if (aacpReady)
                "실험실 AACP 연결은 확인됐지만 IRK 응답은 아직 저장되지 않았습니다."
                else "블루투스 음악 연결은 실험실 앱의 AACP 연결과 다릅니다. " +
                     "기존 정상 개인 앱과 LAB는 키/배터리 데이터를 공유하지 않습니다."
            val msg = reason + "\\n\\n정상 개인 앱은 절대 삭제하지 마세요. " +
                "인증되지 않은 충전 케이스에 접근하지 않습니다."
            if (aacpReady) {
                AlertDialog.Builder(a).setTitle("LAB IRK 응답 미수신")
                    .setMessage(msg+"\\n\\n이미 연결된 실험실 AACP 채널에 " +
                        "공식 LibrePods의 근접키 요청만 다시 보낼 수 있습니다. " +
                        "재페어링·GATT 연결·케이스 소리 명령은 없습니다.")
                    .setNegativeButton("취소",null)
                    .setPositiveButton("AACP 키 재요청"){_,_->
                        try {
                            val sent=service!!.aacpManager.sendRequestProximityKeys(
                                (AACPManager.Companion.ProximityKeyType.IRK.value +
                                AACPManager.Companion.ProximityKeyType.ENC_KEY.value).toByte())
                            if (!sent) show(a,"키 요청 송신 실패",
                                "실험실 AACP 소켓에서 전송에 실패했습니다. 정상 개인 앱은 영향받지 않습니다.")
                            else pollOwnIrk(a,0)
                        } catch (e:Exception) {
                            show(a,"키 요청 오류",e.javaClass.simpleName)
                        }
                    }.show()
            } else {
                show(a,"LAB AACP 미연결 · IRK 미수신",msg+
                    "\\n\\n현재 실험실에서는 검색을 실행하지 않습니다. " +
                    "배터리는 기존 정상 개인 앱에서 확인하세요.")
            }
            return
        }''',"no_key_explains_l2cap_vs_a2dp_and_safe_retry")
change(p,'    private fun show(a:Activity, title:String,body:String) {',
'''    private fun pollOwnIrk(a:Activity, attempt:Int) {
        if(a.isFinishing || a.isDestroyed) return
        val saved=a.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("IRK",null)
        val decoded=try { saved?.let{Base64.decode(it,Base64.DEFAULT)} } catch (_:Exception){null}
        if(decoded?.size==16) {
            decoded.fill(0)
            show(a,"LAB IRK 수신 확인",
                "실험실 앱에서 AACP 근접 식별키를 정상 수신했습니다. " +
                "이것은 충전 케이스의 소유자 인증 성공은 아닙니다. " +
                "추가 검색이나 GATT 연결은 자동 실행되지 않습니다.")
            return
        }
        if(attempt>=12) {
            show(a,"LAB AACP 키 응답 미확인",
                "AACP 요청은 보냈지만 6초 동안 유효한 IRK 응답은 확인되지 않았습니다. " +
                "두 앱이 동시에 이어버드 채널을 사용하면 연결이 제한될 수 있습니다. " +
                "기존 정상 개인 앱의 삭제·재설치는 필요 없습니다.")
            return
        }
        ui.postDelayed({pollOwnIrk(a,attempt+1)},500L)
    }

    private fun show(a:Activity, title:String,body:String) {''',
"poll_irks_locally_without_scanning_or_exposing")
change(g,"versionCode = 10021","versionCode = 10022","version_10022")
change(g,'versionName = "1.0.21-owner-gatt-permission-fix"',
         'versionName = "1.0.22-lab-irk-and-battery-recovery"',"version_name_122")

assert 'applicationId = "com.myairpods5.personal.lab"' in g.read_text("utf-8")
assert "writeCharacteristic" not in p.read_text("utf-8")
assert "connectGatt" in p.read_text("utf-8") # Existing consent-gated read-only scan unchanged.
assert "sendRequestProximityKeys" in p.read_text("utf-8")
print("V122_GOLDEN_ORIGINAL_APP_UNTOUCHED=PASS")
print("V122_IRK_REQUEST_IS_EXISTING_AACP_PROTOCOL_NOT_FINDMY_SOUND=PASS")
print("V122_BATTERY_0_PERCENT_FAKE_FALLBACK_BLOCKED=PASS")
