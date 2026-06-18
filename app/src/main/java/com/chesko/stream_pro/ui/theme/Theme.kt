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
    primary = PrimaryGold,
    onPrimary = Color.Black,
    primaryContainer = PrimaryGoldDark,
    onPrimaryContainer = PrimaryGoldLight,
    
    secondary = SecondaryGold,
    onSecondary = Color.Black,
    
    tertiary = TertiaryGold,
    onTertiary = PureWhite,
    
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
    primary = LightPrimaryNavy,
    onPrimary = Color.White,
    primaryContainer = LightSurface,
    onPrimaryContainer = LightPrimaryNavy,
    
    secondary = LightPrimaryNavy,
    onSecondary = Color.White,
    
    tertiary = LightPrimaryNavy,
    onTertiary = Color.White,
    
    background = LightBackground,
    onBackground = LightOnSurface,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline,
    
    error = CosmicError,
    onError = LightBackground,
    scrim = ScrimColor
)

@Suppress("DEPRECATION")
@Composable
fun IPTV_PlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color? = null,
    backgroundOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val colorScheme = if (accentColor != null || backgroundOverride != null) {
        val isLightAccent = (accentColor ?: baseColorScheme.primary).luminance() > 0.5f
        baseColorScheme.copy(
            primary = accentColor ?: baseColorScheme.primary,
            onPrimary = if (isLightAccent) Color.Black else Color.White,
            secondary = accentColor ?: baseColorScheme.secondary,
            onSecondary = if (isLightAccent) Color.Black else Color.White,
            background = backgroundOverride ?: baseColorScheme.background,
            primaryContainer = (accentColor ?: baseColorScheme.primary).copy(alpha = 0.15f),
            onPrimaryContainer = if (darkTheme) Color.White else (accentColor ?: baseColorScheme.primary),
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

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            val isLightBackground = colorScheme.background.luminance() > 0.5f
            insetsController.isAppearanceLightStatusBars = isLightBackground
            insetsController.isAppearanceLightNavigationBars = isLightBackground
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
