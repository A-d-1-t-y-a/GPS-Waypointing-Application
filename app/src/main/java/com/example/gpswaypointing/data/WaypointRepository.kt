package com.example.gpswaypointing.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Repository for managing waypoint persistence using Kotlinx Serialization.
 * Handles reading and writing waypoints to internal storage.
 */
class WaypointRepository(private val context: Context) {
    private val fileName = "waypoints.json"
    private val json = Json { prettyPrint = true }

    /**
     * Saves the list of waypoints to internal storage.
     * Immediately persists to file when called.
     */
    suspend fun saveWaypoints(waypoints: List<Waypoint>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            val jsonString = json.encodeToString(waypoints)
            file.writeText(jsonString)
        } catch (e: IOException) {
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
            json.decodeFromString<List<Waypoint>>(jsonString)
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

