package me.kavishdevar.librepods

import android.app.Activity
import android.app.AlertDialog

/**
 * v143 replaces no existing feature. The previous v141 read-only evidence
 * remains available; the RF A/B test is a separate user-selected experiment.
 */
object CaseEntryV143 {
    fun open(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("AirPods 5 A3529 케이스 연구 · v143")
            .setMessage(
                "기존 v141 기록은 유지합니다. 새 A/B 시험은 충전 케이스를 가까이 둔 상태와 " +
                "전파를 차폐한 상태를 한 번씩 비교해 주변 후보를 좁힙니다. " +
                "이는 무선 신호 상관관계이며 소유자 인증이나 소리 재생은 아닙니다."
            )
            .setNegativeButton("취소", null)
            .setNeutralButton("기존 v141 진단") { _, _ -> CaseRadioEvidenceV138.open(activity) }
            .setPositiveButton("케이스 RF A/B 확인") { _, _ -> CaseRfCorrelationV143.open(activity) }
            .show()
    }
}
