package me.kavishdevar.librepods

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * V124: source-backed case advertising preflight, NO Find My sound writes.
 * Recorded 0x07/0x11/0x06 block is from AirPods Pro 3, NOT verified AirPods 5.
 * Never equate an encrypted frame with a case-owner credential or speaker access.
 * App's own ENC_KEY only; never attempt to access another package's private store.
 */
object CaseResearchMenuV124 {
    private val ui=Handler(Looper.getMainLooper())
    private var session: ReadOnlySession?=null
    private const val DURATION_MS=10_000L

    fun open(a:Activity) {
        if (a.isFinishing || a.isDestroyed) return
        val prefs=a.getSharedPreferences("settings",Context.MODE_PRIVATE)
        val raw=prefs.getString("ENC_KEY",null)
        val key=try { raw?.let{Base64.decode(it,Base64.DEFAULT)} } catch(_:Exception){null}
        val valid=key?.size==16
        if(!valid) key?.fill(0)
        val status=if(valid) "LAB ENC_KEY: 16바이트 저장됨 (본인 케이스 인증을 의미하지 않음)" else
            "LAB ENC_KEY: 없음. 기존 GOLDEN과 독립 데이터여서 자동 공유되지 않음."
        AlertDialog.Builder(a).setTitle("케이스 찾기 연구 · v1.0.24")
            .setMessage("최종 목표: 양쪽 이어버드가 들어간 닫힌 케이스 자체의 삐삐 찾기 소리입니다.\n\n" +
                "현재 소유자 인증/Find My 케이스 스피커 명령은 아직 확보되지 않았습니다. 소리 재생은 실행하지 않습니다.\n\n" +
                "기존 IRK 전용 주소탐색과 별도로, 공개 AirPods Pro 3에서 확인된 07 11 06 케이스 광고를 " +
                "10초 동안 수동 확인합니다. AirPods 5의 동일 동작은 미확인입니다.\n\n"+status+
                "\n\n기기 주소·키·광고 원문은 출력하지 않습니다.")
            .setNegativeButton("닫기"){_,_->key?.fill(0)}
            .setNeutralButton("기존 GATT 조사"){_,_->key?.fill(0);CaseOwnerProbeV120.open(a)}
            .setPositiveButton("독립 광고 10초"){_,_->
                if(!valid) key?.fill(0)
                if(ensurePermissions(a)) {
                    session?.close()
                    session=ReadOnlySession(a,if(valid)key else null).also{it.start()}
                }else {
                    key?.fill(0)
                }
            }.setOnCancelListener{key?.fill(0)}
            .show()
    }

    private fun ensurePermissions(a:Activity):Boolean {
        val needed=mutableListOf<String>()
        for(p in listOf(Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT)) {
            if(a.checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED) needed.add(p)
        }
        if(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if(needed.isEmpty()) return true
        AlertDialog.Builder(a).setTitle("BLE 권한 필요")
            .setMessage("주변 기기 및 정확한 위치 권한을 허용한 후 같은 연구 메뉴를 다시 눌러 주세요. "+
                "자동으로 검색을 시작하지 않습니다. GPS 위치는 수집하지 않습니다.")
            .setNegativeButton("취소",null)
            .setPositiveButton("권한 허용"){_,_->a.requestPermissions(needed.distinct().toTypedArray(),124)}
            .show()
        return false
    }

    private fun show(a:Activity,title:String,message:String) {
        ui.post {
            if(a.isFinishing||a.isDestroyed)return@post
            AlertDialog.Builder(a).setTitle(title).setMessage(message)
                .setPositiveButton("닫기",null).show()
        }
    }

    private class ReadOnlySession(
        private val a:Activity,
        private val key:ByteArray?
    ): ScanCallback() {
        private var scanner:BluetoothLeScanner?=null
        private val done=AtomicBoolean(false)
        private var apple=0
        private var other07=0
        private var shape=0
        private var decrypted=0
        private var closed=0
        private var opened=0
        private var lastBatteries:String?=null
        private var aborted:String?=null

        @SuppressLint("MissingPermission")
        fun start(){
            try {
                val manager=a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                scanner=manager.adapter.bluetoothLeScanner
                if(scanner==null){finish("BLE 스캐너 준비되지 않음");return}
                scanner!!.startScan(null,ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),this)
                ui.postDelayed({finish(null)},DURATION_MS)
            }catch(e:Exception){finish("검색 시작 실패: "+e.javaClass.simpleName)}
        }

        override fun onScanResult(callbackType:Int,result:ScanResult){
            if(done.get())return
            val data=result.scanRecord?.getManufacturerSpecificData(0x004C) ?: return
            apple++
            if(data.isEmpty() || (data[0].toInt() and 0xff)!=0x07)return
            if(data.size!=19 || (data[1].toInt() and 0xff)!=0x11 ||
                (data[2].toInt() and 0xff)!=0x06) {
                other07++;return
            }
            shape++
            val enc=key ?: return
            try {
                val cipher=Cipher.getInstance("AES/ECB/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(enc,"AES"))
                val block=cipher.doFinal(data.copyOfRange(3,19))
                if(block[8].toInt()!=0 || block[10].toInt()!=0 || block[11].toInt()!=0){
                    block.fill(0);return
                }
                decrypted++
                if((block[2].toInt() and 0x08)!=0)closed++ else opened++
                val left=displayBattery(block[4])
                val right=displayBattery(block[5])
                val caseValue=displayBattery(block[3])
                lastBatteries="케이스: $caseValue / 왼쪽: $left / 오른쪽: $right"
                block.fill(0)
            }catch(_:Exception){}
        }

        private fun displayBattery(value:Byte):String {
            val v=value.toInt() and 0xff
            if(v==0xff)return "부재"
            val percent=v and 0x7f
            if(percent>100)return "미상"
            return "$percent%"+(if((v and 0x80)!=0) " 충전중" else "")
        }

        override fun onScanFailed(errorCode:Int){finish("Android BLE 오류 $errorCode")}

        @SuppressLint("MissingPermission")
        private fun finish(error:String?) {
            if(!done.compareAndSet(false,true))return
            try{scanner?.stopScan(this)}catch(_:Exception){}
            scanner=null
            key?.fill(0)
            val message="Apple 광고: $apple\n" +
                "07 11 06 형식: $shape\n" +
                "다른 07 형식: $other07\n" +
                "저장된 ENC_KEY 형식복호화 일치: $decrypted\n" +
                "일치 자료의 닫힘/열림: $closed / $opened\n" +
                (lastBatteries?.let{it+"\n"} ?: "") +
                (error?.let{"상태: $it\n"} ?: "") +
                "\n형식 일치라도 실제 내 AirPods 5 케이스 신원/소유자 인증은 미확인. "+
                "이번 기능은 케이스 스피커 소리 재생 명령을 보내지 않았습니다."
            if(session===this)session=null
            show(a,"케이스 독립 광고 · 읽기 전용 결과",message)
        }
        @SuppressLint("MissingPermission")
        fun close(){
            if(!done.compareAndSet(false,true))return
            try{scanner?.stopScan(this)}catch(_:Exception){}
            scanner=null
            key?.fill(0)
        }
    }
}
