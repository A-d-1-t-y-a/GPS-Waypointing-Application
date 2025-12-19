package com.vintagenav.explorer.viewmodel

import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vintagenav.explorer.data.LocationPoint
import com.vintagenav.explorer.data.LocationStorageManager
import com.vintagenav.explorer.utils.GeoMathUtils
import kotlinx.coroutines.launch

/**
 * ViewModel managing the log of visited locations (Waypoints).
 * Uses direct MutableState for UI updates instead of Flows.
 */
class TravelLogViewModel(
    private val dataStore: LocationStorageManager
) : ViewModel() {

    private val _journalEntries = mutableStateOf<List<LocationPoint>>(emptyList())
    val journalEntries: State<List<LocationPoint>> = _journalEntries

    private val _myLoc = mutableStateOf<Location?>(null)
    val myLoc: State<Location?> = _myLoc

    private val _targetIdx = mutableStateOf<Int?>(null)
    val targetIdx: State<Int?> = _targetIdx
    
    // Zoom scale for map (default 500m)
    private val _viewScale = mutableStateOf(500f)
    val viewScale: State<Float> = _viewScale

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _journalEntries.value = dataStore.retrieveLocations()
        }
    }

    fun updateMyPosition(loc: Location) {
        _myLoc.value = loc
        checkAutoSelection(loc)
    }

    fun recordSpot(loc: Location) {
        val newPoint = LocationPoint(loc.latitude, loc.longitude)
        val currentList = _journalEntries.value.toMutableList()
        currentList.add(newPoint)
        _journalEntries.value = currentList
        persistData()
    }

    fun clearJournal() {
        _journalEntries.value = emptyList()
        _targetIdx.value = null
        viewModelScope.launch {
            dataStore.wipeData()
        }
    }

    fun setTarget(index: Int) {
        _targetIdx.value = index
    }

    fun setScale(meters: Float) {
        _viewScale.value = meters
    }
    
    fun getActiveTarget(): LocationPoint? {
        return _targetIdx.value?.let { _journalEntries.value.getOrNull(it) }
    }
    
    fun getVisiblePoints(): List<LocationPoint> {
        val center = _myLoc.value ?: return emptyList()
        return GeoMathUtils.filterNearby(
            center.latitude, 
            center.longitude, 
            _journalEntries.value, 
            _viewScale.value
        )
    }

    private fun persistData() {
        viewModelScope.launch {
            dataStore.storeLocations(_journalEntries.value)
        }
    }
    
    private fun checkAutoSelection(currentLoc: Location) {
        val previousTarget = _targetIdx.value?.let { _journalEntries.value.getOrNull(it) }
        
        // If we have a target active, check if we actively reached it? 
        // Or assignment says "auto-select previous waypoint". 
        // "Logic for auto-selecting a previous waypoint when the user walks close enough to it (within 10m)."
        
        // Let's find the closest waypoint within 10m
        val closestIndex = _journalEntries.value.indexOfFirst { pt ->
            GeoMathUtils.calcDist(
                currentLoc.latitude, currentLoc.longitude,
                pt.lat, pt.lng
            ) < 10.0f
        }
        
        if (closestIndex != -1 && closestIndex != _targetIdx.value) {
            _targetIdx.value = closestIndex
        }
    }
}
