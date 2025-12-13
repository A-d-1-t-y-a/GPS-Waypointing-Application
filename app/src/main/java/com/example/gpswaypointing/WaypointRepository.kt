package com.example.gpswaypointing

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Repository class for managing waypoint persistence to file storage.
 * Handles reading and writing waypoints to internal storage as JSON.
 */
class WaypointRepository(private val context: Context) {
    private val fileName = "waypoints.json"

    /**
     * Saves the list of waypoints to internal storage as JSON.
     * Immediately updates the file when called.
     */
    fun saveWaypoints(waypoints: List<Waypoint>) {
        try {
            val file = File(context.filesDir, fileName)
            val jsonArray = JSONArray()
            waypoints.forEach { waypoint ->
                jsonArray.put(waypoint.toJson())
            }
            FileOutputStream(file).use { output ->
                output.write(jsonArray.toString().toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the list of waypoints from internal storage.
     * Returns an empty list if file doesn't exist or parsing fails.
     */
    fun loadWaypoints(): List<Waypoint> {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                return emptyList()
            }
            FileInputStream(file).use { input ->
                val content = input.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(content)
                val waypoints = mutableListOf<Waypoint>()
                for (i in 0 until jsonArray.length()) {
                    waypoints.add(Waypoint.fromJson(jsonArray.getJSONObject(i)))
                }
                waypoints
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Clears all waypoints by deleting the file.
     */
    fun clearWaypoints() {
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

