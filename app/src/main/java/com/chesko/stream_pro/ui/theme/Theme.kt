package com.chesko.stream_pro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = PureWhite,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,
    
    secondary = SecondaryBlue,
    onSecondary = PureWhite,
    
    tertiary = TertiaryCyan,
    onTertiary = CinematicBlack,
    
    background = DarkBackground,
    onBackground = DarkOnSurface,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkOutline,
    
    error = CosmicError,
    onError = PureWhite,
    scrim = ScrimColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = PureWhite,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    
    secondary = SecondaryBlue,
    onSecondary = PureWhite,
    
    tertiary = TertiaryCyan,
    onTertiary = CinematicBlack,
    
    background = LightBackground,
    onBackground = LightOnSurface,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline,
    
    error = CosmicError,
    onError = PureWhite,
    scrim = ScrimColor
)

/**
 * Universal IPTV Player Theme
 * Optimized for cinematic media consumption and interactive browsing
 */
@Suppress("DEPRECATION")
@Composable
fun IPTV_PlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    // Choose base color scheme
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // Apply dynamic branding (Accent Color) with contrast checking
    val colorScheme = if (accentColor != null) {
        val isLightAccent = accentColor.luminance() > 0.5f
        baseColorScheme.copy(
            primary = accentColor,
            onPrimary = if (isLightAccent) Color.Black else Color.White,
            secondary = accentColor,
            onSecondary = if (isLightAccent) Color.Black else Color.White,
            primaryContainer = accentColor.copy(alpha = 0.15f),
            onPrimaryContainer = if (darkTheme) Color.White else accentColor,
            // Ensure surfaces are legible on custom backgrounds
            surface = if (darkTheme) DarkSurface.copy(alpha = 0.9f) else LightSurface.copy(alpha = 0.9f),
            surfaceVariant = if (darkTheme) DarkSurfaceVariant.copy(alpha = 0.7f) else LightSurfaceVariant.copy(alpha = 0.7f)
        )
    } else {
        baseColorScheme.copy(
            surface = if (darkTheme) DarkSurface.copy(alpha = 0.9f) else LightSurface.copy(alpha = 0.9f),
            surfaceVariant = if (darkTheme) DarkSurfaceVariant.copy(alpha = 0.7f) else LightSurfaceVariant.copy(alpha = 0.7f)
        )
    }

    val view = LocalView.current

    // Set System Bar Colors
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Set underlying window color (fallback for older APIs)
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            // Control icon contrast
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
