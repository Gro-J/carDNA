package com.example.cardna.usage

import android.content.Context
import android.os.SystemClock
import com.example.cardna.data.WiperLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReplacementStatus {
    NORMAL,       // 70% 미만
    NEXT_SERVICE, // 70~90%
    CHECK_NOW     // 90% 이상
}

data class UsageUiState(
    val usagePercent: Double,
    val status: ReplacementStatus
)

/**
 * 와이퍼 블레이드 사용률을 계산하고 SharedPreferences에 저장하는 엔진.
 *
 * 누적량 = Σ(와이퍼가 켜져 있던 시간 × 강도 가중치) — 저속 1배, 고속 2배.
 * 사용률(%) = 누적량 ÷ (기준량 × 보정계수 k) × 100.
 * "교체 완료" 시 k = 그 시점 누적량 ÷ 기준량으로 갱신하고 누적량을 0으로 초기화한다.
 */
class UsageEngine(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var accumulatedUsageHours: Double =
        prefs.getFloat(KEY_ACCUMULATED_HOURS, 0f).toDouble()
    private var calibrationFactorK: Double =
        prefs.getFloat(KEY_CALIBRATION_K, 1f).toDouble()

    private val _state = MutableStateFlow(computeState())
    val state: StateFlow<UsageUiState> = _state.asStateFlow()

    private var lastLevel: WiperLevel = WiperLevel.OFF
    private var lastChangeAtMillis: Long = SystemClock.elapsedRealtime()
    private var currentHourPerRealSecond: Double = 1.0 / 3600.0

    /** 데이터 소스를 전환할 때 호출한다. 전환 직전 구간을 먼저 정산한다. */
    fun onSourceChanged(hourPerRealSecond: Double) {
        flush()
        currentHourPerRealSecond = hourPerRealSecond
    }

    /** 와이퍼 상태가 바뀔 때마다 호출한다. 직전 상태가 유지된 시간을 정산한다. */
    fun onWiperLevelChanged(level: WiperLevel) {
        flush()
        lastLevel = level
    }

    private fun flush() {
        val now = SystemClock.elapsedRealtime()
        val elapsedRealSeconds = (now - lastChangeAtMillis) / 1000.0
        lastChangeAtMillis = now
        val weight = weightOf(lastLevel)
        if (weight <= 0.0 || elapsedRealSeconds <= 0.0) return

        val usageHours = elapsedRealSeconds * currentHourPerRealSecond * weight
        accumulatedUsageHours += usageHours
        persist()
        _state.value = computeState()
    }

    private fun weightOf(level: WiperLevel): Double = when (level) {
        WiperLevel.OFF -> 0.0
        WiperLevel.LOW -> 1.0
        WiperLevel.HIGH -> 2.0
    }

    /** "교체 완료" 버튼. */
    fun markReplaced() {
        flush()
        calibrationFactorK = if (BASELINE_USAGE_HOURS > 0.0) {
            accumulatedUsageHours / BASELINE_USAGE_HOURS
        } else {
            1.0
        }
        accumulatedUsageHours = 0.0
        persist()
        _state.value = computeState()
    }

    private fun persist() {
        prefs.edit()
            .putFloat(KEY_ACCUMULATED_HOURS, accumulatedUsageHours.toFloat())
            .putFloat(KEY_CALIBRATION_K, calibrationFactorK.toFloat())
            .apply()
    }

    private fun computeState(): UsageUiState {
        val denominator = BASELINE_USAGE_HOURS * calibrationFactorK
        val percent = if (denominator > 0.0) (accumulatedUsageHours / denominator) * 100.0 else 0.0
        val status = when {
            percent >= 90.0 -> ReplacementStatus.CHECK_NOW
            percent >= 70.0 -> ReplacementStatus.NEXT_SERVICE
            else -> ReplacementStatus.NORMAL
        }
        return UsageUiState(usagePercent = percent, status = status)
    }

    companion object {
        private const val PREFS_NAME = "cardna_usage_prefs"
        private const val KEY_ACCUMULATED_HOURS = "accumulated_usage_hours"
        private const val KEY_CALIBRATION_K = "calibration_factor_k"

        /**
         * 가정치: 와이퍼 블레이드가 "저속 환산"으로 약 90시간 사용되면 교체 시점에
         * 도달한다고 가정한다. 데모에서 핵심 루프(정상 → 다음 정비 때 함께 → 지금 확인)를
         * 몇 분 내에 관찰할 수 있도록 의도적으로 작게 잡은 가정치이며, 실제 블레이드
         * 수명 데이터가 아니다.
         */
        const val BASELINE_USAGE_HOURS = 90.0
    }
}
