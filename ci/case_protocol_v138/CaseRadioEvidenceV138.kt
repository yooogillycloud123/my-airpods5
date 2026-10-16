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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import java.util.LinkedHashMap
import java.util.HashSet
import java.util.concurrent.atomic.AtomicBoolean

/**
 * v138 evidence-first. v136's filter 12:19 only observes separated Find My
 * accessories. AirPods 5 proximity is Apple 07:19, 0x3020 / 0x3620.
 * NEVER equate either unpaired radio signature with an OWN charging case.
 */
object CaseRadioEvidenceV138 {
    private val main = Handler(Looper.getMainLooper())
    private var current: Session? = null

    fun open(a: Activity) {
        if (a.isFinishing || a.isDestroyed) return
        val required = listOf(Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = required.filter {
            a.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            AlertDialog.Builder(a).setTitle("BLE 검색 권한 필요")
                .setMessage("이 실험용 앱에 주변 기기와 정확한 위치 권한이 필요합니다. 위치 좌표는 수집하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("권한 요청") { _, _ ->
                    a.requestPermissions(missing.toTypedArray(), 138)
                }.show()
            return
        }
        AlertDialog.Builder(a).setTitle("AirPods 5 / Find My 신호 분리 · LAB")
            .setMessage(
                "지난 앱은 12:19 분리 추적기만 후보로 세어 AirPods 주변 광고 07:19를 놓쳤습니다.\n\n" +
                "이번에는 12초 동안 Apple 광고 유형을 각각 집계합니다. 07:19의 0x3020/0x3620 표기는 AirPods 5 모델 단서일 뿐 충전 케이스 신원 증명은 아닙니다.\n\n" +
                "다른 사람 기기에 연결하거나 소리 명령을 보내지 않습니다. 자동 재검색도 없습니다."
            )
            .setNegativeButton("취소", null)
            .setPositiveButton("한 번 확인") { _, _ ->
                current?.close()
                current = Session(a).also { it.start() }
            }.show()
    }

    private fun dialog(a: Activity, title: String, body: String) {
        main.post {
            if (!a.isFinishing && !a.isDestroyed)
                AlertDialog.Builder(a).setTitle(title).setMessage(body)
                    .setPositiveButton("확인", null).show()
        }
    }

    private data class Candidate(val device: BluetoothDevice, val rssi: Int)
    private data class Snapshot(
        val all: Int, val apple: Int, val proximity07: Int, val proximity5: Int,
        val proximity5wireless: Int, val proximity5standard: Int,
        val other07: Int, val findmy19: Int, val findmy02: Int,
        val casePro3: Int, val nearInfo10: Int, val generic12: Int,
        val candidates: List<Candidate>
    )

    @SuppressLint("MissingPermission")
    private class Session(private val a: Activity) : ScanCallback() {
        private val done = AtomicBoolean(false)
        private val lock = Any()
        private var scanner: android.bluetooth.le.BluetoothLeScanner? = null
        private var gatt: BluetoothGatt? = null
        private var identityProbe: CaseIdentityReadV137? = null
        private val candidates = LinkedHashMap<String, Candidate>()
        private var all = 0
        private var apple = 0
        private var proximity07 = 0
        private var proximity5 = 0
        private var proximity5wireless = 0
        private var proximity5standard = 0
        private var other07 = 0
        private var findmy19 = 0
        private var findmy02 = 0
        private var casePro3 = 0
        private var nearInfo10 = 0
        private var generic12 = 0

        fun start() {
            try {
                val adapter = (a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                scanner = adapter.bluetoothLeScanner
                if (scanner == null) {
                    close()
                    dialog(a, "검색할 수 없음", "Android BLE 스캐너가 없습니다.")
                    return
                }
                scanner?.startScan(null, ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setReportDelay(0L)
                    .build(), this)
                main.postDelayed({ showReport() }, 12000L)
            } catch (e: Exception) {
                close()
                dialog(a, "BLE 검색 오류", e.javaClass.simpleName)
            }
        }
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            observe(result)
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (result in results) observe(result)
        }

        private fun observe(result: ScanResult) {
            if (done.get()) return
            synchronized(lock) {
                if (done.get()) return
                all++
                val data = result.scanRecord?.getManufacturerSpecificData(0x004c) ?: return
                apple++
                if (data.size < 2) return
                val kind = data[0].toInt() and 255
                val subtype = data[1].toInt() and 255
                if (kind == 7) {
                    if (subtype == 25) {
                        proximity07++
                        // LibrePods/CAPod: Apple Continuity 07:19, model in bytes 3,4.
                        if (data.size >= 27 && (data[2].toInt() and 255) == 1) {
                            val model = ((data[3].toInt() and 255) shl 8) or
                                (data[4].toInt() and 255)
                            if (model == 0x3020 || model == 0x3620) {
                                proximity5++
                                if (model == 0x3020) proximity5wireless++
                                else proximity5standard++
                            }
                        }
                    } else {
                        other07++
                        if (data.size >= 3 && subtype == 0x11 &&
                            (data[2].toInt() and 255) == 0x06) casePro3++
                    }
                } else if (kind == 0x12) {
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
                } else if (kind == 0x10) {
                    nearInfo10++
                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            if (done.compareAndSet(false, true)) {
                stopScanner()
                dialog(a, "검색 실패", "Android BLE 오류=" + errorCode +
                    "\n기존 앱이나 페어링 변경은 하지 않았습니다.")
                if (current === this) current = null
            }
        }
        private fun stopScanner() {
            try { scanner?.stopScan(this) } catch (_: Exception) {}
            scanner = null
        }
        private fun snapshot(): Snapshot = synchronized(lock) {
            Snapshot(all, apple, proximity07, proximity5, proximity5wireless,
                proximity5standard, other07, findmy19, findmy02, casePro3,
                nearInfo10, generic12, candidates.values.toList())
        }
        private fun showReport() {
            if (done.get()) return
            stopScanner()
            val s = snapshot()
            if (a.isFinishing || a.isDestroyed) { close(); return }
            val report = StringBuilder()
            report.append("전체 BLE 콜백=").append(s.all).append('\n')
            report.append("Apple 제조사 콜백=").append(s.apple).append("\n\n")
            report.append("AirPods 주변 광고 07:19=").append(s.proximity07).append('\n')
            report.append("  ↳ AirPods 5 모델 단서=").append(s.proximity5).append('\n')
            report.append("  ↳ 0x3020 무선충전 모델=").append(s.proximity5wireless).append('\n')
            report.append("  ↳ 0x3620 일반 모델=").append(s.proximity5standard).append('\n')
            report.append("Apple 07 기타=").append(s.other07).append('\n')
            report.append("Pro3 케이스형 07:11:06=").append(s.casePro3).append("\n\n")
            report.append("Find My 주변형 12:02=").append(s.findmy02).append('\n')
            report.append("Find My 분리형 12:19 연결가능=").append(s.findmy19).append('\n')
            report.append("분리형 서로 다른 후보=").append(s.candidates.size).append('\n')
            report.append("다른 Find My 12형=").append(s.generic12).append('\n')
            report.append("Apple Nearby 10형=").append(s.nearInfo10).append("\n\n")
            report.append("07:19 모델 단서 ≠ 본인 케이스 인증\n")
            report.append("12:19 후보 ≠ 본인 에어팟 인증\n")
            report.append("케이스 소리=미구현, GATT 쓰기=0\n")
            report.append("검색을 자동 반복하지 않습니다.")
            val builder = AlertDialog.Builder(a).setTitle("12초 BLE 신호별 결과")
                .setMessage(report.toString())
                .setNegativeButton("닫기") { _, _ -> close() }
                .setOnCancelListener { close() }
            if (s.candidates.isNotEmpty()) {
                builder.setPositiveButton("12:19 후보 선택") { _, _ ->
                    presentCandidates(s.candidates)
                }
            } else {
                builder.setPositiveButton("확인") { _, _ -> close() }
            }
            builder.show()
        }
        private fun presentCandidates(items: List<Candidate>) {
            if (done.get() || a.isFinishing || a.isDestroyed) { close(); return }
            val rows = items.mapIndexed { i, candidate ->
                (i + 1).toString() + ": 분리 Find My (신원 미확인) " +
                    candidate.rssi.toString() + " dBm"
            }.toTypedArray()
            // Android AlertDialog setMessage + setItems are mutually exclusive.
            AlertDialog.Builder(a).setTitle("본인 소유를 확인할 수 있을 때만 선택")
                .setItems(rows) { _, index ->
                    AlertDialog.Builder(a).setTitle("신원 확인되지 않은 12:19 후보")
                        .setMessage("이 신호는 주변 다른 사람의 Find My 추적기일 수도 있습니다. " +
                            "본인이 소유한 장치임을 확실히 확인할 수 있을 때만 읽기 전용 조회를 진행하세요." +
                            "\n소리 명령·페어링·설정 변경은 하지 않습니다.")
                        .setNegativeButton("취소") { _, _ -> close() }
                        .setPositiveButton("읽기 전용 서비스 조회") { _, _ ->
                            connectReadOnly(items[index].device)
                        }.show()
                }
                .setNegativeButton("취소") { _, _ -> close() }
                .setOnCancelListener { close() }
                .show()
        }

        private fun connectReadOnly(device: BluetoothDevice) {
            if (done.get()) return
            try {
                gatt = device.connectGatt(a.applicationContext, false,
                    object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
                            if (done.get()) return
                            if (status == BluetoothGatt.GATT_SUCCESS &&
                                state == BluetoothProfile.STATE_CONNECTED) {
                                if (!g.discoverServices())
                                    finish("GATT 검색 거부", "서비스 검색 시작 거부")
                            } else if (state == BluetoothProfile.STATE_DISCONNECTED ||
                                status != BluetoothGatt.GATT_SUCCESS) {
                                finish("GATT 연결 실패", "Bluetooth GATT 상태=" + status)
                            }
                        }
                        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                            if (done.get()) return
                            if (status != BluetoothGatt.GATT_SUCCESS) {
                                finish("GATT 조회 실패", "상태=" + status); return
                            }
                            main.post {
                                if (!done.get()) {
                                    identityProbe = CaseIdentityReadV137(g) { body ->
                                        finish("Find My 후보 프로파일 · 미인증", body)
                                    }
                                    identityProbe?.start()
                                }
                            }
                        }
                        @Suppress("DEPRECATION")
                        override fun onCharacteristicRead(
                            g: BluetoothGatt,
                            ch: android.bluetooth.BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            if (android.os.Build.VERSION.SDK_INT < 33 && !done.get())
                                identityProbe?.onRead(ch, ch.value ?: byteArrayOf(), status)
                        }
                        override fun onCharacteristicRead(
                            g: BluetoothGatt,
                            ch: android.bluetooth.BluetoothGattCharacteristic,
                            value: ByteArray,
                            status: Int
                        ) {
                            if (!done.get()) identityProbe?.onRead(ch, value, status)
                        }
                    }, BluetoothDevice.TRANSPORT_LE)
                main.postDelayed({
                    if (!done.get())
                        finish("GATT 조회 시간초과", "20초 안에 서비스 확인이 끝나지 않았습니다.")
                }, 20000L)
            } catch (e: Exception) {
                finish("GATT 조회 오류", e.javaClass.simpleName)
            }
        }

        private fun finish(title: String, body: String) {
            if (!done.compareAndSet(false, true)) return
            stopScanner()
            try { gatt?.disconnect() } catch (_: Exception) {}
            try { gatt?.close() } catch (_: Exception) {}
            gatt = null
            if (current === this) current = null
            dialog(a, title, body)
        }
        fun close() {
            if (!done.compareAndSet(false, true)) return
            stopScanner()
            try { gatt?.disconnect() } catch (_: Exception) {}
            try { gatt?.close() } catch (_: Exception) {}
            gatt = null
            if (current === this) current = null
        }
    }
}
