package com.orion.navigator.data

/**
 * Data class representing a navigation beacon (waypoint).
 */
data class NavBeacon(
    val lat: Double,
    val lng: Double,
    val ts: Long = System.currentTimeMillis()
)
