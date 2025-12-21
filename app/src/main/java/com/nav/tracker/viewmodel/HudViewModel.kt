package com.nav.tracker.viewmodel

import androidx.lifecycle.ViewModel
import com.nav.tracker.service.GyroscopeMonitor
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for the Heads-Up Display (Compass) logic.
 * Exposes sensor data to the UI.
 * @param gyro The GyroscopeMonitor instance.
 */
class HudViewModel(
    gyro: GyroscopeMonitor
) : ViewModel() {
    /**
     * Flow of current azimuth (compass direction).
     */
    val azimuth: Flow<Float> = gyro.streamAzimuth()
}
