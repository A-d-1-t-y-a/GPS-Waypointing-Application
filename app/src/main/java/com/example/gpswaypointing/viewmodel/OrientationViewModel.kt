package com.example.gpswaypointing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpswaypointing.service.OrientationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel handling device orientation data.
 */
class OrientationViewModel(private val orientationProvider: OrientationProvider) : ViewModel() {
    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth

    init {
        viewModelScope.launch {
            orientationProvider.orientationFlow().collect {
                _azimuth.value = it
            }
        }
    }
}
