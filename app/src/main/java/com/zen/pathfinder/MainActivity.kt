package com.zen.pathfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.zen.pathfinder.mind.StoneArchive
import com.zen.pathfinder.senses.CompassSpirit
import com.zen.pathfinder.senses.PathSense
import com.zen.pathfinder.expression.ZenCanvas
import com.zen.pathfinder.expression.theme.ZenScheme
import com.zen.pathfinder.expression.theme.ZenType

class MainActivity : ComponentActivity() {
    private val req = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sense = PathSense(this)
        val spirit = CompassSpirit(this)
        val store = StoneArchive(this)
        
        if (!sense.allowed(this)) {
            req.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        setContent {
            androidx.compose.material3.MaterialTheme(
                colorScheme = ZenScheme,
                typography = ZenType
            ) {
                ZenCanvas(sense, spirit, store)
            }
        }
    }
}
