package com.nav.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.nav.tracker.data.BeaconRegistry
import com.nav.tracker.service.GyroscopeMonitor
import com.nav.tracker.service.PositionMonitor
import com.nav.tracker.theme.NavTheme

/**
 * Main Entry point of the Application.
 * Initializes core services (Position, Gyroscope, Registry) and sets up the UI.
 */
class MainActivity : ComponentActivity() {
    private lateinit var pMon: PositionMonitor
    private lateinit var gMon: GyroscopeMonitor
    private lateinit var reg: BeaconRegistry

    /**
     * Activity Result Launcher for requesting permissions.
     */
    private val perms = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /**
     * Called when the activity is starting.
     * Instantiates services and checks permissions.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        pMon = PositionMonitor(this)
        gMon = GyroscopeMonitor(this)
        reg = BeaconRegistry(this)

        if (!pMon.hasPerms()) {
            perms.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            NavTheme {
                HudInterface(pMon, gMon, reg)
            }
        }
    }
}
