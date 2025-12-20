package com.zen.pathfinder

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

@Composable
fun ZenPathfinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        // Dark Golden Theme
        darkColorScheme(
            primary = Color(0xFFD4AF37),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFFB8860B),
            onPrimaryContainer = Color(0xFFFFF8DC),
            secondary = Color(0xFFFFD700),
            onSecondary = Color(0xFF1A1A1A),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE0E0E0)
        )
    } else {
        // Light Golden Theme
        lightColorScheme(
            primary = Color(0xFFD4AF37),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFFFFF8DC),
            onPrimaryContainer = Color(0xFF8B4513),
            secondary = Color(0xFFFFD700),
            onSecondary = Color(0xFF1A1A1A),
            background = Color(0xFFFFFBF0),
            onBackground = Color(0xFF3D3D3D),
            surface = Color(0xFFFFF8DC),
            onSurface = Color(0xFF3D3D3D)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
