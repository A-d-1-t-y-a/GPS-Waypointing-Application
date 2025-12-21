package com.nav.tracker.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nav.tracker.data.BeaconRegistry
import com.nav.tracker.data.NavBeacon
import com.nav.tracker.domain.usecase.MeasureDistance
import com.nav.tracker.domain.usecase.ScanBeacons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Beacon (Waypoint) state.
 * Handles adding, selecting, and filtering beacons.
 */
class BeaconsViewModel(
    private val repo: BeaconRegistry
) : ViewModel() {
    
    private val distCalc = MeasureDistance()
    private val scanner = ScanBeacons()

    private val _beaconList = MutableStateFlow<List<NavBeacon>>(emptyList())
    val beaconList: StateFlow<List<NavBeacon>> = _beaconList

    private val _activeIdx = MutableStateFlow<Int?>(null)
    val activeIdx: StateFlow<Int?> = _activeIdx

    private val _currPos = MutableStateFlow<Location?>(null)
    val currPos: StateFlow<Location?> = _currPos

    private val _radarRange = MutableStateFlow(500f)
    val radarRange: StateFlow<Float> = _radarRange

    val activeBeacon: NavBeacon?
        get() = _activeIdx.value?.let { _beaconList.value.getOrNull(it) }

    init {
        loadDb()
    }

    /**
     * Loads beacons from persistence.
     */
    private fun loadDb() {
        viewModelScope.launch {
            _beaconList.value = repo.retrieveBeacons()
        }
    }

    /**
     * Deploys a new beacon at the given location.
     * @param loc The Location to save.
     */
    fun deployBeacon(loc: Location) {
        viewModelScope.launch {
            val newB = NavBeacon(loc.latitude, loc.longitude)
            val list = _beaconList.value + newB
            _beaconList.value = list
            repo.commitBeacons(list)
        }
    }

    /**
     * Updates current user position and checks proximity to waypoints.
     */
    fun updatePos(loc: Location) {
        _currPos.value = loc
        checkProximity(loc)
    }

    /**
     * Checks if user is within 10m of target to auto-switch.
     */
    private fun checkProximity(loc: Location) {
        val idx = _activeIdx.value ?: return
        val target = _beaconList.value.getOrNull(idx) ?: return
        
        if (distCalc(loc, target) <= 10f) {
            // Auto Select previous
            if (idx > 0) _activeIdx.value = idx - 1
        }
    }

    /**
     * Selects a beacon by index.
     */
    fun selectBeacon(idx: Int) {
        _activeIdx.value = idx
    }

    /**
     * Deletes all beacons.
     */
    fun purgeAll() {
        viewModelScope.launch {
            _beaconList.value = emptyList()
            _activeIdx.value = null
            repo.wipeRegistry()
        }
    }

    /**
     * Sets radar zoom range (500m - 2000m).
     */
    fun setRange(r: Float) {
        _radarRange.value = r.coerceIn(500f, 2000f)
    }

    /**
     * Returns list of beacons visible within current range.
     */
    fun getVisibleBeacons(): List<NavBeacon> {
        val p = _currPos.value ?: return emptyList()
        return scanner(_beaconList.value, p, _radarRange.value)
    }
}
