package com.zen.pathfinder.system

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

class PathSense(ctx: Context) {
    private val sys = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun flow(): Flow<Location> = callbackFlow {
        val list = object : LocationListener {
            override fun onLocationChanged(l: Location) { trySend(l) }
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        
        sys.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 0f, list, Looper.getMainLooper())
        sys.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 0f, list, Looper.getMainLooper())
        
        awaitClose { sys.removeUpdates(list) }
    }

    fun allowed(c: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            c, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
