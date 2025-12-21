package com.vintagenav.explorer

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.vintagenav.explorer.data.LocationStorageManager
import com.vintagenav.explorer.service.GPSMonitor
import com.vintagenav.explorer.service.OrientationProvider
import com.vintagenav.explorer.theme.ExpeditionTheme

/**
 * Entry point for the Vintage Navigator Application.
 */
class MainActivity : ComponentActivity() {

    private lateinit var locationTracker: GPSMonitor
    private lateinit var compassSensor: OrientationProvider
    private lateinit var dataStore: LocationStorageManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> 
        // Logic handled in composable state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Managers
        locationTracker = GPSMonitor(this)
        compassSensor = OrientationProvider(this)
        dataStore = LocationStorageManager(this)

        if (!locationTracker.checkPerms()) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            ExpeditionTheme {
                ExpeditionDashboard(
                    locationTracker = locationTracker,
                    compassSensor = compassSensor,
                    dataStore = dataStore
                )
            }
        }
    }
}
