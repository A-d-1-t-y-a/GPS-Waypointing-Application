package com.example.gpswaypointing.viewmodel

import androidx.lifecycle.ViewModel
import com.example.gpswaypointing.service.SensorService
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for compass related data.
 */
class CompassViewModel(
    sensorService: SensorService
) : ViewModel() {
    val compassRotation: Flow<Float> = sensorService.getCompassRotation()
}
