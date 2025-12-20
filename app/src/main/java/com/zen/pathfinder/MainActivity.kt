package com.zen.pathfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val gpsManager = GPSManager(this)
        val compassSensor = CompassSensor(this)
        val fileManager = WaypointFileManager(this)

        // Check permissions immediately or let UI handle?
        // "app will first check the availability of these permissions before performing GPS operations"
        // I'll launch permission request here.
        if (!gpsManager.hasLocationPermission(this)) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            GPSWaypointTheme {
                // Factory for ViewModel with arguments
                val viewModel: AppViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AppViewModel(fileManager) as T
                        }
                    }
                )
                
                WaypointApp(viewModel, gpsManager, compassSensor)
            }
        }
    }
}
