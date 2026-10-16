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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * V134 LAB. Read-only Apple offline-finding / case GATT preflight.
 * This does not ring a CASE, change bonds, configure settings or send any
 * GATT characteristic writes. It preserves the frozen original AirPods engine.
 */
object CasePathV134 {
    private val handler = Handler(Looper.getMainLooper())
    private var active: Session? = null
    fun open(a: Activity) {
        if (a.isDestroyed || a.isFinishing) return
        val required = listOf(Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = required.filter {
            a.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            AlertDialog.Builder(a).setTitle("블루투스 검색 권한 필요")
                .setMessage("권한 승인 후 다시 열어 주세요. 케이스의 기존 페어링은 변경하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("권한 요청") { _, _ -> a.requestPermissions(missing.toTypedArray(), 134) }
                .show()
            return
        }
        AlertDialog.Builder(a).setTitle("A3530 독립 소리 경로 확인 · LAB")
            .setMessage("AirGuard 공개 구현의 비소유자 Find My / DULT 오디오 서비스가 실제 내 기기에 보이는지 확인합니다.\n\n" +
                "Apple 12 19 분리 추적기 광고만 8초 조사합니다. 과거 실패한 Pro3 07 11 06 분석은 반복하지 않습니다.\n\n" +
                "본인 기기라고 판단할 수 있는 후보만 직접 선택하면 GATT 서비스 식별만 합니다. " +
                "케이스 신원을 자동으로 판별하거나, 소리 명령을 보내지 않습니다.\n\n" +
                "현재 A3530의 실제 소리 재생은 아직 검증되지 않았습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("한 번 확인") { _, _ ->
                active?.close()
                active = Session(a).also { it.start() }
            }.show()
    }

    private fun show(a: Activity, title: String, body: String) {
        handler.post {
            if (!a.isFinishing && !a.isDestroyed)
                AlertDialog.Builder(a).setTitle(title).setMessage(body)
                    .setPositiveButton("확인", null).show()
        }
    }

    @SuppressLint("MissingPermission")
    private class Session(private val a: Activity) : ScanCallback() {
        private val done = AtomicBoolean(false)
        private var scanner: android.bluetooth.le.BluetoothLeScanner? = null
        private var gatt: BluetoothGatt? = null
        private val found = LinkedHashMap<String, Pair<BluetoothDevice, Int>>()
        private var total = 0

        fun start() {
            try {
                scanner = (a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                    .adapter.bluetoothLeScanner
                if (scanner == null) {
                    finish("검색 장치 없음", "BLE 스캐너를 사용할 수 없습니다."); return
                }
                scanner?.startScan(null, ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), this)
                handler.postDelayed({ options() }, 8000)
            } catch (e: Exception) {
                finish("검색 오류", e.javaClass.simpleName)
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (done.get()) return
            total++
            val manufacturer = result.scanRecord?.getManufacturerSpecificData(0x004c) ?: return
            if (manufacturer.size < 3 || (manufacturer[0].toInt() and 255) != 0x12 ||
                (manufacturer[1].toInt() and 255) != 0x19 || !result.isConnectable) return
            if (found.size >= 12 && !found.containsKey(result.device.address)) return
            found[result.device.address] = Pair(result.device, result.rssi)
        }
        override fun onScanFailed(errorCode: Int) {
            finish("검색 실패", "Android BLE 오류=$errorCode")
        }
        private fun stopScanner() {
            try { scanner?.stopScan(this) } catch (_: Exception) {}
            scanner = null
        }

        private fun options() {
            if (done.get()) return
            stopScanner()
            val items = found.values.toList()
            if (items.isEmpty()) {
                finish("분리 추적기 후보 없음", "전체 BLE=$total\nApple 12 19 연결 가능 후보=0\n" +
                    "A3530 신원 및 케이스 소리 지원은 미확인입니다. 0건이 미지원의 증거는 아닙니다.")
                return
            }
            if (a.isFinishing || a.isDestroyed) { close(); return }
            val labels = items.mapIndexed { i, v ->
                "${i + 1}: Apple Find My 후보 ...${v.first.address.takeLast(5)} / ${v.second} dBm"
            }.toTypedArray()
            AlertDialog.Builder(a).setTitle("후보 ${items.size}개 · 케이스 신원 미확인")
                .setMessage("사용자 소유가 확실한 대상만 선택하세요. 다른 사람 기기는 선택하지 마세요.")
                .setItems(labels) { _, index ->
                    val device = items[index].first
                    AlertDialog.Builder(a).setTitle("읽기 전용 연결")
                        .setMessage("직접 확인한 내 장치에서만 GATT 서비스 종류를 조회합니다.\n" +
                            "소리 명령, 페어링 요청, 설정 변경은 없습니다.")
                        .setNegativeButton("취소") { _, _ -> close() }
                        .setPositiveButton("내 기기 서비스 확인") { _, _ -> connect(device) }
                        .setOnCancelListener { close() }.show()
                }
                .setNegativeButton("취소") { _, _ -> close() }
                .setOnCancelListener { close() }.show()
        }

        private fun connect(device: BluetoothDevice) {
            if (done.get()) return
            try {
                gatt = device.connectGatt(a.applicationContext, false,
                    object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
                            if (done.get()) return
                            if (status == BluetoothGatt.GATT_SUCCESS &&
                                state == BluetoothProfile.STATE_CONNECTED) {
                                if (!g.discoverServices())
                                    finish("조회 실패", "서비스 검색 시작 거절 / 명령 0")
                            } else if (state == BluetoothProfile.STATE_DISCONNECTED ||
                                status != BluetoothGatt.GATT_SUCCESS) {
                                finish("연결 실패", "GATT 상태=$status / 명령 0")
                            }
                        }
                        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                            if (done.get()) return
                            if (status != BluetoothGatt.GATT_SUCCESS) {
                                finish("조회 실패", "GATT 상태=$status / 명령 0"); return
                            }
                            val fd44 = g.services.any { s ->
                                s.uuid.toString().contains("fd44", ignoreCase = true) &&
                                s.characteristics.any { c ->
                                    c.uuid.toString().startsWith("4f860003", ignoreCase = true)
                                }
                            }
                            val dult = g.services.any { s ->
                                s.uuid.toString().startsWith("15190001", ignoreCase = true) &&
                                s.characteristics.any { c ->
                                    c.uuid.toString().startsWith("8e0c0001", ignoreCase = true)
                                }
                            }
                            finish("비소유자 소리 서비스 조회 결과",
                                "Find My FD44 소리 특성=$fd44\nDULT 소리 특성=$dult\n" +
                                "GATT 서비스 개수=${g.services.size}\n\n" +
                                "케이스 A3530 신원=미검증\n" +
                                "소리 실행/소유자 인증=미검증\n" +
                                "쓰기 명령=0, 원본 앱 기능 변경=0")
                        }
                    }, BluetoothDevice.TRANSPORT_LE)
                handler.postDelayed({
                    if (!done.get()) finish("시간 초과", "10초 내 서비스 조회를 완료하지 못했습니다.")
                }, 10000)
            } catch (e: Exception) { finish("조회 오류", e.javaClass.simpleName) }
        }

        private fun finish(title: String, detail: String) {
            if (!done.compareAndSet(false, true)) return
            stopScanner()
            try { gatt?.disconnect() } catch (_: Exception) {}
            try { gatt?.close() } catch (_: Exception) {}
            gatt = null
            if (active === this) active = null
            show(a, title, detail)
        }
        fun close() {
            if (!done.compareAndSet(false, true)) return
            stopScanner()
            try { gatt?.disconnect() } catch (_: Exception) {}
            try { gatt?.close() } catch (_: Exception) {}
            gatt = null
            if (active === this) active = null
        }
    }
}
