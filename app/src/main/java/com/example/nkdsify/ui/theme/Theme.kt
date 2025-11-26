package com.example.nkdsify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.nkdsify.data.AppFontFamily
import com.example.nkdsify.data.GoogleSansFontFamily
import com.example.nkdsify.data.JetBrainsMonoFontFamily
import com.example.nkdsify.data.Theme

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val AmoledColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerHigh = Color.Black,
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    onBackground = Color.White,
    onSurface = Color.White
)


@Suppress("DEPRECATION")
@Composable
fun NkdsifyAppTheme(
    theme: Theme = Theme.SYSTEM,
    appFontFamily: AppFontFamily = AppFontFamily.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDarkTheme = when (theme) {
        Theme.SYSTEM -> isSystemInDarkTheme()
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.AMOLED -> true // AMOLED is always dark
    }

    val colors = when {
        theme == Theme.AMOLED -> AmoledColorScheme
        dynamicColor && useDarkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !useDarkTheme -> dynamicLightColorScheme(context)
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }
    val typography = when (appFontFamily) {
        AppFontFamily.JETBRAINS_MONO -> {
            val defaultTypography = Typography()
            Typography(
                displayLarge = defaultTypography.displayLarge.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.displayLarge.fontSize.value - 1).sp),
                displayMedium = defaultTypography.displayMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.displayMedium.fontSize.value - 1).sp),
                displaySmall = defaultTypography.displaySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.displaySmall.fontSize.value - 1).sp),
                headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.headlineLarge.fontSize.value - 1).sp),
                headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.headlineMedium.fontSize.value - 1).sp),
                headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.headlineSmall.fontSize.value - 1).sp),
                titleLarge = defaultTypography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.titleLarge.fontSize.value - 1).sp),
                titleMedium = defaultTypography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.titleMedium.fontSize.value - 1).sp),
                titleSmall = defaultTypography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.titleSmall.fontSize.value - 1).sp),
                bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.bodyLarge.fontSize.value - 1).sp),
                bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.bodyMedium.fontSize.value - 1).sp),
                bodySmall = defaultTypography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.bodySmall.fontSize.value - 1).sp),
                labelLarge = defaultTypography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.labelLarge.fontSize.value - 1).sp),
                labelMedium = defaultTypography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.labelMedium.fontSize.value - 1).sp),
                labelSmall = defaultTypography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = (defaultTypography.labelSmall.fontSize.value - 1).sp)
            )
        }
        AppFontFamily.GOOGLE_SANS -> {
            val defaultTypography = Typography()
            Typography(
                displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSansFontFamily),
                displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSansFontFamily),
                displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSansFontFamily),
                headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSansFontFamily),
                headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSansFontFamily),
                headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSansFontFamily),
                titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSansFontFamily),
                titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSansFontFamily),
                titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSansFontFamily),
                bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSansFontFamily),
                bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSansFontFamily),
                bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSansFontFamily),
                labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSansFontFamily),
                labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSansFontFamily),
                labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSansFontFamily)
            )
        }

        else -> Typography()
    }

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content
    )
}
