package com.example.gpswaypointing.data

/**
 * Data model for a stored location.
 */
data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val ts: Long = System.currentTimeMillis()
)
