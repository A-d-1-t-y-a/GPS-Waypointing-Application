package com.vintagenav.explorer.data

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
class LocationStorageManager(private val appCtx: Context) {
    private val storageFile = "survey_log.json"

    /**
     * Persist list of location points to disk.
     */
    suspend fun storeLocations(pts: List<LocationPoint>) = withContext(Dispatchers.IO) {
        try {
            val nodeArray = JSONArray()
            pts.forEach { p ->
                val record = JSONObject().apply {
                    put("latitude", p.lat)
                    put("longitude", p.lng)
                    put("time_start", p.timestamp)
                }
                nodeArray.put(record)
            }
            
            val destFile = File(appCtx.filesDir, storageFile)
            destFile.writeText(nodeArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieve list of location points from disk.
     */
    suspend fun retrieveLocations(): List<LocationPoint> = withContext(Dispatchers.IO) {
        return@withContext try {
            val fileObj = File(appCtx.filesDir, storageFile)
            if (!fileObj.exists()) {
                return@withContext emptyList()
            }
            
            val rawContent = fileObj.readText()
            val parsedArray = JSONArray(rawContent)
            val outputList = ArrayList<LocationPoint>()
            
            for (i in 0 until parsedArray.length()) {
                val node = parsedArray.getJSONObject(i)
                outputList.add(
                    LocationPoint(
                        lat = node.getDouble("latitude"),
                        lng = node.getDouble("longitude"),
                        timestamp = node.getLong("time_start")
                    )
                )
            }
            outputList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Delete the storage file.
     */
    suspend fun wipeData() = withContext(Dispatchers.IO) {
        try {
            val f = File(appCtx.filesDir, storageFile)
            if (f.exists()) {
                f.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
