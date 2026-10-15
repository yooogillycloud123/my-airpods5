package me.kavishdevar.librepods

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import java.lang.ref.WeakReference
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Solves the 147 *target selection* defect (connecting GATT to Classic earbud
 * address, not to the advertising case). Only decrypts advertisements with
 * this OWN AirPods' proximity encryption key; other devices are not eligible.
 * Case Find My speaker GATT protocol still has to be proven on AirPods5.
 */
object CaseBeaconFinder {
    private const val APPLE = 76
    private const val CASE_DURATION_MS = 35000L
    private val main = Handler(Looper.getMainLooper())
    private var active: Session? = null

    fun open(activity: Activity) {
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            dialog(activity, "권한 필요", "블루투스 주변 기기 연결·검색 권한을 허용해주세요.")
            return
        }
        val pref=activity.getSharedPreferences("settings",Context.MODE_PRIVATE)
        val enc=pref.getString("ENC_KEY",null)
        val key=try { if (enc.isNullOrBlank()) null else Base64.decode(enc, Base64.DEFAULT) }
                catch (_: Exception) { null }
        if (key==null || key.size!=16) {
            dialog(activity,"케이스 암호화 식별키 없음",
                "에어팟과 현재 실험실 앱 사이에 AACP 연결이 아직 성립되지 않아 내 케이스의 " +
                "변경되는 BLE 주소를 안전하게 식별할 키가 없습니다.\n\n" +
                "실험실 앱에서 내 에어팟이 실제 연결되고 배터리가 갱신되도록 한 뒤 다시 확인해주세요. " +
                "다른 기기의 주소로 임의 연결하지 않습니다.")
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("내 케이스 전파 직접 검색")
            .setMessage("이전 오류 147은 블루투스 LE 연결 시간 초과입니다. " +
                "이어버드의 저장된 블루투스 주소는 실제 케이스 BLE 주소와 다릅니다.\n\n" +
                "이번에는 약 35초 동안 주변 광고 신호만 읽고, 내 에어팟에서 받은 키로 " +
                "복호화되는 케이스 신호만 찾아 연결 가능 여부를 확인합니다.\n" +
                "찾지 못하면 연결을 시도하지 않습니다.")
            .setNegativeButton("취소",null)
            .setPositiveButton("35초 검색") { _,_ ->
                active?.close()
                val x=Session(activity,key)
                active=x
                x.begin()
            }.show()
    }

    private fun dialog(a:Activity,title:String,body:String) {
        if(!a.isFinishing && !a.isDestroyed) AlertDialog.Builder(a)
            .setTitle(title).setMessage(body).setPositiveButton("확인",null).show()
    }
    private class Session(a:Activity,private val key:ByteArray):ScanCallback() {
        private val host=WeakReference(a)
        private val seen=mutableMapOf<String,Int>()
        private var scanner:BluetoothLeScanner?=null
        private var done=false
        private var total=0
        private var candidateFrames=0
        private var verified=0
        private var lastRssi=0
        private var found:BluetoothDevice?=null
        private var foundConnectable=false

        private fun report(title:String,body:String) {
            host.get()?.let{ a->
                AirPodsCrashDiary.mark(a,"CASE_ADDR_SEARCH "+title+" "+body.take(250))
                main.post {
                    if(!a.isFinishing&&!a.isDestroyed) AlertDialog.Builder(a)
                        .setTitle(title).setMessage(body)
                        .setNegativeButton("닫기",null)
                        .setPositiveButton("결과 복사"){_,_->
                            val clip=a.getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clip.setPrimaryClip(android.content.ClipData.newPlainText(
                                "Case scan evidence",title+"\n"+body))
                        }.show()
                }
            }
        }
        @SuppressLint("MissingPermission")
        fun begin() {
            val a=host.get() ?:return
            try {
                scanner=(a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                    .adapter.bluetoothLeScanner
                if(scanner==null){report("BLE 스캔 불가","스캐너가 준비되지 않았습니다.");return}
                val mask=byteArrayOf(0xff.toByte(),0xff.toByte(),0xff.toByte())
                val filter=ScanFilter.Builder()
                    .setManufacturerData(APPLE, byteArrayOf(0x07,0x11,0x06),mask)
                    .build()
                scanner?.startScan(listOf(filter),ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),this)
                main.postDelayed({finishScan()},CASE_DURATION_MS)
            }catch(e:Exception){
                report("BLE 스캔 예외","오류: ${e.javaClass.simpleName}: ${e.message}")
                close()
            }
        }
        override fun onScanResult(callbackType:Int,result:ScanResult) {
            if(done)return
            val payload=result.scanRecord?.getManufacturerSpecificData(APPLE) ?:return
            total++
            if(payload.size!=19 || payload[0]!=0x07.toByte() ||
                payload[1]!=0x11.toByte()|| payload[2]!=0x06.toByte())return
            candidateFrames++
            val block=try {
                Cipher.getInstance("AES/ECB/NoPadding").run{
                    init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"))
                    doFinal(payload.copyOfRange(3,19))
                }
            } catch (_:Exception) {return}
            // Source-backed case frame: 29 20 header; fixed 8,10,11 zero;
            // battery fields <=100 or 0xff absent. Not cryptographic MAC,
            // but wrong keys almost never match this structured plaintext.
            if(block[0]!=0x29.toByte() || block[1]!=0x20.toByte() ||
                block[8]!=0.toByte()||block[10]!=0.toByte()||block[11]!=0.toByte())return
            val cells=intArrayOf(block[3].toInt() and 0x7f,block[4].toInt() and 0x7f,
                block[5].toInt() and 0x7f)
            if(cells.any{it>100 && it!=127})return
            verified++
            val addr=result.device.address
            val n=(seen[addr]?:0)+1
            seen[addr]=n
            if(n>=2 && found==null) {
                found=result.device
                foundConnectable=result.isConnectable
                lastRssi=result.rssi
            }
        }
        override fun onScanFailed(errorCode:Int) {
            if(done)return
            report("BLE 스캔 오류","scanError=$errorCode, 광고 수신=$total")
            close()
        }

        @SuppressLint("MissingPermission")
        private fun finishScan() {
            if(done)return
            done=true
            try{ scanner?.stopScan(this) }catch (_:Exception){}
            scanner=null
            if(active===this)active=null
            val a=host.get()?:return
            val summary="전체 Apple 케이스형 신호: $candidateFrames / " +
                "내 암호화키 일치: $verified / 동일 주소 2회 확인: ${found!=null}\n" +
                "RSSI: $lastRssi dBm / 연결 가능 광고: $foundConnectable\n" +
                "주소는 표시·저장하지 않습니다."
            AirPodsCrashDiary.mark(a,"CASE_BEACON_SCAN "+summary)
            main.post{
                if(a.isDestroyed||a.isFinishing)return@post
                if(found==null){
                    report("내 케이스 신호 미확인",
                        summary+"\n\n오류 147을 재시도하지 않았습니다. " +
                        "케이스가 광고를 보내는 시점·폰 BLE 검색 조건·암호화키를 확인해야 합니다.")
                }else if(!foundConnectable){
                    report("내 케이스 확인 · 연결 불가 신호",summary+
                        "\n\n연결할 수 없는 BLE 광고이므로 GATT 연결을 강행하지 않았습니다.")
                }else{
                    AlertDialog.Builder(a).setTitle("내 케이스 BLE 주소 확인")
                        .setMessage(summary+"\n\n실제 케이스 주소로 GATT 서비스를 조사하겠습니까? " +
                            "검증된 서비스가 없다면 아무런 소리 명령도 보내지 않습니다.")
                        .setNegativeButton("취소",null)
                        .setPositiveButton("GATT 조사") {_,_->
                            found?.let { CaseSpeakerLab.openVerifiedCase(a,it) }
                        }.show()
                }
            }
        }
        @SuppressLint("MissingPermission")
        fun close() {
            if(done)return
            done=true
            try{scanner?.stopScan(this)}catch (_:Exception){}
            scanner=null
            if(active===this)active=null
        }
    }
}
