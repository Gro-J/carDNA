package com.example.cardna.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 실제 차량 신호 없이도 핵심 루프를 시연할 수 있도록 와이퍼/주차 브레이크 이벤트를
 * 시간 가속(1초 = 1시간)으로 생성하는 시뮬레이션 소스.
 *
 * [UsageEngine]이 [hourPerRealSecond] 값을 이용해 실제 경과 시간을 "사용 시간(시간 단위)"으로
 * 환산하므로, 이 소스가 살아있는 동안 흐른 실제 1초는 사용량 계산상 1시간으로 취급된다.
 */
class SimulatedVehicleSource : VehicleDataSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _wiperLevel = MutableStateFlow(WiperLevel.OFF)
    override val wiperLevel: Flow<WiperLevel> = _wiperLevel.asStateFlow()

    private val _parkingBrakeEngaged = MutableStateFlow(false)
    override val parkingBrakeEngaged: Flow<Boolean> = _parkingBrakeEngaged.asStateFlow()

    /** 1초 = 1시간 가속. [UsageEngine]이 사용량을 환산할 때 참조한다. */
    override val hourPerRealSecond: Double = 1.0

    init {
        scope.launch { runWiperCycle() }
        scope.launch { runParkingBrakeCycle() }
    }

    private suspend fun runWiperCycle() {
        // OFF 비중을 높게 두어 "비가 올 때만 와이퍼가 돈다"는 상황을 단순 모사한다.
        val candidates = listOf(
            WiperLevel.OFF, WiperLevel.OFF, WiperLevel.OFF,
            WiperLevel.LOW, WiperLevel.LOW,
            WiperLevel.HIGH
        )
        while (true) {
            _wiperLevel.value = candidates.random()
            val simulatedHoursHeld = Random.nextLong(2, 7) // 이 상태를 2~6 "시간" 유지
            delay(simulatedHoursHeld * 1000L)
        }
    }

    private suspend fun runParkingBrakeCycle() {
        while (true) {
            _parkingBrakeEngaged.value = false
            delay(Random.nextLong(4_000, 9_000)) // 주행 구간
            _parkingBrakeEngaged.value = true
            delay(Random.nextLong(3_000, 6_000)) // 주차 구간
        }
    }

    override fun release() {
        scope.cancel()
    }
}
