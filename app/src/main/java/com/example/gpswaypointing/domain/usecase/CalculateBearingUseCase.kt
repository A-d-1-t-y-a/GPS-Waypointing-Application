package com.example.gpswaypointing.domain.usecase

import android.location.Location
import com.example.gpswaypointing.data.Waypoint

/**
 * Use case for calculating bearing from current location to a waypoint.
 */
class CalculateBearingUseCase {
    /**
     * Calculates bearing in degrees from current location to waypoint.
     */
    operator fun invoke(location: Location, waypoint: Waypoint): Float {
        val waypointLocation = Location("").apply {
            latitude = waypoint.latitude
            longitude = waypoint.longitude
        }
        return location.bearingTo(waypointLocation)
    }
}

