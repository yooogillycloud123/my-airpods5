package me.kavishdevar.librepods
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

/** One manually chosen candidate, no pairing changes; unverified public FD44 experiment. */
object CaseSoundTrialV140 {
  private val handler = Handler(Looper.getMainLooper())
  @SuppressLint("MissingPermission")
  fun run(activity: Activity, candidate: BluetoothDevice) {
    val target = UUID.fromString("4F860003-943B-49EF-BED4-2F730304427A")
    var conn: BluetoothGatt? = null
    var done = false
    var response = "no start"
    var stopQueued = false
    var soundChar: BluetoothGattCharacteristic? = null
    fun finish(reason: String) {
      if (done) return
      done = true
      handler.post {
        try { conn?.disconnect() } catch (_: Exception) {}
        try { conn?.close() } catch (_: Exception) {}
        if (!activity.isFinishing && !activity.isDestroyed)
          AlertDialog.Builder(activity).setTitle("A3529 실험 결과")
            .setMessage(reason + "\n" + response + "\nGATT ACK는 실제 케이스 소리를 뜻하지 않습니다.")
            .setPositiveButton("확인", null).show()
      }
    }
    try {
      conn = candidate.connectGatt(activity.applicationContext, false,
        object: BluetoothGattCallback() {
          override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
            if (done) return
            if (status == 0 && state == BluetoothProfile.STATE_CONNECTED) {
              if (!g.discoverServices()) finish("서비스 검색 시작 실패")
            } else finish("연결 실패 " + status)
          }
          override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (done) return
            if (status != 0) { finish("서비스 조회 실패"); return }
            val c = g.services.firstOrNull { it.uuid.toString().contains("fd44", true) }?.getCharacteristic(target)
            if (c == null) { finish("FD44 비소유자 소리 특성 없음"); return }
            val bytes = byteArrayOf(1,0,3)
            val queued = if (Build.VERSION.SDK_INT >= 33)
              g.writeCharacteristic(c, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == 0
            else {
              @Suppress("DEPRECATION") c.value = bytes
              @Suppress("DEPRECATION") g.writeCharacteristic(c)
            }
            soundChar = c
            response = if (queued) "start request queued" else "start request rejected"
            if (!queued) finish("명령 전송 불가")
          }
          override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (done) return
            if (stopQueued) {
              response += "; stop GATT status=" + status
              finish("시작 및 중지 1회 시험 종료")
              return
            }
            response = "start GATT status=" + status
            if (status != 0) { finish("시작 명령 GATT 응답 실패"); return }
            handler.postDelayed({
              if (!done && !stopQueued) {
                stopQueued = true
                val stop = byteArrayOf(1,1,3)
                val queued = try {
                  if (Build.VERSION.SDK_INT >= 33)
                    g.writeCharacteristic(c, stop, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == 0
                  else {
                    @Suppress("DEPRECATION") c.value = stop
                    @Suppress("DEPRECATION") g.writeCharacteristic(c)
                  }
                } catch (_: Exception) { false }
                if (!queued) finish("중지 명령 큐 전송 실패")
              }
            }, 2000L)
          }
        }, BluetoothDevice.TRANSPORT_LE)
      handler.postDelayed({ finish("시간 초과") }, 12000L)
    } catch (e: Exception) { finish("GATT 오류 " + e.javaClass.simpleName) }
  }
}
