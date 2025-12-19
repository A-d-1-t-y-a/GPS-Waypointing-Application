package com.orion.navigator.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Registry for persisting navigational beacons.
 */
class BeaconRegistry(private val ctx: Context) {
    private val storeFile = "beacons_db.json"

    suspend fun commitBeacons(list: List<NavBeacon>) = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            list.forEach { b ->
                val obj = JSONObject().apply {
                    put("lat", b.lat)
                    put("lng", b.lng)
                    put("ts", b.ts)
                }
                arr.put(obj)
            }
            
            File(ctx.filesDir, storeFile).writeText(arr.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun retrieveBeacons(): List<NavBeacon> = withContext(Dispatchers.IO) {
        return@withContext try {
            val f = File(ctx.filesDir, storeFile)
            if (!f.exists()) return@withContext emptyList()
            
            val raw = f.readText()
            val arr = JSONArray(raw)
            val res = mutableListOf<NavBeacon>()
            
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                res.add(NavBeacon(
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    ts = obj.optLong("ts", 0L)
                ))
            }
            res
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun wipeRegistry() = withContext(Dispatchers.IO) {
        try {
            val f = File(ctx.filesDir, storeFile)
            if (f.exists()) f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
