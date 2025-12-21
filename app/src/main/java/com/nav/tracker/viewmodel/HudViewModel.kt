package com.nav.tracker.viewmodel

import androidx.lifecycle.ViewModel
import com.nav.tracker.service.GyroscopeMonitor
import kotlinx.coroutines.flow.Flow

class HudViewModel(
    gyro: GyroscopeMonitor
) : ViewModel() {
    val azimuth: Flow<Float> = gyro.streamAzimuth()
}
