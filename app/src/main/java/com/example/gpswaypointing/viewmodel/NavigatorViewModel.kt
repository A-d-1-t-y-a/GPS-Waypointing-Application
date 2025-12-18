package com.example.gpswaypointing.viewmodel

import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.gpswaypointing.data.LocationPoint
import com.example.gpswaypointing.utils.GeoMathUtils

/**
 * ViewModel handling navigation calculations (Distance/Bearing).
 */
class NavigatorViewModel : ViewModel() {
    
    data class NavData(
        val dist: Float = 0f,
        val azi: Float = 0f,
        val isActive: Boolean = false
    )

    private val _navStatus = mutableStateOf(NavData())
    val navStatus: State<NavData> = _navStatus

    fun refreshNavigation(userLoc: Location, target: LocationPoint?) {
        if (target == null) {
            _navStatus.value = NavData(isActive = false)
            return
        }

        val d = GeoMathUtils.computeDistance(userLoc.latitude, userLoc.longitude, target.lat, target.lng)
        val b = GeoMathUtils.computeBearing(userLoc.latitude, userLoc.longitude, target.lat, target.lng)
        
        _navStatus.value = NavData(
            dist = d,
            azi = b,
            isActive = true
        )
    }
}
