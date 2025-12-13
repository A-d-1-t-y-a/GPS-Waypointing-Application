package com.example.gpswaypointing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gpswaypointing.ui.theme.GPSWaypointingTheme

/**
 * Main activity that serves as the entry point for the GPS waypointing application.
 * Handles permission requests and sets up the Compose UI.
 */
class MainActivity : ComponentActivity() {
    private lateinit var locationService: LocationService
    private lateinit var waypointRepository: WaypointRepository
    private val compassState = CompassState()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, can start tracking if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationService = LocationService(this)
        waypointRepository = WaypointRepository(this)

        // Load saved waypoints
        compassState.waypoints = waypointRepository.loadWaypoints()

        // Request location permission if not granted
        if (!locationService.hasLocationPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            GPSWaypointingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        compassState = compassState,
                        locationService = locationService,
                        waypointRepository = waypointRepository
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationService.stopTracking()
    }
}

