package com.example.gpswaypointing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Service class for managing GPS location updates.
 * Handles location permission checks and provides location updates via callback.
 */
class LocationService(private val context: Context) {
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null
    private val updateInterval: Long = 5000 // 5 seconds

    /**
     * Checks if location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts GPS tracking with location updates every 5 seconds.
     * Calls the provided callback with each location update.
     */
    fun startTracking(onLocationUpdate: (Location) -> Unit) {
        if (!hasLocationPermission()) {
            return
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocationUpdate(location)
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                updateInterval,
                0f,
                locationListener!!
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Stops GPS tracking and removes the location listener.
     */
    fun stopTracking() {
        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        locationListener = null
    }

    /**
     * Gets the last known location if available.
     */
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        }
    }
}

