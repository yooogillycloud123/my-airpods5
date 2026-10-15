package me.kavishdevar.librepods

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.annotation.SuppressLint

/**
 * Read-only observation of Apple Find My advertisement formats.
 * No remote connections or sound commands. No device identifiers retained.
 */
object FindMyRadioAudit {
    private val main = Handler(Looper.getMainLooper())
    private var current: Session? = null

    fun open(activity: Activity) {
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            popup(activity, "검색 권한", "실험실 앱에 주변 기기 검색 권한을 허용해 주세요.")
            return
        }
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(activity)
                .setTitle("블루투스 신호 확인 권한")
                .setMessage("기종별 BLE 신호 누락을 피하기 위해 정확한 위치 권한이 필요합니다. 위치 좌표는 수집하지 않습니다. 권한 허용 후 메뉴를 다시 눌러 주세요.")
                .setNegativeButton("취소", null)
                .setPositiveButton("권한 요청") { _, _ ->
                    activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 115)
                }.show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Apple Find My 신호 조사")
            .setMessage("35초 동안 블루투스 광고를 관찰합니다. 실제 AirPods 케이스를 확인한 것은 아니며, 다른 Apple 기기 신호도 포함될 수 있습니다. 개별 주소와 광고 원문은 기록하지 않습니다. 어떠한 장치에도 연결하거나 명령을 보내지 않습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("35초 측정") { _, _ ->
                current?.close()
                val session = Session(activity)
                current = session
                session.start()
            }.show()
    }

    private fun popup(activity: Activity, title: String, message: String) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            AlertDialog.Builder(activity)
                .setTitle(title).setMessage(message)
                .setPositiveButton("확인", null).show()
        }
    }

    private class Session(private val activity: Activity) : ScanCallback() {
        private val handler = Handler(Looper.getMainLooper())
        private var scanner: android.bluetooth.le.BluetoothLeScanner? = null
        private var stopped = false
        private var all = 0
        private var apple = 0
        private var type07 = 0
        private var type12 = 0
        private var airpodsMask = 0
        private var fd44 = 0
        private var connectable12 = 0
        private val appleKinds = mutableMapOf<String, Int>()

        @SuppressLint("MissingPermission")
        fun start() {
            try {
                scanner = (activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                    .adapter.bluetoothLeScanner
                if (scanner == null) {
                    finish("Bluetooth LE 검색 서비스를 사용할 수 없습니다.")
                    return
                }
                scanner?.startScan(null, ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build(), this)
                handler.postDelayed({ finish(null) }, 35000)
            } catch (e: Exception) {
                finish("스캔 시작 오류: " + e.javaClass.simpleName)
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (stopped) return
            all += 1
            val record = result.scanRecord ?: return
            if (record.serviceUuids?.any {
                it.uuid.toString().startsWith("0000fd44-", ignoreCase = true)
            } == true) fd44 += 1
            val data = record.getManufacturerSpecificData(76) ?: return
            apple += 1
            if (data.isEmpty()) return
            val t = data[0].toInt() and 255
            val subtype = if (data.size > 1) data[1].toInt() and 255 else -1
            val label = "%02X:%02X size=%d".format(t, subtype, data.size)
            appleKinds[label] = (appleKinds[label] ?: 0) + 1
            if (t == 0x07) type07 += 1
            if (t == 0x12) {
                type12 += 1
                if (result.isConnectable) connectable12 += 1
                if (data.size > 2 && (data[2].toInt() and 0x18) == 0x18) airpodsMask += 1
            }
        }

        override fun onScanFailed(errorCode: Int) {
            finish("Android BLE scan failed: " + errorCode)
        }

        @SuppressLint("MissingPermission")
        fun close() {
            if (stopped) return
            stopped = true
            try { scanner?.stopScan(this) } catch (_: Exception) { }
            scanner = null
            if (current === this) current = null
        }

        private fun finish(error: String?) {
            if (stopped) return
            close()
            val top = appleKinds.entries
                .sortedByDescending { it.value }
                .take(12)
                .joinToString("; ") { it.key + "=" + it.value }
                .ifBlank { "없음" }
            val body = "전체 BLE 콜백: " + all +
                "\nApple 제조사 광고: " + apple +
                "\nApple 07형: " + type07 +
                "\nApple Find My 12형: " + type12 +
                "\nAirGuard AirPods 조건(12·??·18): " + airpodsMask +
                "\n12형 중 연결 가능 표시: " + connectable12 +
                "\nFD44 서비스 UUID 광고: " + fd44 +
                "\nApple 종류/길이 상위 12개: " + top +
                "\n실제 내 케이스 여부: 미확인" +
                "\n케이스 소리 재생: 시도하지 않음" +
                (if (error != null) "\n오류: " + error else "")
            main.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                AlertDialog.Builder(activity)
                    .setTitle("Find My 신호 조사 결과")
                    .setMessage(body)
                    .setNegativeButton("닫기", null)
                    .setPositiveButton("결과 복사") { _, _ ->
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Find My radio audit", body))
                    }.show()
            }
        }
    }
}
