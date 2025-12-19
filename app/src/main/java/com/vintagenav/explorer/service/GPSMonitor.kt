package com.vintagenav.explorer.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Service class to monitor GPS location updates.
 */
class GPSMonitor(private val sysCtx: Context) {
    private val locMgr = sysCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Check if permissions are granted.
     */
    fun checkPerms(): Boolean {
        return ContextCompat.checkSelfPermission(
            sysCtx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get a stream of location updates using callbackFlow.
     * Updates every 5 seconds (5000ms).
     */
    @SuppressLint("MissingPermission")
    fun streamLocation(): Flow<Location> = callbackFlow {
        if (!checkPerms()) {
            close()
            return@callbackFlow
        }

        val gpsListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locMgr.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L, // 5 seconds interval
            0f,
            gpsListener
        )

        // Also look for last known
        val knownPos = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        knownPos?.let { trySend(it) }

        awaitClose {
            locMgr.removeUpdates(gpsListener)
        }
    }
}
