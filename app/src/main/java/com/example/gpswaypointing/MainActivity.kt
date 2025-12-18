package com.example.gpswaypointing

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.gpswaypointing.data.LocationStorageManager
import com.example.gpswaypointing.service.GPSMonitor
import com.example.gpswaypointing.service.OrientationProvider
import com.example.gpswaypointing.theme.VintageMapTheme

/**
 * Entry point for the Vintage Edition GPS Tracker.
 */
class MainActivity : ComponentActivity() {

    private lateinit var gpsMonitor: GPSMonitor
    private lateinit var orientationProvider: OrientationProvider
    private lateinit var storageManager: LocationStorageManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> 
        // Logic handled in composable state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Managers
        gpsMonitor = GPSMonitor(this)
        orientationProvider = OrientationProvider(this)
        storageManager = LocationStorageManager(this)

        if (!gpsMonitor.isPermissionGranted()) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            VintageMapTheme {
                MainExplorerInterface(
                    gpsMonitor = gpsMonitor,
                    orientationProvider = orientationProvider,
                    storageManager = storageManager
                )
            }
        }
    }
}
