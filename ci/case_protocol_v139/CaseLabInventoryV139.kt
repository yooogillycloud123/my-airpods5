package me.kavishdevar.librepods

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * LAB management is explicit and local. Never silently force-stop, uninstall,
 * clear data, or re-pair anyone's Bluetooth devices. App info is read-only until
 * the user elects an Android Settings action.
 */
object CaseLabInventoryV139 {
    private data class Item(val title: String, val pkg: String, val info: String)
    private val items = listOf(
        Item("원본 개인 앱 · 보존", "com.myairpods5.personal",
            "사용자 일부 정상 기능 확인 기준 앱입니다. 삭제하거나 데이터 초기화하지 마세요."),
        Item("초기 LAB v1.0.34 · 보존", "com.myairpods5.personal.lab",
            "과거 LAB 내부 키·진단 기록이 존재할 수 있어 지금은 삭제하지 않습니다. 백그라운드 동작이 의심되면 강제 종료만 하세요."),
        Item("이전 후보 LAB v1.0.36 · 사용 종료", "com.myairpods5.personal.caseprobe",
            "후보 선택/GATT 수동 조사 증거는 기록됐습니다. 다음 실험 동안 강제 종료 가능합니다. 삭제 시 해당 LAB 데이터는 없어집니다."),
        Item("이전 신원 LAB v1.0.37 · 사용 종료", "com.myairpods5.personal.identityprobe",
            "이전 LAB은 중복 스캐너가 있을 수 있으므로 실험 동안 강제 종료 가능합니다. 삭제 시 해당 LAB 데이터는 없어집니다."),
        Item("현재 단일 실험 LAB v1.0.39 · 유지", "com.myairpods5.personal.radiolab",
            "현재 앱입니다. 실험용 내부 주기 BLE 검색은 비활성화했고 사용자가 실행한 수동 조사만 동작합니다.")
    )

    fun open(a: Activity) {
        if (a.isDestroyed || a.isFinishing) return
        val labels = items.map { it.title }.toTypedArray()
        AlertDialog.Builder(a).setTitle("설치 앱 구분 · 삭제 없이 정리")
            // Don't combine setMessage and setItems: on Samsung this hides choices.
            .setItems(labels) { _, index ->
                val item = items[index]
                AlertDialog.Builder(a).setTitle(item.title)
                    .setMessage(item.info + "\n\n패키지: " + item.pkg +
                        "\n\n[앱 정보 열기]를 누르면 Android 시스템 설정으로 이동합니다. " +
                        "앱이 자동으로 다른 앱을 종료하거나 삭제하지 않습니다.")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("앱 정보 열기") { _, _ ->
                        try {
                            a.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", item.pkg, null)))
                        } catch (_: Exception) {
                            AlertDialog.Builder(a).setTitle("앱 정보 이동 실패")
                                .setMessage("설정 > 애플리케이션에서 해당 LAB 앱을 직접 선택하세요.")
                                .setPositiveButton("확인", null).show()
                        }
                    }.show()
            }
            .setNegativeButton("닫기", null)
            .show()
    }
}
