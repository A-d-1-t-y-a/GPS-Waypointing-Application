package com.example.gpswaypointing.data

import kotlinx.serialization.Serializable

/**
 * Data class representing a GPS waypoint with latitude, longitude, and timestamp.
 * Uses Kotlinx Serialization for persistence.
 */
@Serializable
data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

