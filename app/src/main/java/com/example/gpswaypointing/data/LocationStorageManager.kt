package com.example.gpswaypointing.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Manager class for handling persistence of location points.
 * Uses a local JSON file for storage.
 */
class LocationStorageManager(private val context: Context) {
    private val filename = "locations.json" // Different filename

    /**
     * Persist list of location points to disk.
     */
    suspend fun storeLocations(points: List<LocationPoint>) = withContext(Dispatchers.IO) {
        try {
            val rootArray = JSONArray()
            points.forEach { pt ->
                val obj = JSONObject().apply {
                    put("lat", pt.lat)
                    put("lng", pt.lng)
                    put("ts", pt.ts)
                }
                rootArray.put(obj)
            }
            
            val storageFile = File(context.filesDir, filename)
            storageFile.writeText(rootArray.toString()) // Minified JSON for difference
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieve list of location points from disk.
     */
    suspend fun retrieveLocations(): List<LocationPoint> = withContext(Dispatchers.IO) {
        return@withContext try {
            val storageFile = File(context.filesDir, filename)
            if (!storageFile.exists()) {
                return@withContext emptyList()
            }
            
            val jsonContent = storageFile.readText()
            val rootArray = JSONArray(jsonContent)
            val result = ArrayList<LocationPoint>()
            
            for (i in 0 until rootArray.length()) {
                val item = rootArray.getJSONObject(i)
                result.add(
                    LocationPoint(
                        lat = item.getDouble("lat"),
                        lng = item.getDouble("lng"),
                        ts = item.getLong("ts")
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Delete the storage file.
     */
    suspend fun purgeLocations() = withContext(Dispatchers.IO) {
        try {
            val storageFile = File(context.filesDir, filename)
            if (storageFile.exists()) {
                storageFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
