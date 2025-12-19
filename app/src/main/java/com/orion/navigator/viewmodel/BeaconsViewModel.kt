package com.orion.navigator.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.navigator.data.BeaconRegistry
import com.orion.navigator.data.NavBeacon
import com.orion.navigator.domain.usecase.MeasureDistance
import com.orion.navigator.domain.usecase.ScanBeacons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private fun loadDb() {
        viewModelScope.launch {
            _beaconList.value = repo.retrieveBeacons()
        }
    }

    fun deployBeacon(loc: Location) {
        viewModelScope.launch {
            val newB = NavBeacon(loc.latitude, loc.longitude)
            val list = _beaconList.value + newB
            _beaconList.value = list
            repo.commitBeacons(list)
        }
    }

    fun updatePos(loc: Location) {
        _currPos.value = loc
        checkProximity(loc)
    }

    private fun checkProximity(loc: Location) {
        val idx = _activeIdx.value ?: return
        val target = _beaconList.value.getOrNull(idx) ?: return
        
        if (distCalc(loc, target) <= 10f) {
            // Auto Select previous
            if (idx > 0) _activeIdx.value = idx - 1
        }
    }

    fun selectBeacon(idx: Int) {
        _activeIdx.value = idx
    }

    fun purgeAll() {
        viewModelScope.launch {
            _beaconList.value = emptyList()
            _activeIdx.value = null
            repo.wipeRegistry()
        }
    }

    fun setRange(r: Float) {
        _radarRange.value = r.coerceIn(500f, 2000f)
    }

    fun getVisibleBeacons(): List<NavBeacon> {
        val p = _currPos.value ?: return emptyList()
        return scanner(_beaconList.value, p, _radarRange.value)
    }
}
