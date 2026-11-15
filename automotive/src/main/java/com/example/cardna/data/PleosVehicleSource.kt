package com.example.cardna.data

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.control.WiperSwitch
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.util.Log
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

private const val TAG = "PleosVehicleSource"

/**
 * 실제 Pleos Vehicle SDK(ai.pleos.playground:Vehicle:2.3.2)를 사용하는 데이터 소스.
 *
 * 공식 샘플(pleos-sample-vehicle-app)의 VehicleClient 패턴을 따라 capability 확인 후
 * listener를 등록한다. 클래스/메서드명은 SDK aar(classes.jar)를 직접 디컴파일해 확인한
 * 실제 시그니처이며 추측으로 작성하지 않았다.
 *
 * 주의: 이 SDK에는 "주차 브레이크" API가 없다. Brake 도메인은 브레이크 '페달' 밟음 여부
 * (getCurrentBrakePedalStatus)만 제공하고, Safety 도메인은 브레이크 액 레벨만 제공한다.
 * 그래서 parkingBrakeEngaged는 이 소스에서도 실제 신호가 아니라 시뮬레이션으로 대체한다.
 * (화면에는 "시뮬레이션" 배지를 띄우지 않지만, 아래 주석과 README에 명시함)
 */
class PleosVehicleSource(context: Context) : VehicleDataSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val windshield = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private val vehicle = Vehicle(context.applicationContext)
    private var isInitialized = false

    private val _wiperLevel = MutableStateFlow(WiperLevel.OFF)
    override val wiperLevel: Flow<WiperLevel> = _wiperLevel.asStateFlow()

    // TODO(parking brake): Pleos Vehicle SDK 2.3.2에 주차 브레이크 API가 없어 대체할 수 없음.
    private val _parkingBrakeEngaged = MutableStateFlow(false)
    override val parkingBrakeEngaged: Flow<Boolean> = _parkingBrakeEngaged.asStateFlow()

    // 실제 차량 신호는 가속하지 않고 실제 시간 그대로 누적한다 (1초 = 1/3600 시간).
    override val hourPerRealSecond: Double = 1.0 / 3600.0

    private val wiperStateListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            onWiperStateChanged(state)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "Wiper state listener failed", e)
        }
    }

    init {
        try {
            vehicle.initialize()
            isInitialized = true
            vehicle.getWiper().checkWiperStateCapabilities(
                onComplete = { _, ranges ->
                    if (ranges.keys.any { it == windshield }) {
                        vehicle.getWiper().registerWiperState(windshield, wiperStateListener)
                    } else {
                        Log.w(TAG, "Front windshield wiper state capability not supported on this vehicle")
                    }
                },
                onFailed = { e -> Log.w(TAG, "Wiper capability check failed", e) }
            )
        } catch (e: Exception) {
            // Pleos Connect 차량 서비스에 바인딩할 수 없는 환경(예: 이 SDK가 실제로 연동되지 않은
            // 일반 AAOS 에뮬레이터)에서는 여기서 실패한다. MVP에서는 조용히 OFF 상태를 유지한다.
            Log.e(TAG, "Vehicle SDK initialization failed; no live wiper signal available", e)
        }
        // 주차 브레이크 신호가 없으므로, 데모 가능하도록 저빈도 사이클로 대체한다.
        scope.launch { runParkingBrakePlaceholderCycle() }
    }

    private fun onWiperStateChanged(state: WiperState?) {
        when (state) {
            WiperState.OFF, WiperState.SERVICE, WiperState.OTHER, null -> {
                _wiperLevel.value = WiperLevel.OFF
            }
            WiperState.ON -> {
                vehicle.getWiper().getCurrentWiperSwitch(
                    windshield,
                    onComplete = { switch -> _wiperLevel.value = mapSwitchToLevel(switch) },
                    onFailed = { e ->
                        Log.w(TAG, "getCurrentWiperSwitch failed; defaulting to LOW", e)
                        _wiperLevel.value = WiperLevel.LOW
                    }
                )
            }
        }
    }

    /**
     * WiperSwitch는 OFF/MIST/간헐(1~5단)/연속(1~5단)/AUTO/SERVICE로 세분화되어 있지만,
     * 이번 MVP는 저속/고속 2단만 쓴다. 간헐·미스트·자동은 저속(1배), 연속은 고속(2배)으로
     * 매핑한다 — 이 매핑은 "가정"이며 SDK 문서에 저속/고속 구분이 별도로 명시되어 있지 않다.
     */
    private fun mapSwitchToLevel(switch: WiperSwitch?): WiperLevel = when (switch) {
        null, WiperSwitch.OFF, WiperSwitch.SERVICE, WiperSwitch.OTHER -> WiperLevel.OFF
        WiperSwitch.MIST,
        WiperSwitch.INTERMITTENT_LEVEL_1,
        WiperSwitch.INTERMITTENT_LEVEL_2,
        WiperSwitch.INTERMITTENT_LEVEL_3,
        WiperSwitch.INTERMITTENT_LEVEL_4,
        WiperSwitch.INTERMITTENT_LEVEL_5,
        WiperSwitch.AUTO -> WiperLevel.LOW
        WiperSwitch.CONTINUOUS_LEVEL_1,
        WiperSwitch.CONTINUOUS_LEVEL_2,
        WiperSwitch.CONTINUOUS_LEVEL_3,
        WiperSwitch.CONTINUOUS_LEVEL_4,
        WiperSwitch.CONTINUOUS_LEVEL_5 -> WiperLevel.HIGH
    }

    private suspend fun runParkingBrakePlaceholderCycle() {
        while (true) {
            _parkingBrakeEngaged.value = false
            delay(Random.nextLong(4_000, 9_000))
            _parkingBrakeEngaged.value = true
            delay(Random.nextLong(3_000, 6_000))
        }
    }

    override fun release() {
        if (isInitialized) {
            try {
                vehicle.getWiper().unregisterWiperState(windshield, wiperStateListener)
                vehicle.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing Vehicle SDK", e)
            }
        }
        scope.cancel()
    }
}
