package com.vintagenav.explorer.utils

import android.location.Location
import com.vintagenav.explorer.data.LocationPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class for geographical calculations.
 */
object GeoMathUtils {

    /**
     * Compute distance in meters between two points.
     */
    fun calcDist(originLat: Double, originLng: Double, destLat: Double, destLng: Double): Float {
        val outRes = FloatArray(1)
        Location.distanceBetween(originLat, originLng, destLat, destLng, outRes)
        return outRes[0]
    }

    /**
     * Compute bearing in degrees from start to end.
     */
    fun calcBearing(originLat: Double, originLng: Double, destLat: Double, destLng: Double): Float {
        val outRes = FloatArray(2)
        Location.distanceBetween(originLat, originLng, destLat, destLng, outRes)
        return outRes[1]
    }
    
    /**
     * Helper to filter points within a radius.
     */
    fun filterNearby(centerLat: Double, centerLng: Double, markerList: List<LocationPoint>, rangeMeters: Float): List<LocationPoint> {
        return markerList.filter { 
            calcDist(centerLat, centerLng, it.lat, it.lng) <= rangeMeters 
        }
    }
}
