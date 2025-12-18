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
import com.example.gpswaypointing.data.WaypointRepository
import com.example.gpswaypointing.service.LocationService
import com.example.gpswaypointing.service.SensorService

/**
 * Main activity - simplified entry point that sets up services and UI.
 */
class MainActivity : ComponentActivity() {
    private lateinit var locationService: LocationService
    private lateinit var sensorService: SensorService
    private lateinit var waypointRepository: WaypointRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize services
        locationService = LocationService(this)
        sensorService = SensorService(this)
        waypointRepository = WaypointRepository(this)

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
                    CompassScreen(
                        locationService = locationService,
                        sensorService = sensorService,
                        waypointRepository = waypointRepository
                    )
                }
            }
        }
    }
}

