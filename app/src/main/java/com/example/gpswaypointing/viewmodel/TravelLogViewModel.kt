package com.example.gpswaypointing.viewmodel

import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpswaypointing.data.LocationPoint
import com.example.gpswaypointing.data.LocationStorageManager
import com.example.gpswaypointing.utils.GeoMathUtils
import kotlinx.coroutines.launch

/**
 * ViewModel managing the log of visited locations (Waypoints).
 * Uses direct MutableState for UI updates instead of Flows.
 */
class TravelLogViewModel(
    private val storageManager: LocationStorageManager
) : ViewModel() {

    private val _logEntries = mutableStateOf<List<LocationPoint>>(emptyList())
    val logEntries: State<List<LocationPoint>> = _logEntries

    private val _userLocation = mutableStateOf<Location?>(null)
    val userLocation: State<Location?> = _userLocation

    private val _activeTargetIndex = mutableStateOf<Int?>(null)
    val activeTargetIndex: State<Int?> = _activeTargetIndex
    
    // Zoom scale for map (default 500m)
    private val _zoomLevel = mutableStateOf(500f)
    val zoomLevel: State<Float> = _zoomLevel

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _logEntries.value = storageManager.retrieveLocations()
        }
    }

    fun updateUserPosition(loc: Location) {
        _userLocation.value = loc
        checkAutoSelection(loc)
    }

    fun recordLocation(loc: Location) {
        val newPoint = LocationPoint(loc.latitude, loc.longitude)
        val currentList = _logEntries.value.toMutableList()
        currentList.add(newPoint)
        _logEntries.value = currentList
        persistData()
    }

    fun clearLog() {
        _logEntries.value = emptyList()
        _activeTargetIndex.value = null
        viewModelScope.launch {
            storageManager.purgeLocations()
        }
    }

    fun setTarget(index: Int) {
        _activeTargetIndex.value = index
    }

    fun setZoom(meters: Float) {
        _zoomLevel.value = meters
    }
    
    fun getTargetPoint(): LocationPoint? {
        return _activeTargetIndex.value?.let { _logEntries.value.getOrNull(it) }
    }
    
    fun getPointsInView(): List<LocationPoint> {
        val center = _userLocation.value ?: return emptyList()
        return GeoMathUtils.filterNearby(
            center.latitude, 
            center.longitude, 
            _logEntries.value, 
            _zoomLevel.value
        )
    }

    private fun persistData() {
        viewModelScope.launch {
            storageManager.storeLocations(_logEntries.value)
        }
    }
    
    private fun checkAutoSelection(currentLoc: Location) {
        val previousTarget = _activeTargetIndex.value?.let { _logEntries.value.getOrNull(it) }
        
        // If we have a target active, check if we actively reached it? 
        // Or assignment says "auto-select previous waypoint". 
        // "Logic for auto-selecting a previous waypoint when the user walks close enough to it (within 10m)."
        
        // Let's find the closest waypoint within 10m
        val closestIndex = _logEntries.value.indexOfFirst { pt ->
            GeoMathUtils.computeDistance(
                currentLoc.latitude, currentLoc.longitude,
                pt.lat, pt.lng
            ) < 10.0f
        }
        
        if (closestIndex != -1 && closestIndex != _activeTargetIndex.value) {
            _activeTargetIndex.value = closestIndex
        }
    }
}
