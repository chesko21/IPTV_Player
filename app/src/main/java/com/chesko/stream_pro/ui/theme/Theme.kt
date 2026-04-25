package com.chesko.stream_pro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VibrantBlue,
    onPrimary = OffWhiteText,
    primaryContainer = DeepBlue,
    onPrimaryContainer = LightBlue,
    secondary = VibrantBlue,
    onSecondary = OffWhiteText,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorRed,
    onError = OffWhiteText
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantBlue,
    onPrimary = OffWhiteText,
    primaryContainer = LightBlue,
    onPrimaryContainer = DeepBlue,
    secondary = VibrantBlue,
    onSecondary = OffWhiteText,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
    onError = OffWhiteText
)

@Suppress("DEPRECATION")
@Composable
fun IPTV_PlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val colorScheme = if (accentColor != null) {
        baseColorScheme.copy(
            primary = accentColor,
            secondary = accentColor,
            // primaryContainer could also be derived from accentColor if desired
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Set colors for older versions (API < 35) or fallback
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            // Appearance control for light/dark bars
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
