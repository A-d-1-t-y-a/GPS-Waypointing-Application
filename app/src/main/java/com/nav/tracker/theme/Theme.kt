package com.nav.tracker.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NavScheme = darkColorScheme(
    primary = NavPalette.CoreCyan,
    background = NavPalette.VoidBlack,
    surface = NavPalette.VoidGrey,
    error = NavPalette.SysError,
    onPrimary = Color.Black,
    onBackground = NavPalette.TextMain,
    onSurface = NavPalette.TextMain
)

@Composable
fun NavTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavPalette.VoidBlack.toArgb()
            window.navigationBarColor = NavPalette.VoidBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = NavScheme,
        typography = NavTypography,
        content = content
    )
}
