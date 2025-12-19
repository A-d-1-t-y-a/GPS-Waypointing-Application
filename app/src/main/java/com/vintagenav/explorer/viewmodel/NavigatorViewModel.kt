package com.vintagenav.explorer.viewmodel

import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.vintagenav.explorer.data.LocationPoint
import com.vintagenav.explorer.utils.GeoMathUtils

/**
 * ViewModel handling navigation calculations (Distance/Bearing).
 */
class NavigatorViewModel : ViewModel() {
    
    data class NavData(
        val dist: Float = 0f,
        val azi: Float = 0f,
        val isActive: Boolean = false
    )

    private val _guidanceState = mutableStateOf(NavData())
    val guidanceState: State<NavData> = _guidanceState

    fun updateGuidance(userLoc: Location, target: LocationPoint?) {
        if (target == null) {
            _guidanceState.value = NavData(isActive = false)
            return
        }

        val d = GeoMathUtils.calcDist(userLoc.latitude, userLoc.longitude, target.lat, target.lng)
        val b = GeoMathUtils.calcBearing(userLoc.latitude, userLoc.longitude, target.lat, target.lng)
        
        _guidanceState.value = NavData(
            dist = d,
            azi = b,
            isActive = true
        )
    }
}
