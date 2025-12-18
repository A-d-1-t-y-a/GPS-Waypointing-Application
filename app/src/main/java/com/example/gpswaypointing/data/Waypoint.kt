package com.example.gpswaypointing.data

/**
 * Data class representing a GPS waypoint with latitude, longitude, and timestamp.
 */
data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

