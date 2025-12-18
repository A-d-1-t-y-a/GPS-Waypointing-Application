package com.example.gpswaypointing.domain.usecase

import android.location.Location
import com.example.gpswaypointing.data.Waypoint

/**
 * Use case for filtering waypoints within a specified range.
 */
class FilterWaypointsUseCase {
    /**
     * Returns waypoints within the specified scale range from current location.
     */
    operator fun invoke(
        waypoints: List<Waypoint>,
        location: Location,
        scaleMeters: Float
    ): List<Waypoint> {
        return waypoints.filter { waypoint ->
            val waypointLocation = Location("").apply {
                latitude = waypoint.latitude
                longitude = waypoint.longitude
            }
            location.distanceTo(waypointLocation) <= scaleMeters
        }
    }
}

