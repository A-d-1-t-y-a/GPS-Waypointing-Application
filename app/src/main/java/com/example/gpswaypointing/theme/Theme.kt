package com.example.gpswaypointing.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Vintage Explorer Theme.
 * strictly Light Mode to resemble physical map.
 */
private val VintageScheme = lightColorScheme(
    primary = VintageColors.InkBlue,
    onPrimary = VintageColors.Parchment,
    primaryContainer = VintageColors.ParchmentDark,
    onPrimaryContainer = VintageColors.InkBlue,
    secondary = VintageColors.LeatherBrown,
    onSecondary = VintageColors.Parchment,
    tertiary = VintageColors.BurntOrange,
    onTertiary = VintageColors.Parchment,
    background = VintageColors.Parchment,
    onBackground = VintageColors.InkBlack,
    surface = VintageColors.Parchment,
    onSurface = VintageColors.InkBlack,
    error = VintageColors.OldRed,
    onError = VintageColors.Parchment,
    surfaceVariant = VintageColors.ParchmentDark,
    onSurfaceVariant = VintageColors.FadedInk
)

/**
 * Custom theme for the Friend's GPS app.
 */
@Composable
fun VintageMapTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = VintageScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VintageColors.ParchmentDark.toArgb()
            window.navigationBarColor = VintageColors.ParchmentDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VintageTypography,
        content = content
    )
}
