package com.zen.pathfinder

import android.content.Context
import java.io.File
import java.io.IOException

class WaypointFileManager(private val context: Context) {
    private val fileName = "waypoints.txt"

    fun saveWaypoints(waypoints: List<WaypointData>) {
        try {
            val file = File(context.filesDir, fileName)
            val fileContent = StringBuilder()
            for (waypoint in waypoints) {
                fileContent.append(waypoint.toFileString()).append("\n")
            }
            file.writeText(fileContent.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadWaypoints(): MutableList<WaypointData> {
        val waypoints = mutableListOf<WaypointData>()
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            try {
                file.readLines().forEach { line ->
                    if (line.isNotBlank()) {
                        WaypointData.fromFileString(line)?.let {
                            waypoints.add(it)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return waypoints
    }
    
    fun clearWaypoints() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
}
