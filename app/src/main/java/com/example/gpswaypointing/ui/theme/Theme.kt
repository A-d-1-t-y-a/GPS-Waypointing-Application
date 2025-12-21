package com.example.gpswaypointing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SciFiPrimary,
    secondary = SciFiSecondary,
    tertiary = MatrixGreen,
    background = SciFiBackground,
    surface = SciFiSurface,
    onPrimary = SciFiOnPrimary,
    onSecondary = SciFiOnPrimary, // Black text on neon
    onTertiary = SciFiOnPrimary,
    onBackground = SciFiOnBackground,
    onSurface = SciFiOnBackground
)

@Composable
fun GPSWaypointingTheme(
    darkTheme: Boolean = true, // Force dark theme for Sci-Fi look
    // internal parameters can be unused as we force specific look
    dynamicColor: Boolean = false, // Disable dynamic color to enforce theme
    content: @Composable () -> Unit
) {
    // We strictly use the custom DarkColorScheme
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

