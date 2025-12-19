package com.vintagenav.explorer.theme

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
private val ExpeditionScheme = lightColorScheme(
    primary = ExplorerPalette.DeepBlue,
    onPrimary = ExplorerPalette.MapCream,
    primaryContainer = ExplorerPalette.MapBeige,
    onPrimaryContainer = ExplorerPalette.DeepBlue,
    secondary = ExplorerPalette.SaddleBrown,
    onSecondary = ExplorerPalette.MapCream,
    tertiary = ExplorerPalette.RustOrange,
    onTertiary = ExplorerPalette.MapCream,
    background = ExplorerPalette.MapCream,
    onBackground = ExplorerPalette.Charcoal,
    surface = ExplorerPalette.MapCream,
    onSurface = ExplorerPalette.Charcoal,
    error = ExplorerPalette.Crimson,
    onError = ExplorerPalette.MapCream,
    surfaceVariant = ExplorerPalette.MapBeige,
    onSurfaceVariant = ExplorerPalette.SlateGray
)

/**
 * Custom theme for the Friend's GPS app.
 */
@Composable
fun ExpeditionTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ExpeditionScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ExplorerPalette.MapBeige.toArgb()
            window.navigationBarColor = ExplorerPalette.MapBeige.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LogbookFont,
        content = content
    )
}
