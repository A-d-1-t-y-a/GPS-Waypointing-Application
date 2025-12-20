package com.zen.pathfinder

import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class AppViewModel(private val fileManager: WaypointFileManager) : ViewModel() {

    var waypoints = mutableStateOf<List<WaypointData>>(emptyList())
        private set

    var selectedWaypointIndex = mutableStateOf<Int?>(null)
        private set

    var currentLocation = mutableStateOf<Location?>(null)
        private set

    var compassRange = mutableStateOf(500f) // Scale in meters
        private set

    init {
        // Load initial waypoints
        viewModelScope.launch {
            waypoints.value = fileManager.loadWaypoints()
        }
    }

    fun addWaypoint(location: Location) {
        viewModelScope.launch {
            val validLocation = location // In production check for validity
            val newWaypoint = WaypointData(
                latitude = validLocation.latitude,
                longitude = validLocation.longitude
            )
            val updatedList = waypoints.value + newWaypoint
            waypoints.value = updatedList
            fileManager.saveWaypoints(updatedList)
        }
    }

    fun deleteWaypoints() {
        viewModelScope.launch {
            waypoints.value = emptyList()
            selectedWaypointIndex.value = null
            fileManager.clearWaypoints() 
        }
    }

    fun selectWaypoint(index: Int) {
        if (index in waypoints.value.indices) {
            selectedWaypointIndex.value = index
        } else {
            selectedWaypointIndex.value = null
        }
    }

    fun updateLocation(location: Location) {
        currentLocation.value = location
        checkAutoNavigation(location)
    }

    private fun checkAutoNavigation(location: Location) {
        val index = selectedWaypointIndex.value
        if (index != null && index > 0) {
            val target = waypoints.value.getOrNull(index)
            if (target != null) {
                // Calculate distance
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, target.latitude, target.longitude, results)
                val distance = results[0]
                
                // "The previous waypoint should be automatically selected when a user gets within 10 metres of the current waypoint"
                if (distance < 10) {
                    selectedWaypointIndex.value = index - 1
                }
            }
        }
    }

    fun updateRange(newRange: Float) {
        // "adjust the scale from a minimum of 500 metres to a maximum of 2 kilometres"
        compassRange.value = newRange.coerceIn(500f, 2000f)
    }
}
