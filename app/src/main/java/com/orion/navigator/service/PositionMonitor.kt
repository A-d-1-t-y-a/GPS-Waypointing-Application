package com.orion.navigator.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PositionMonitor(private val ctx: Context) {
    private val mgr = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun trackPosition(): Flow<Location> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) { trySend(loc) }
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        
        val providers = mgr.getProviders(true)
        providers.forEach { p ->
            mgr.requestLocationUpdates(p, 5000L, 5f, listener, Looper.getMainLooper())
        }
        
        awaitClose { mgr.removeUpdates(listener) }
    }

    fun hasPerms(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
