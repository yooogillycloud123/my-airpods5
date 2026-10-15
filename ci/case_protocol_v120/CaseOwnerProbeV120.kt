package me.kavishdevar.librepods

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import caseproto.CaseRpaIdentity
import caseproto.KeySource
import caseproto.RpaDecision
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Source-backed AirPods BLE owner-identity preflight.
 * This makes no guess about Apple Find My case sound GATT opcodes.
 * Resolves only the user's AACP IRK against BLE RPA addresses; matching the
 * earbud IRK does NOT prove that the peripheral is the separate charging case.
 * The optional user-confirmed GATT step discovers UUIDs, writes NOTHING.
 */
object CaseOwnerProbeV120 {
    private val ui = Handler(Looper.getMainLooper())
    private var current: Session? = null

    fun open(a: Activity) {
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
        val raw = a.getSharedPreferences("settings",Context.MODE_PRIVATE)
            .getString("IRK",null)
        val key = try {
            raw?.let { Base64.decode(it, Base64.DEFAULT) }?.takeIf{it.size==16}
        } catch (_:Exception) {null}
        if (key==null) {
            show(a,"내 AirPods IRK 미수신",
                "현재 실험실 앱에 AirPods AACP에서 받은 IRK가 없습니다. " +
                "정상 개인 앱의 데이터는 공유되지 않으므로 지우지 마세요. " +
                "실험실 앱에서 이어버드와 한번 정상 연결해 키가 저장된 경우에만 " +
                "내 기기 BLE 주소를 안전하게 확인할 수 있습니다.\n" +
                "이 상태에서는 타인의 장치에 연결하지 않습니다.")
            return
        }
        AlertDialog.Builder(a).setTitle("케이스 제어 경로 · 한 번만 확인")
            .setMessage("공개 LibrePods의 AES-IRK 주소 확인 알고리즘으로 " +
                "내 AirPods 관련 BLE 주소만 12초 검색합니다. " +
                "발견되더라도 케이스 자체임을 증명하는 것은 아닙니다.\n\n" +
                "연결 가능한 대상이 확인되면 별도 동의를 받은 뒤 " +
                "읽기 전용 GATT 서비스 조회만 진행합니다. " +
                "찾기 소리 명령은 인증 방식이 입증되지 않아 보내지 않습니다. " +
                "기기 주소·키는 결과에 포함되지 않습니다.")
            .setNegativeButton("취소",null)
            .setPositiveButton("12초 식별") {_,_->
                current?.close()
                Session(a,key).also {current=it;it.start()}
            }.show()
    }

    private fun show(a:Activity, title:String,body:String) {
        if(a.isFinishing||a.isDestroyed)return
        AlertDialog.Builder(a).setTitle(title).setMessage(body)
            .setNegativeButton("닫기",null)
            .setPositiveButton("결과 복사"){_,_->
                val clipboard=a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Case owner probe",title+"\n"+body))
            }.show()
    }

    private class Session(private val a:Activity, private val irk:ByteArray): ScanCallback() {
        private var scanner:android.bluetooth.le.BluetoothLeScanner?=null
        private var target:BluetoothDevice?=null
        private var gatt:BluetoothGatt?=null
        private val done=AtomicBoolean(false)
        private var reports=0
        private var matches=0
        private var appleMatches=0
        private var connectableMatches=0
        private var latestRssi:Int?=null
        private var matchDevice:BluetoothDevice?=null
        private var scanStopped=false

        @SuppressLint("MissingPermission")
        fun start(){
            try{
                scanner=(a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                    .adapter.bluetoothLeScanner
                if(scanner==null){
                    finish("BLE 검색 불가","Android Bluetooth LE Scanner가 준비되지 않았습니다.");return
                }
                scanner?.startScan(null,ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),this)
                ui.postDelayed({onScanEnd()},12000L)
            }catch(e:Exception){finish("BLE 검색 오류",e.javaClass.simpleName)}
        }

        override fun onScanResult(callbackType:Int,result:ScanResult){
            if(done.get()||scanStopped)return
            reports++
            val decision=try{
                CaseRpaIdentity.assess(result.device.address,irk,KeySource.EARBUD_AACP_PROXIMITY)
            }catch(_:Exception){RpaDecision.INVALID_INPUT}
            if(decision!=RpaDecision.MATCHES_KEY_BUT_CASE_IDENTITY_UNPROVEN)return
            matches++
            if(result.scanRecord?.getManufacturerSpecificData(0x004c)!=null) appleMatches++
            if(result.isConnectable){
                connectableMatches++
                if(matchDevice==null) {
                    matchDevice=result.device
                    latestRssi=result.rssi
                }
            }
        }

        override fun onScanFailed(errorCode:Int){
            finish("BLE 스캔 실패","Android 오류 코드: "+errorCode)
        }

        @SuppressLint("MissingPermission")
        private fun stopScan(){
            if(scanStopped)return
            scanStopped=true
            try{scanner?.stopScan(this)}catch(_:Exception){}
            scanner=null
        }

        private fun onScanEnd(){
            if(done.get())return
            stopScan()
            val body="전체 BLE 콜백: $reports\n" +
                "내 이어버드 IRK 주소 일치: $matches\n" +
                "그중 Apple 광고: $appleMatches\n" +
                "그중 연결 가능: $connectableMatches\n" +
                "일치 후보 중앙 아닌 마지막 RSSI: ${latestRssi?.toString() ?: "미측정"}\n" +
                "실제 충전 케이스 자체 신원: 미확인\n" +
                "Find My 인증: 미확인"
            val device=matchDevice
            if(device==null){
                finish("케이스 제어 대상 미확인",body+"\n\n이어버드 IRK와 일치하는 연결 가능한 BLE 주소가 없습니다. " +
                    "다른 기기 주소를 추측해 GATT 접근하지 않습니다.")
                return
            }
            if(a.isFinishing||a.isDestroyed){close();return}
            AlertDialog.Builder(a).setTitle("연관된 BLE 주소 식별")
                .setMessage(body+"\n\n이 키는 이어버드에서 전달된 것으로, " +
                    "독립 충전 케이스라고 증명하지 못합니다. " +
                    "사용자 동의 시에만 10초 이내 GATT 서비스 UUID를 조회합니다. " +
                    "어떠한 쓰기 명령·소리 재생도 하지 않습니다.")
                .setNegativeButton("종료"){_,_->finish("대상 비교만 완료",body)}
                .setPositiveButton("GATT 읽기 조사"){_,_-> startGatt(device,body)}
                .setOnCancelListener{close()}.show()
        }

        @SuppressLint("MissingPermission")
        private fun startGatt(device:BluetoothDevice, context:String){
            if(done.get())return
            val gattCallback=object:BluetoothGattCallback(){
                override fun onConnectionStateChange(g:BluetoothGatt,status:Int,state:Int){
                    if(done.get())return
                    if(status==BluetoothGatt.GATT_SUCCESS && state==BluetoothProfile.STATE_CONNECTED){
                        try{
                            if(!g.discoverServices())finish("서비스 검색 시작 실패",context)
                        }catch(e:Exception){finish("서비스 검색 예외",e.javaClass.simpleName)}
                    }else if(state==BluetoothProfile.STATE_DISCONNECTED){
                        finish("GATT 연결 불가",context+"\nGATT 종료 코드=$status (147은 timeout)")
                    }
                }
                override fun onServicesDiscovered(g:BluetoothGatt,status:Int){
                    if(done.get())return
                    val services=g.services
                    val fd44=services.any { it.uuid.toString()
                        .startsWith("0000fd44-",true) }
                    val ownerPoint=services.any { service->service.characteristics.any{
                        it.uuid.toString().startsWith("4f860002-",true)
                    }}
                    val nonOwnerPoint=services.any { service->service.characteristics.any{
                        it.uuid.toString().startsWith("4f860003-",true)
                    }}
                    val uuids=services.map { it.uuid.toString() }.sorted()
                        .take(15).joinToString(", ")
                    finish("읽기 전용 GATT 서비스 확인",context+
                        "\n검색상태=$status / 서비스수=${services.size}"+
                        "\nFD44 서비스=$fd44"+
                        "\n소유자 제어특성 후보=$ownerPoint"+
                        "\n비소유자 제어특성 후보=$nonOwnerPoint"+
                        "\n서비스 UUID: $uuids"+
                        "\n\n발견 자체는 실제 케이스 인증/소리 성공이 아닙니다. "+
                        "명령 쓰기는 수행하지 않았습니다.")
                }
            }
            try{
                target=device
                gatt=device.connectGatt(a.applicationContext,false,gattCallback,BluetoothDevice.TRANSPORT_LE)
                ui.postDelayed({ if(!done.get())finish("GATT 조회 시간 초과",context+
                    "\n10초 내 연결/서비스 검색 완료되지 않음. 소리 명령 없음.") },10000L)
            }catch(e:Exception){finish("GATT 조사 오류",context+"\n"+e.javaClass.simpleName)}
        }

        private fun finish(title:String,body:String){
            if(!done.compareAndSet(false,true))return
            stopScan()
            try{gatt?.disconnect()}catch(_:Exception){}
            try{gatt?.close()}catch(_:Exception){}
            gatt=null
            irk.fill(0)
            if(current===this)current=null
            ui.post{show(a,title,body)}
        }

        @SuppressLint("MissingPermission")
        fun close(){
            if(!done.compareAndSet(false,true))return
            stopScan()
            try{gatt?.disconnect()}catch(_:Exception){}
            try{gatt?.close()}catch(_:Exception){}
            gatt=null
            irk.fill(0)
            if(current===this)current=null
        }
    }
}
