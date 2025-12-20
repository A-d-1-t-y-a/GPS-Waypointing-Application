package com.zen.pathfinder

import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

import android.content.Context
import android.content.SharedPreferences

class AppViewModel(private val fileManager: WaypointFileManager, context: Context) : ViewModel() {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("pathfinder_prefs", Context.MODE_PRIVATE)

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
        
        // Load last known location
        val lat = prefs.getFloat("last_lat", Float.NaN)
        val lon = prefs.getFloat("last_lon", Float.NaN)
        if (!lat.isNaN() && !lon.isNaN()) {
            val loc = Location("last_known")
            loc.latitude = lat.toDouble()
            loc.longitude = lon.toDouble()
            loc.time = System.currentTimeMillis()
            currentLocation.value = loc
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
        
        // Save to prefs
        with(prefs.edit()) {
            putFloat("last_lat", location.latitude.toFloat())
            putFloat("last_lon", location.longitude.toFloat())
            apply()
        }
        
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
