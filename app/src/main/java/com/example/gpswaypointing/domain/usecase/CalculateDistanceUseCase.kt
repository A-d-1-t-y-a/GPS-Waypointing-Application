package com.example.gpswaypointing.domain.usecase

import android.location.Location
import com.example.gpswaypointing.data.Waypoint

/**
 * Use case for calculating distance between current location and a waypoint.
 */
class CalculateDistanceUseCase {
    /**
     * Calculates distance in meters from current location to waypoint.
     */
    operator fun invoke(location: Location, waypoint: Waypoint): Float {
        val waypointLocation = Location("").apply {
            latitude = waypoint.latitude
            longitude = waypoint.longitude
        }
        return location.distanceTo(waypointLocation)
    }
}

