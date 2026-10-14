package me.kavishdevar.librepods

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RESEARCH/LAB ONLY. Find My case sound uses a different path from charging
 * case tones (AACP 0x31). This GATT path is documented for Find My accessories,
 * NOT verified on AirPods 5 case. Only a paired, app-configured AirPods target
 * may be probed. Never scans/unpaired-address-enumerates/randomly writes.
 */
object CaseSpeakerLab {
    private val handler = Handler(Looper.getMainLooper())
    private val service = UUID.fromString("0000fd44-0000-1000-8000-00805f9b34fb")
    private val ownerControl = UUID.fromString("4f860002-943b-49ef-bed4-2f730304427a")
    private val startSound = byteArrayOf(0x01, 0x00, 0x02)
    private val stopSound = byteArrayOf(0x01, 0x01, 0x02)
    private var active: Probe? = null

    fun open(activity: Activity) {
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            message(activity, "블루투스 연결 권한 필요",
                "설정에서 주변 기기 연결 권한을 허용한 뒤 다시 확인해주세요.")
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("케이스 찾기 소리 · 연구 기능")
            .setMessage(
                "소리 스피커가 탑재된 AirPods 5 무선충전 케이스 전용 실험입니다.\n\n" +
                "애플의 나의 찾기와 같은 기능을 완성했다는 뜻이 아닙니다. " +
                "우선 현재 앱에 등록된 내 에어팟의 BLE 서비스에서 소리 명령 경로를 " +
                "읽기 전용으로 확인합니다. 지원하는 소리 제어 특성이 발견될 때만 " +
                "별도 동의를 받고 소리 명령을 시도합니다.\n\n" +
                "실행 중 음악/블루투스에 잠깐 영향을 줄 수 있습니다."
            )
            .setNegativeButton("취소", null)
            .setPositiveButton("기능 확인") { _, _ -> probe(activity) }
            .show()
    }

    private fun message(activity: Activity, title: String, body: String) {
        if (!activity.isDestroyed && !activity.isFinishing) {
            AlertDialog.Builder(activity).setTitle(title).setMessage(body)
                .setPositiveButton("확인", null).show()
        }
    }

    private fun report(activity: Activity, title: String, body: String) {
        AirPodsCrashDiary.mark(activity, "CASE_GATT " + title + " " + body.take(220))
        if (!activity.isDestroyed && !activity.isFinishing) {
            AlertDialog.Builder(activity).setTitle(title).setMessage(body)
                .setNegativeButton("닫기", null)
                .setPositiveButton("진단 복사") { _, _ ->
                    val clip = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("Case find GATT status", title + "\n" + body))
                }.show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun probe(activity: Activity) {
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) return
        val address = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("mac_address", "")?.trim().orEmpty()
        val adapter = (activity.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager)?.adapter
        val target = try {
            adapter?.bondedDevices?.firstOrNull {
                it.address.equals(address, ignoreCase = true) && it.bondState == BluetoothDevice.BOND_BONDED
            }
        } catch (_: Exception) { null }
        if (target == null) {
            report(activity, "에어팟 대상 검증 실패",
                "개인용 앱에 저장된 블루투스 주소와 일치하는 페어링된 기기가 없습니다. " +
                "다른 사람의 블루투스 기기에는 접속하지 않습니다.")
            return
        }
        active?.close()
        val p = Probe(activity, target)
        active = p
        p.start()
    }

    private class Probe(activity: Activity, private val device: BluetoothDevice) :
        BluetoothGattCallback() {
        private val ref = WeakReference(activity)
        private val finished = AtomicBoolean(false)
        private var gatt: BluetoothGatt? = null
        private var owner: BluetoothGattCharacteristic? = null
        private var started = false
        private var stopping = false

        private fun log(text: String) {
            ref.get()?.let { AirPodsCrashDiary.mark(it, "CASE_GATT " + text) }
        }
        private fun ui(f: (Activity) -> Unit) {
            handler.post {
                val a = ref.get()
                if (a != null && !a.isDestroyed && !a.isFinishing) f(a)
            }
        }

        @SuppressLint("MissingPermission")
        fun start() {
            val a = ref.get() ?: return
            try {
                log("owner-bonded target GATT probe started, no BT address retained")
                gatt = device.connectGatt(a.applicationContext, false, this, BluetoothDevice.TRANSPORT_LE)
                if (gatt == null) finish("BLE 접근 실패", "BLE GATT 연결을 시작할 수 없습니다.")
                handler.postDelayed({
                    if (!finished.get() && owner == null) {
                        finish("서비스 확인 시간 초과",
                            "12초 안에 케이스 찾기용 제어 서비스를 확인하지 못했습니다. " +
                            "에어팟의 이어버드 Bluetooth 주소와 케이스의 BLE 주소는 다를 수 있습니다.")
                    }
                }, 12000)
            } catch (e: Exception) {
                finish("BLE 연결 오류", "${e.javaClass.simpleName}: ${e.message}")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
            if (finished.get()) return
            if (status == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                log("GATT connected; discovering services")
                try { if (!g.discoverServices()) finish("서비스 검색 실패", "discoverServices 반환 false") }
                catch (e: Exception) { finish("서비스 검색 오류", "${e.javaClass.simpleName}") }
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                finish("BLE 연결 종료", "상태코드: $status (GATT 연결이 허용되지 않았을 수 있음)")
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (finished.get()) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                finish("서비스 확인 실패", "서비스 조회 상태코드: $status")
                return
            }
            val cap = g.getService(service)?.getCharacteristic(ownerControl)
            if (cap == null) {
                val present = g.getService(service) != null
                finish("케이스 소리 경로 미확인",
                    if (present) "Find My FD44 서비스는 있지만 인증된 사용자용 소리 제어 특성은 없습니다. " +
                        "AirPods 5는 별도의 보호된 프로토콜을 사용할 수 있습니다. 음향 재생 명령을 보내지 않았습니다."
                    else "이 페어링된 주소에서는 Find My FD44 서비스를 발견하지 못했습니다. " +
                        "케이스의 BLE 송신 주소가 별개이거나 기능 접근이 제한됐을 수 있습니다. " +
                        "음향 재생 명령을 보내지 않았습니다.")
                return
            }
            if ((cap.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
                finish("소리 명령 미지원",
                    "소리 제어 특성을 발견했지만 안전한 응답형 Write 속성이 없습니다. 명령을 보내지 않았습니다.")
                return
            }
            owner = cap
            log("paired-owner FD44/4f860002 discovered; no writes yet")
            ui { a ->
                AlertDialog.Builder(a)
                    .setTitle("실험용 소리 제어 경로 감지")
                    .setMessage("연결된 내 에어팟에서 Find My 형태의 소리 제어 특성이 감지됐습니다.\n\n" +
                        "이 경로는 연구 자료에 공개된 프로토콜로, AirPods 5에서 소리가 난다고 검증된 것은 아닙니다. " +
                        "약 4초 동안 소리 명령을 시도한 뒤 중단합니다. 시도할까요?")
                    .setNegativeButton("취소") { _, _ -> close() }
                    .setPositiveButton("4초 소리 시험") { _, _ -> play() }
                    .setOnCancelListener { close() }
                    .show()
            }
        }

        @SuppressLint("MissingPermission")
        private fun write(values: ByteArray): Boolean {
            val g = gatt ?: return false
            val ch = owner ?: return false
            return try {
                ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    g.writeCharacteristic(ch, values, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                        BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    ch.value = values
                    @Suppress("DEPRECATION")
                    g.writeCharacteristic(ch)
                }
            } catch (e: Exception) {
                log("write threw ${e.javaClass.simpleName} ${e.message}")
                false
            }
        }

        fun play() {
            if (finished.get() || owner == null) return
            log("experimental owner START attempted")
            if (!write(startSound)) {
                finish("소리 시험 실패", "블루투스에서 실험용 소리 시작 명령을 받아들이지 않았습니다.")
                return
            }
            started = true
            handler.postDelayed({ stop() }, 4000)
            handler.postDelayed({ if (!finished.get()) stop() }, 6000)
        }

        @SuppressLint("MissingPermission")
        private fun stop() {
            if (finished.get() || stopping) return
            stopping = true
            log("experimental owner STOP attempted")
            val queued = write(stopSound)
            handler.postDelayed({
                finish("실험용 소리 명령 전송 종료",
                    if (queued) "약 4초 동안 소리 시작/중지 명령을 BLE로 전송했습니다. " +
                        "전송 성공 여부는 실제 케이스에서 소리가 난다는 뜻이 아닙니다. " +
                        "소리가 들렸는지 직접 확인해 주세요."
                    else "시작 명령 후 중지 명령이 전송되지 않았습니다. " +
                        "케이스에서 소리가 나면 잠시 기다려 보세요.")
            }, 1000)
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            log("characteristic write ACK status=$status stop=$stopping")
        }

        @SuppressLint("MissingPermission")
        fun close() {
            if (finished.compareAndSet(false, true)) {
                try { gatt?.disconnect() } catch (_: Exception) {}
                try { gatt?.close() } catch (_: Exception) {}
                gatt = null
                if (active === this) active = null
            }
        }
        private fun finish(title: String, text: String) {
            if (finished.get()) return
            log("result " + title + " " + text.take(100))
            ui { a -> report(a, title, text) }
            close()
        }
    }
}
