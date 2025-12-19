package com.zen.pathfinder.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ZenPalette {
    val PaperWhite = Color(0xFFF5F5F0)
    val InkBlack = Color(0xFF1A1A1A)
    val StoneGrey = Color(0xFF757575)
    val BambooGreen = Color(0xFF4E6E58)
    val KoiOrange = Color(0xFFD86C4E)
    val Mist = Color(0xFFE0E0E0)
}

val ZenType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        color = ZenPalette.InkBlack
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 2.sp,
        color = ZenPalette.InkBlack
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp,
        color = ZenPalette.StoneGrey
    )
)

val ZenScheme = lightColorScheme(
    primary = ZenPalette.BambooGreen,
    onPrimary = ZenPalette.PaperWhite,
    background = ZenPalette.PaperWhite,
    onBackground = ZenPalette.InkBlack,
    surface = ZenPalette.PaperWhite,
    onSurface = ZenPalette.InkBlack,
    error = ZenPalette.KoiOrange
)
