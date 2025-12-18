package com.example.gpswaypointing.service

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
class GPSMonitor(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Check if permissions are granted.
     */
    fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get a stream of location updates using callbackFlow.
     * Updates every 5 seconds (5000ms).
     */
    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(): Flow<Location> = callbackFlow {
        if (!isPermissionGranted()) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L, // 5 seconds interval
            0f,
            listener
        )

        // Also look for last known
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        lastKnown?.let { trySend(it) }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }
}
