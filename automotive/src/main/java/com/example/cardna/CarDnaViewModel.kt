package com.example.cardna

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardna.data.PleosVehicleSource
import com.example.cardna.data.SimulatedVehicleSource
import com.example.cardna.data.VehicleDataSource
import com.example.cardna.data.VehicleSourceType
import com.example.cardna.tts.AndroidVoiceAnnouncer
import com.example.cardna.tts.VoiceAnnouncer
import com.example.cardna.usage.ReplacementStatus
import com.example.cardna.usage.UsageEngine
import com.example.cardna.usage.UsageUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CarDnaUiState(
    val sourceType: VehicleSourceType = VehicleSourceType.SIMULATED,
    val usage: UsageUiState = UsageUiState(0.0, ReplacementStatus.NORMAL)
) {
    val isSimulated: Boolean get() = sourceType == VehicleSourceType.SIMULATED
}

class CarDnaViewModel(application: Application) : AndroidViewModel(application) {

    private val usageEngine = UsageEngine(application)
    private val voiceAnnouncer: VoiceAnnouncer = AndroidVoiceAnnouncer(application)

    private var vehicleSource: VehicleDataSource? = null
    private var collectJob: Job? = null
    private var lastParkingBrakeEngaged = false

    private val _uiState = MutableStateFlow(
        CarDnaUiState(sourceType = VehicleSourceType.SIMULATED, usage = usageEngine.state.value)
    )
    val uiState: StateFlow<CarDnaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            usageEngine.state.collect { usage ->
                _uiState.value = _uiState.value.copy(usage = usage)
            }
        }
        switchSource(VehicleSourceType.SIMULATED)
    }

    fun switchSource(type: VehicleSourceType) {
        if (_uiState.value.sourceType == type && vehicleSource != null) return

        vehicleSource?.release()
        collectJob?.cancel()
        lastParkingBrakeEngaged = false

        val source: VehicleDataSource = when (type) {
            VehicleSourceType.SIMULATED -> SimulatedVehicleSource()
            VehicleSourceType.PLEOS_SDK -> PleosVehicleSource(getApplication())
        }
        vehicleSource = source
        usageEngine.onSourceChanged(source.hourPerRealSecond)
        _uiState.value = _uiState.value.copy(sourceType = type)

        collectJob = viewModelScope.launch {
            launch {
                source.wiperLevel.collect { level ->
                    usageEngine.onWiperLevelChanged(level)
                }
            }
            launch {
                source.parkingBrakeEngaged.collect { engaged ->
                    val justEngaged = engaged && !lastParkingBrakeEngaged
                    lastParkingBrakeEngaged = engaged
                    if (justEngaged) {
                        announceIfNeeded()
                    }
                }
            }
        }
    }

    private fun announceIfNeeded() {
        val usage = usageEngine.state.value
        val percent = usage.usagePercent.toInt()
        val message = when (usage.status) {
            ReplacementStatus.NORMAL -> return
            ReplacementStatus.NEXT_SERVICE ->
                "와이퍼 블레이드 사용률이 ${percent}%예요. 다음 정비 때 함께 교체하시길 권해요."
            ReplacementStatus.CHECK_NOW ->
                "와이퍼 블레이드 사용률이 ${percent}%예요. 지금 바로 확인해 보세요."
        }
        voiceAnnouncer.announce(message)
    }

    fun onReplacementConfirmed() {
        usageEngine.markReplaced()
    }

    override fun onCleared() {
        collectJob?.cancel()
        vehicleSource?.release()
        voiceAnnouncer.release()
        super.onCleared()
    }
}
