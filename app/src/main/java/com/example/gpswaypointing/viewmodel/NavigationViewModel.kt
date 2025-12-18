package com.example.gpswaypointing.viewmodel

import androidx.lifecycle.ViewModel
import android.location.Location
import com.example.gpswaypointing.data.Waypoint
import com.example.gpswaypointing.domain.model.NavigationState
import com.example.gpswaypointing.domain.usecase.CalculateBearingUseCase
import com.example.gpswaypointing.domain.usecase.CalculateDistanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing navigation state and calculations.
 */
class NavigationViewModel(
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val calculateBearingUseCase: CalculateBearingUseCase
) : ViewModel() {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    /**
     * Updates navigation state based on current location and selected waypoint.
     */
    fun updateNavigation(location: Location, selectedWaypoint: Waypoint?) {
        if (selectedWaypoint == null) {
            _navigationState.value = NavigationState.Idle
            return
        }

        val distance = calculateDistanceUseCase(location, selectedWaypoint)
        val bearing = calculateBearingUseCase(location, selectedWaypoint)

        _navigationState.value = NavigationState.Navigating(
            selectedWaypoint = selectedWaypoint,
            distance = distance,
            bearing = bearing
        )
    }
}
