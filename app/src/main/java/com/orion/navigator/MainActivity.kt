package com.orion.navigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.orion.navigator.data.BeaconRegistry
import com.orion.navigator.service.GyroscopeMonitor
import com.orion.navigator.service.PositionMonitor
import com.orion.navigator.theme.OrionTheme

class MainActivity : ComponentActivity() {
    private lateinit var pMon: PositionMonitor
    private lateinit var gMon: GyroscopeMonitor
    private lateinit var reg: BeaconRegistry

    private val perms = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        pMon = PositionMonitor(this)
        gMon = GyroscopeMonitor(this)
        reg = BeaconRegistry(this)

        if (!pMon.hasPerms()) {
            perms.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            OrionTheme {
                HudInterface(pMon, gMon, reg)
            }
        }
    }
}
