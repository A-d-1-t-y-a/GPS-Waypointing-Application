package com.zen.pathfinder.core

/**
 * Represents a marker on the path.
 */
data class GuideStone(
    val y: Double, // Latitude
    val x: Double, // Longitude
    val epoch: Long = System.currentTimeMillis()
)
