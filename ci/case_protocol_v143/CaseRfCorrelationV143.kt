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
import java.util.LinkedHashMap

/**
 * User-controlled A/B radio signal observation only, with no GATT, bond
 * changes, signal ID exports, credentials or remote ringing assumptions.
 */
object CaseRfCorrelationV143 {
    private val main = Handler(Looper.getMainLooper())
    private var active: Trial? = null

    fun open(a: Activity) {
        if (a.isFinishing || a.isDestroyed) return
        val missing = listOf(Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
            .filter { a.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            AlertDialog.Builder(a).setTitle("BLE 권한 확인")
                .setMessage("Bluetooth와 위치 권한이 필요합니다. 좌표나 MAC 주소는 저장하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("권한 요청") { _, _ ->
                    a.requestPermissions(missing.toTypedArray(), 143)
                }.show()
            return
        }
        AlertDialog.Builder(a).setTitle("내 A3529 케이스 무선 신호 A/B")
            .setMessage("새로 추정한 소리 패킷을 보내는 실험이 아닙니다.\n\n" +
                "① 이어버드 모두 수납, 닫힌 케이스를 휴대폰 바로 옆에 두고 8초 측정.\n" +
                "② 안내 후 케이스만 멀리 옮기거나 금속 용기로 차폐한 상태에서 8초 측정.\n" +
                "기기 신호 상관관계만 관찰하며 소유자 인증/소리 재생은 불가능합니다.\n" +
                "BLE 기기 연결, 소리 명령, 페어링 변경 모두 없음.")
            .setNegativeButton("취소", null)
            .setPositiveButton("첫 구간 시작") { _, _ ->
                active?.close()
                active = Trial(a).also { it.measure(true) }
            }.show()
    }

    private class Radio(val kind: String) {
        var count = 0
        var sum = 0
        var peak = -127
        fun observe(rssi: Int) { count++; sum += rssi; if (rssi > peak) peak = rssi }
        fun avg() = if (count == 0) -127 else sum / count
    }
    private class Trial(private val a: Activity) {
        private var finished = false
        private var scanner: BluetoothLeScanner? = null
        private var cb: ScanCallback? = null
        private var timeout: Runnable? = null
        private val nearby = LinkedHashMap<String, Radio>()
        private val distant = LinkedHashMap<String, Radio>()

        @SuppressLint("MissingPermission")
        fun measure(first: Boolean) {
            if (finished || a.isFinishing || a.isDestroyed) { close(); return }
            val bucket = if (first) nearby else distant
            try {
                val adapter = (a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                scanner = adapter.bluetoothLeScanner
                val s = scanner ?: run { error("스캐너 없음"); return }
                val callback = object : ScanCallback() {
                    override fun onScanResult(t: Int, result: ScanResult) { add(bucket, result) }
                    override fun onBatchScanResults(results: MutableList<ScanResult>) {
                        results.forEach { add(bucket, it) }
                    }
                    override fun onScanFailed(code: Int) {
                        main.post { error("BLE 오류 $code") }
                    }
                }
                cb = callback
                s.startScan(null, ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0L).build(), callback)
                val runnable = Runnable {
                    stop()
                    if (!finished) {
                        if (first) offerSecond() else report()
                    }
                }
                timeout = runnable
                main.postDelayed(runnable, 8000L)
            } catch (ex: Exception) {
                error("검색 오류: " + ex.javaClass.simpleName)
            }
        }
        private fun add(bucket: LinkedHashMap<String, Radio>, r: ScanResult) {
            if (finished) return
            val d = r.scanRecord?.getManufacturerSpecificData(0x004c) ?: return
            if (d.size < 2) return
            val k = d[0].toInt() and 255
            val sub = d[1].toInt() and 255
            val kind = when {
                k == 0x12 && sub == 0x02 -> "12:02 Find My 주변"
                k == 0x12 && sub == 0x19 -> "12:19 Find My 분리"
                k == 0x07 && sub == 0x19 -> "07:19 AirPods 근접"
                k == 0x07 -> "07 Apple 기타"
                else -> return
            }
            // Radio address is transient and NEVER rendered, exported or saved.
            synchronized(bucket) {
                val key = r.device.address + "|" + kind
                val prev = bucket[key]
                if (prev != null) prev.observe(r.rssi)
                else if (bucket.size < 60) bucket[key] = Radio(kind).also { it.observe(r.rssi) }
            }
        }
        private fun offerSecond() {
            if (finished) return
            AlertDialog.Builder(a).setTitle("가까이 8초 측정 완료")
                .setMessage("이제 케이스만 휴대폰에서 4~5m 이상 옮기거나 " +
                    "케이스를 충전하지 않은 상태로 닫힌 금속 용기 안에 두세요. " +
                    "이후 두 번째 구간을 한 번 측정합니다.\n\n" +
                    "다른 Apple 기기는 이동하지 마세요.")
                .setNegativeButton("중단") { _, _ -> close() }
                .setOnCancelListener { close() }
                .setPositiveButton("멀리 놓았음 · 비교 시작") { _, _ -> measure(false) }
                .show()
        }
        private fun report() {
            if (finished) return
            val near = synchronized(nearby) { LinkedHashMap(nearby) }
            val far = synchronized(distant) { LinkedHashMap(distant) }
            val rows = near.entries
                .filter { it.value.count >= 2 }
                .map { entry ->
                    val opposite = far[entry.key]
                    val delta = if (opposite != null) entry.value.avg() - opposite.avg() else 0
                    Triple(entry.value, opposite, delta)
                }
                .sortedWith(compareByDescending<Triple<Radio, Radio?, Int>> {
                    if (it.second == null) 1 else 0
                }.thenByDescending { it.third }.thenByDescending { it.first.peak })
            val out = StringBuilder("가까운 Apple 후보=").append(near.size)
                .append("\n멀리/차폐 Apple 후보=").append(far.size).append("\n\n")
            if (rows.isEmpty()) out.append("비교 가능한 Apple 신호 없음.\n")
            rows.take(7).forEachIndexed { index, row ->
                out.append(index + 1).append(". ").append(row.first.kind)
                    .append(" 근접 평균 ").append(row.first.avg()).append(" dBm")
                if (row.second == null) out.append(" → 차폐 후 관찰 안 됨")
                else out.append(" → ").append(row.second!!.avg()).append(" dBm")
                    .append(" (변화 ").append(row.third).append(" dB)")
                out.append("\n")
            }
            out.append("\n전파 반사, 주소 변경, 다른 기기 때문에")
                .append(" 이 신호가 본인 케이스라는 보장은 없습니다.")
                .append("\n소유자 인증=미확인 · GATT 쓰기=0")
                .append("\n실제 케이스 소리=미구현")
            close()
            if (!a.isFinishing && !a.isDestroyed) {
                AlertDialog.Builder(a).setTitle("A3529 무선 신호 A/B 결과")
                    .setMessage(out.toString()).setPositiveButton("확인", null).show()
            }
        }
        private fun error(message: String) {
            close()
            if (!a.isFinishing && !a.isDestroyed)
                AlertDialog.Builder(a).setTitle("시험 중단").setMessage(message)
                    .setPositiveButton("확인", null).show()
        }
        fun close() { finished = true; stop(); nearby.clear(); distant.clear(); if (active === this) active = null }
        @SuppressLint("MissingPermission")
        private fun stop() {
            timeout?.let { main.removeCallbacks(it) }
            timeout = null
            val c = cb
            cb = null
            if (c != null) try { scanner?.stopScan(c) } catch (_: Exception) {}
            scanner = null
        }
    }
}
