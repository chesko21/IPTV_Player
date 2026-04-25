package com.chesko.stream_pro_tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.*

private val DarkColorScheme = darkColorScheme(
    primary = VibrantBlue,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = DeepBlack,
    surface = DarkGrey,
    onPrimary = GhostWhite,
    onSecondary = GhostWhite,
    onTertiary = GhostWhite,
    onBackground = GhostWhite,
    onSurface = GhostWhite,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IPTV_PlayerTheme(
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (accentColor != null) {
        DarkColorScheme.copy(
            primary = accentColor
        )
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content
    )
}
