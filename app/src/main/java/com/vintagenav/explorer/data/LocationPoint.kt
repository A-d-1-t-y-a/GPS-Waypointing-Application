package com.vintagenav.explorer.data

/**
 * Data model for a stored location.
 */
data class LocationPoint(
    val lat: Double,
    val lng: Double,z
    val timestamp: Long = System.currentTimeMillis()
)
