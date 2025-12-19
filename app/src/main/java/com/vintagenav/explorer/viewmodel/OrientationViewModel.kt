package com.vintagenav.explorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vintagenav.explorer.service.OrientationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel handling device orientation data.
 */
class OrientationViewModel(private val compassSensor: OrientationProvider) : ViewModel() {
    private val _heading = MutableStateFlow(0f)
    val currHeading: StateFlow<Float> = _heading

    init {
        viewModelScope.launch {
            compassSensor.sensorStream().collect {
                _heading.value = it
            }
        }
    }
}
