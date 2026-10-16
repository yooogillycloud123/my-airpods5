package me.kavishdevar.librepods

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Handler
import android.os.Looper
import java.util.LinkedHashMap

/**
 * v137: strictly READ-ONLY BLE candidate classification.
 * This is NOT AirPods5 case identification or Find My speaker control.
 * No serial number, Bluetooth address, raw value or identifier is displayed.
 */
internal class CaseIdentityReadV137(
    private val gatt: BluetoothGatt,
    private val complete: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private data class Field(val id: String, val serviceUuid: String, val characteristicUuid: String)
    private data class ReadTarget(val field: Field, val characteristic: BluetoothGattCharacteristic)
    private val fields = listOf(
        Field("제조사", "0000180a-0000-1000-8000-00805f9b34fb", "00002a29-0000-1000-8000-00805f9b34fb"),
        Field("모델", "0000180a-0000-1000-8000-00805f9b34fb", "00002a24-0000-1000-8000-00805f9b34fb"),
        Field("일반 이름", "00001800-0000-1000-8000-00805f9b34fb", "00002a00-0000-1000-8000-00805f9b34fb"),
        Field("Find My 장치 표기", "87290102-3c51-43b1-a1a9-11b9dc38478b", "6aa50003-6352-4d57-a7b4-003a416fbb0b")
    )
    private val results = LinkedHashMap<String, String>()
    private val queue = ArrayList<ReadTarget>()
    private var cursor = 0
    private var waiting: ReadTarget? = null
    private var stopped = false

    fun start() {
        if (stopped) return
        for (f in fields) {
            val svc = gatt.services.firstOrNull { it.uuid.toString().equals(f.serviceUuid, ignoreCase = true) }
            val ch = svc?.characteristics?.firstOrNull { it.uuid.toString().equals(f.characteristicUuid, ignoreCase = true) }
            when {
                svc == null -> results[f.id] = "서비스 없음"
                ch == null -> results[f.id] = "특성 없음"
                ch.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0 ->
                    results[f.id] = "읽기 불가(접근 제한)"
                else -> {
                    results[f.id] = "미수신"
                    queue.add(ReadTarget(f, ch))
                }
            }
        }
        main.postDelayed({ if (!stopped) end() }, 13500L)
        readNext()
    }

    @SuppressLint("MissingPermission")
    private fun readNext() {
        if (stopped) return
        if (cursor >= queue.size) { end(); return }
        val target = queue[cursor++]
        waiting = target
        val ok = try { gatt.readCharacteristic(target.characteristic) } catch (_: Exception) { false }
        if (!ok) {
            waiting = null
            results[target.field.id] = "읽기 시작 불가"
            main.post { readNext() }
            return
        }
        // A characteristic read may time out or be rejected by an unbonded peripheral.
        // Neither outcome authorises bonding, descriptor writes or a retry storm.
        main.postDelayed({
            if (!stopped && waiting === target) {
                waiting = null
                results[target.field.id] = "응답 없음"
                readNext()
            }
        }, 2600L)
    }

    fun onRead(characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
        // Move all state changes to the main thread, away from Android's Binder callback.
        main.post {
            if (stopped) return@post
            val target = waiting ?: return@post
            if (target.characteristic.uuid != characteristic.uuid) return@post
            waiting = null
            results[target.field.id] = if (status == BluetoothGatt.GATT_SUCCESS) {
                classify(target.field.id, value)
            } else {
                "접근 거부/오류($status)"
            }
            readNext()
        }
    }

    private fun classify(field: String, value: ByteArray): String {
        if (value.isEmpty()) return "값 없음"
        // Do not output arbitrary UTF-8/UTF-16 values: they can contain a personal device name,
        // serial number, BLE address or other identifier. Expose only coarse evidence classes.
        val raw = value.take(96).toByteArray().toString(Charsets.UTF_8)
            .filter { it.code in 32..126 }.trim().lowercase()
        if (raw.isEmpty()) return "값 수신(이진 데이터, 원문 비공개)"
        val airpods = raw.contains("airpods") || raw.contains("air pods")
        return when (field) {
            "제조사" -> when {
                raw.contains("apple") -> "Apple 표기(미인증)"
                else -> "Apple 외 제조사/미확인(원문 비공개)"
            }
            "모델" -> when {
                airpods && (raw.contains(" 5") || raw.contains("airpods5")) -> "AirPods 5 표기(미인증)"
                airpods -> "AirPods 계열 표기(미인증)"
                raw.contains("a3529") -> "A3529 표기(미인증)"
                raw.contains("a3530") -> "A3530 표기(미인증)"
                else -> "다른/불명 모델 표기(원문 비공개)"
            }
            "일반 이름" -> when {
                airpods -> "AirPods 이름 표기(미인증)"
                raw == "left" || raw == "right" -> "이어버드 쪽 표기(미인증)"
                else -> "기타 이름(원문 비공개)"
            }
            "Find My 장치 표기" -> when {
                raw == "left" || raw == "right" -> "이어버드 쪽 표기(미인증)"
                airpods -> "AirPods 관련 표기(미인증)"
                else -> "값 수신(원문 비공개)"
            }
            else -> "값 수신(원문 비공개)"
        }
    }

    private fun end() {
        if (stopped) return
        stopped = true
        val fd44Service = gatt.services.firstOrNull { it.uuid.toString().contains("fd44", ignoreCase = true) }
        val nonOwner = fd44Service?.characteristics?.any {
            it.uuid.toString().startsWith("4f860003", ignoreCase = true)
        } == true
        val ownerPoint = fd44Service?.characteristics?.any {
            it.uuid.toString().startsWith("4f860002", ignoreCase = true)
        } == true
        val dult = gatt.services.any { service ->
            service.uuid.toString().startsWith("15190001", ignoreCase = true) &&
                service.characteristics.any { it.uuid.toString().startsWith("8e0c0001", ignoreCase = true) }
        }
        val b = StringBuilder()
        b.append("Find My 후보 · 광고 세부종류 미확인(신원 인증 아님)\n")
        b.append("FD44 비소유자 특성=").append(nonOwner).append('\n')
        b.append("FD44 소유자 관련 특성 존재=").append(ownerPoint).append(" (인증 아님)\n")
        b.append("DULT 특성=").append(dult).append('\n')
        b.append("GATT 서비스=").append(gatt.services.size).append("개\n\n")
        for (f in fields) b.append(f.id).append(" = ").append(results[f.id] ?: "미수신").append('\n')
        b.append("\n본인 AirPods 5 케이스 신원=미검증\n")
        b.append("소유자 인증=미검증, 실제 소리=미구현\n")
        b.append("GATT 쓰기=0, 페어링 변경=0\n")
        b.append("기기명·주소·시리얼·원문은 공개하지 않음")
        complete(b.toString())
    }
}
