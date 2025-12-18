package com.example.gpswaypointing.utils

import android.location.Location
import com.example.gpswaypointing.data.LocationPoint
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
    fun computeDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    /**
     * Compute bearing in degrees from start to end.
     */
    fun computeBearing(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float {
        val results = FloatArray(2)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[1]
    }
    
    /**
     * Helper to filter points within a radius.
     */
    fun filterNearby(centerLat: Double, centerLng: Double, points: List<LocationPoint>, radiusMeters: Float): List<LocationPoint> {
        return points.filter { 
            computeDistance(centerLat, centerLng, it.lat, it.lng) <= radiusMeters 
        }
    }
}
