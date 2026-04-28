package com.chesko.stream_pro_tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.*

private val UniverseColorScheme = darkColorScheme(
    primary = UniversePrimary,
    onPrimary = Color.Black,
    secondary = UniverseSecondary,
    onSecondary = Color.Black,
    tertiary = UniverseTertiary,
    onTertiary = Color.White,
    background = UniverseBackground,
    onBackground = StarlightWhite,
    surface = UniverseSurface,
    onSurface = StarlightWhite,
    surfaceVariant = UniverseSurfaceVariant,
    onSurfaceVariant = StarlightWhite.copy(alpha = 0.7f),
    error = GalacticError,
    onError = Color.Black
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IPTV_PlayerTheme(
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (accentColor != null) {
        UniverseColorScheme.copy(
            primary = accentColor
        )
    } else {
        UniverseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content
    )
}
