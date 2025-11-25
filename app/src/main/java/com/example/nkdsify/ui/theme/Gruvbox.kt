package com.example.nkdsify.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Gruvbox Palette ---

// Dark Backgrounds
val GbDarkBgHard = Color(0xFF1D2021)
val GbDarkBg     = Color(0xFF282828)
val GbDarkBgSoft = Color(0xFF32302F)

// Dark Foreground (Text)
val GbDarkFg     = Color(0xFFEBDBB2)
val GbDarkGray   = Color(0xFFA89984)

// Bright Colors (Хорошо смотрятся на темном фоне)
val GbRed        = Color(0xFFFB4934)
val GbGreen      = Color(0xFFB8BB26)
val GbYellow     = Color(0xFFFABD2F)
val GbBlue       = Color(0xFF83A598)
val GbPurple     = Color(0xFFD3869B)
val GbAqua       = Color(0xFF8EC07C)
val GbOrange     = Color(0xFFFE8019)

// Neutral Colors (Для рамок, разделителей)
val GbGrayRed    = Color(0xFF9D0006) // Темно-красный
val GbGrayOrange = Color(0xFFAF3A03) // Темно-оранжевый

// For the light theme, we will generate a simplified version as the user did not provide a full light palette.
val GbLightBg = Color(0xFFFBF1C7)
val GbLightFg = Color(0xFF3C3836)


val DarkGruvboxColorScheme = darkColorScheme(
    primary = GbAqua,
    secondary = GbYellow,
    tertiary = GbPurple,
    background = GbDarkBg,
    surface = GbDarkBgSoft, // Softer surface color
    onPrimary = GbDarkBgHard, // High contrast for text on primary
    onSecondary = GbDarkBgHard,
    onTertiary = GbDarkBgHard,
    onBackground = GbDarkFg,
    onSurface = GbDarkFg, 
    onSurfaceVariant = GbDarkGray, // For less prominent text
    error = GbRed,
    onError = GbDarkBg
)

val LightGruvboxColorScheme = lightColorScheme(
    primary = Color(0xFF427B58), // A darker Aqua for better readability
    secondary = Color(0xFFB57614), // Darker Yellow
    tertiary = Color(0xFF8F3F71), // Darker Purple
    background = GbLightBg,
    surface = Color(0xFFF2E5BC), // Slightly darker surface
    onPrimary = GbLightBg,
    onSecondary = GbLightFg,
    onTertiary = GbLightBg,
    onBackground = GbLightFg,
    onSurface = GbLightFg,
    onSurfaceVariant = Color(0xFF504945), // A bit darker gray for text
    error = GbGrayRed,
    onError = GbLightBg
)
