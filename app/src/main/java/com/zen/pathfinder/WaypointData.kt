package com.zen.pathfinder

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class WaypointData(
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFileString(): String {
        return "$id,$latitude,$longitude,$timestamp"
    }

    companion object {
        fun fromFileString(str: String): WaypointData? {
            return try {
                val parts = str.split(",")
                WaypointData(
                    id = parts[0],
                    latitude = parts[1].toDouble(),
                    longitude = parts[2].toDouble(),
                    timestamp = parts[3].toLong()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
