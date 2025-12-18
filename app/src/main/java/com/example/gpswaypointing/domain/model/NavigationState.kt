package com.example.gpswaypointing.domain.model

import android.location.Location
import com.example.gpswaypointing.data.Waypoint

/**
 * Sealed class representing different navigation states.
 */
sealed class NavigationState {
    object Idle : NavigationState()
    data class Navigating(
        val selectedWaypoint: Waypoint,
        val distance: Float,
        val bearing: Float
    ) : NavigationState()
}

