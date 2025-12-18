package com.example.gpswaypointing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.location.Location
import com.example.gpswaypointing.data.Waypoint
import com.example.gpswaypointing.data.WaypointRepository
import com.example.gpswaypointing.domain.usecase.CalculateDistanceUseCase
import com.example.gpswaypointing.domain.usecase.FilterWaypointsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing waypoint state and operations.
 */
class WaypointViewModel(
    private val waypointRepository: WaypointRepository,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val filterWaypointsUseCase: FilterWaypointsUseCase
) : ViewModel() {

    private val _waypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val waypoints: StateFlow<List<Waypoint>> = _waypoints.asStateFlow()

    private val _selectedWaypointIndex = MutableStateFlow<Int?>(null)
    val selectedWaypointIndex: StateFlow<Int?> = _selectedWaypointIndex.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _scaleMeters = MutableStateFlow(500f)
    val scaleMeters: StateFlow<Float> = _scaleMeters.asStateFlow()

    val selectedWaypoint: Waypoint?
        get() = _selectedWaypointIndex.value?.let { index ->
            if (index in _waypoints.value.indices) _waypoints.value[index] else null
        }

    init {
        loadWaypoints()
    }

    /**
     * Loads waypoints from repository.
     */
    private fun loadWaypoints() {
        viewModelScope.launch {
            _waypoints.value = waypointRepository.loadWaypoints()
        }
    }

    /**
     * Adds a new waypoint at current location.
     */
    fun addWaypoint(location: Location) {
        viewModelScope.launch {
            val newWaypoint = Waypoint(
                latitude = location.latitude,
                longitude = location.longitude
            )
            val updatedWaypoints = _waypoints.value + newWaypoint
            _waypoints.value = updatedWaypoints
            waypointRepository.saveWaypoints(updatedWaypoints)
        }
    }

    /**
     * Updates current location.
     */
    fun updateLocation(location: Location) {
        _currentLocation.value = location
        checkAutoSelection(location)
    }

    /**
     * Checks if user is within 10m of selected waypoint and auto-selects previous.
     */
    private fun checkAutoSelection(location: Location) {
        val selectedIndex = _selectedWaypointIndex.value ?: return
        if (selectedIndex <= 0) return

        val waypoint = _waypoints.value.getOrNull(selectedIndex) ?: return
        val distance = calculateDistanceUseCase(location, waypoint)

        if (distance <= 10f) {
            _selectedWaypointIndex.value = selectedIndex - 1
        }
    }

    /**
     * Selects a waypoint by index.
     */
    fun selectWaypoint(index: Int) {
        if (index in _waypoints.value.indices) {
            _selectedWaypointIndex.value = index
        }
    }

    /**
     * Clears all waypoints.
     */
    fun clearWaypoints() {
        viewModelScope.launch {
            _waypoints.value = emptyList()
            _selectedWaypointIndex.value = null
            waypointRepository.clearWaypoints()
        }
    }

    /**
     * Updates the compass scale.
     */
    fun updateScale(newScale: Float) {
        _scaleMeters.value = newScale.coerceIn(500f, 2000f)
    }

    /**
     * Gets waypoints within current scale range.
     */
    fun getWaypointsInRange(): List<Waypoint> {
        val location = _currentLocation.value ?: return emptyList()
        return filterWaypointsUseCase(_waypoints.value, location, _scaleMeters.value)
    }
}
