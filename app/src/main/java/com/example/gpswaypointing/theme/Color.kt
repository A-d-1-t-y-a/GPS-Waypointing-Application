package com.example.gpswaypointing.theme

import androidx.compose.ui.graphics.Color

/**
 * Sci-Fi HUD Color Palette.
 * High contrast neon colors on deep dark backgrounds.
 */
object AppColors {
    // Deep Space Backgrounds
    val Background = Color(0xFF050B14) // Almost black blue
    val Surface = Color(0xFF0F1623)    // Dark blue-grey
    val SurfaceDark = Color(0xFF0A0F18)
    
    // Neon Accents
    val Primary = Color(0xFF00F0FF)      // Cyan Neon
    val PrimaryDark = Color(0xFF00B8D4)  // Darker Cyan
    val Secondary = Color(0xFFD500F9)    // Electric Purple
    val Accent = Color(0xFFFF2A6D)       // Neon Pink/Red
    
    // Functional Colors
    val Success = Color(0xFF00FF9D)      // Matrix Green
    val Error = Color(0xFFFF003C)        // Cyberpunk Red
    val Warning = Color(0xFFFFD600)      // Neon Amber
    
    // Compass Specific
    val CompassBackground = Color(0xFF0F1623)
    val CompassRing = Color(0xFF00F0FF)  // Cyan rings
    val CompassGrid = Color(0xFF00F0FF)  // Cyan grid
    
    val NorthColor = Color(0xFFFF003C)         // Red North
    val WaypointColor = Color(0xFF00F0FF)      // Cyan Blip
    val SelectedWaypointColor = Color(0xFFFFD600) // Amber Target
    val NavigationArrow = Color(0xFF00FF9D)    // Green Arrow
    
    // Text
    val TextPrimary = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFFA0A0A0)
}
