package me.kavishdevar.librepods

import android.app.Activity
import android.app.AlertDialog

/**
 * Explicit evidence-based status for *AirPods 5 A3530 charging-case* speaker.
 * NO BLE scan, no network calls, no Find My/owner GATT writes, no audio.
 * No user re-test is requested until the case owner protocol is sourced.
 */
object CaseSoundStatusV125 {
    fun open(a: Activity) {
        if (a.isFinishing || a.isDestroyed) return
        AlertDialog.Builder(a)
            .setTitle("AirPods 5 케이스 찾기 · 현재 상태")
            .setMessage(
                "목표: A3530 무선충전 케이스 뚜껑을 닫고 양쪽 이어버드를 넣어도 " +
                "갤럭시에서 케이스 자체의 찾기 소리를 재생하는 것입니다.\n\n" +
                "케이스 스피커는 존재합니다. 하지만 공개 소스에서 " +
                "A3530의 소유자 인증 및 Find My 소리 재생·중지 명령은 " +
                "아직 검증되지 않았습니다.\n\n" +
                "기존 10초 광고 테스트는 실제 사용자 측정 4/33건 중 " +
                "Pro 3 전용 07 11 06 후보가 둘 다 0건이므로 중단했습니다. " +
                "추가 스캔/재페어링/앱 삭제를 요청하지 않습니다.\n\n" +
                "현재 앱은 인증되지 않은 기기에 임의 명령을 보내지 않고, " +
                "찾기 성공을 허위로 표시하지 않습니다.\n\n" +
                "케이스 소리 기능: 아직 미구현 / 실기 성공: 미확인"
            )
            .setPositiveButton("확인", null)
            .show()
    }
}
