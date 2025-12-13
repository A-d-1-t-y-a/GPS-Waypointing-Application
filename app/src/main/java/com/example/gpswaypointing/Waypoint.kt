package com.example.gpswaypointing

import org.json.JSONObject

/**
 * Data class representing a GPS waypoint with latitude, longitude, and timestamp.
 */
data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts the waypoint to a JSON object for file storage.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", timestamp)
        }
    }

    companion object {
        /**
         * Creates a Waypoint from a JSON object loaded from file.
         */
        fun fromJson(json: JSONObject): Waypoint {
            return Waypoint(
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                timestamp = json.getLong("timestamp")
            )
        }
    }
}

