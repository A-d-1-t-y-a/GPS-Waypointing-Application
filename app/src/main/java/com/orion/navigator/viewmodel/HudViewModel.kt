package com.orion.navigator.viewmodel

import androidx.lifecycle.ViewModel
import com.orion.navigator.service.GyroscopeMonitor
import kotlinx.coroutines.flow.Flow

class HudViewModel(
    gyro: GyroscopeMonitor
) : ViewModel() {
    val azimuth: Flow<Float> = gyro.streamAzimuth()
}
