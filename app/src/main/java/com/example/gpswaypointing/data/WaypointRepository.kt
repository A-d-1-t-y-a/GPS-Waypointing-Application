package com.example.gpswaypointing.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Repository for managing waypoint persistence using standard org.json.
 * Handles reading and writing waypoints to internal storage.
 */
class WaypointRepository(private val context: Context) {
    private val fileName = "waypoints.json"

    /**
     * Saves the list of waypoints to internal storage.
     * Immediately persists to file when called.
     */
    suspend fun saveWaypoints(waypoints: List<Waypoint>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            waypoints.forEach { waypoint ->
                val jsonObject = JSONObject().apply {
                    put("latitude", waypoint.latitude)
                    put("longitude", waypoint.longitude)
                }
                jsonArray.put(jsonObject)
            }
            
            val file = File(context.filesDir, fileName)
            file.writeText(jsonArray.toString(2)) // Indent with 2 spaces for pretty print
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads waypoints from internal storage.
     * Returns empty list if file doesn't exist or parsing fails.
     */
    suspend fun loadWaypoints(): List<Waypoint> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                return@withContext emptyList()
            }
            
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val waypoints = mutableListOf<Waypoint>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                waypoints.add(
                    Waypoint(
                        latitude = jsonObject.getDouble("latitude"),
                        longitude = jsonObject.getDouble("longitude")
                    )
                )
            }
            waypoints
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Clears all waypoints by deleting the storage file.
     */
    suspend fun clearWaypoints() = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

