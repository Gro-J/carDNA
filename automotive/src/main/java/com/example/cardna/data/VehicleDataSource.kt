package com.example.cardna.data

import kotlinx.coroutines.flow.Flow

/** 와이퍼 작동 강도. OFF=정지, LOW=저속(간헐/미스트), HIGH=고속(연속). */
enum class WiperLevel {
    OFF,
    LOW,
    HIGH
}

/** 차량 데이터 소스 종류. 화면의 토글이 이 값을 전환한다. */
enum class VehicleSourceType {
    PLEOS_SDK,
    SIMULATED
}

/**
 * 와이퍼 상태와 주차 브레이크 체결 여부를 Flow로 제공하는 인터페이스.
 * [PleosVehicleSource](실제 Pleos Vehicle SDK)와 [SimulatedVehicleSource](시간 가속 시뮬레이션)
 * 두 구현체가 동일한 계약을 따른다.
 */
interface VehicleDataSource {
    val wiperLevel: Flow<WiperLevel>
    val parkingBrakeEngaged: Flow<Boolean>

    /**
     * 실제 경과 1초를 몇 "시간"의 사용량으로 환산할지. [UsageEngine]이 누적량을 계산할 때 사용한다.
     * 실제 SDK 소스는 1.0 / 3600.0(= 실제 시간 그대로), 시뮬레이션 소스는 시간 가속 배율을 반환한다.
     */
    val hourPerRealSecond: Double

    /** 리스너 해제 등 리소스 정리. 데이터 소스 전환/화면 종료 시 호출한다. */
    fun release()
}
