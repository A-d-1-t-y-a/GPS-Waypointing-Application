package com.zen.pathfinder

import android.location.Location

object GeoUtils {
    fun calculateDistance(loc: Location, lat: Double, lon: Double): Float {
        val r = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, lat, lon, r)
        return r[0]
    }

    fun calculateBearing(loc: Location, lat: Double, lon: Double): Float {
        val r = FloatArray(2)
        Location.distanceBetween(loc.latitude, loc.longitude, lat, lon, r)
        // Bearing is r[1]
        return r[1]
    }
}
