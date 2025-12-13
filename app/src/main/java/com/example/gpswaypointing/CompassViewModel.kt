package com.example.gpswaypointing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.location.Location

/**
 * ViewModel-like state holder for compass and waypoint navigation state.
 * Manages selected waypoint, scale, and tracking status.
 */
class CompassState {
    var isTracking by mutableStateOf(false)
    var currentLocation: Location? by mutableStateOf(null)
    var waypoints by mutableStateOf<List<Waypoint>>(emptyList())
    var selectedWaypointIndex by mutableStateOf<Int?>(null)
    var compassRotation by mutableStateOf(0f) // Rotation in degrees
    var scaleMeters by mutableStateOf(500f) // Scale in meters (500m to 2000m)
    var showClearDialog by mutableStateOf(false)

    /**
     * Gets the currently selected waypoint.
     */
    val selectedWaypoint: Waypoint?
        get() = selectedWaypointIndex?.let { 
            if (it in waypoints.indices) waypoints[it] else null 
        }

    /**
     * Calculates distance from current location to selected waypoint in meters.
     */
    fun getDistanceToSelectedWaypoint(): Float? {
        val location = currentLocation ?: return null
        val waypoint = selectedWaypoint ?: return null
        val waypointLocation = Location("").apply {
            latitude = waypoint.latitude
            longitude = waypoint.longitude
        }
        return location.distanceTo(waypointLocation)
    }

    /**
     * Calculates bearing from current location to selected waypoint in degrees.
     */
    fun getBearingToSelectedWaypoint(): Float? {
        val location = currentLocation ?: return null
        val waypoint = selectedWaypoint ?: return null
        return location.bearingTo(
            Location("").apply {
                latitude = waypoint.latitude
                longitude = waypoint.longitude
            }
        )
    }

    /**
     * Gets waypoints within the current scale range.
     */
    fun getWaypointsInRange(): List<Waypoint> {
        val location = currentLocation ?: return emptyList()
        return waypoints.filter { waypoint ->
            val waypointLocation = Location("").apply {
                latitude = waypoint.latitude
                longitude = waypoint.longitude
            }
            location.distanceTo(waypointLocation) <= scaleMeters
        }
    }

    /**
     * Selects the previous waypoint in the list.
     */
    fun selectPreviousWaypoint() {
        selectedWaypointIndex?.let { currentIndex ->
            if (currentIndex > 0) {
                selectedWaypointIndex = currentIndex - 1
            }
        }
    }
}

