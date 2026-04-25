package com.chesko.stream_pro_tv.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFavoritesScreen(
    viewModel: MainViewModel,
    onChannelClick: (IptvChannel) -> Unit,
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmall = screenWidth < 600.dp
    
    val columns = when {
        screenWidth < 600.dp -> 3
        screenWidth < 1240.dp -> 4
        else -> 6
    }

    val favorites by viewModel.favoriteChannels.collectAsState()

    BackHandler {
        onBack()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE50914).copy(alpha = 0.15f), Color.Transparent),
                    center = center.copy(x = center.x + (bgOffset % 1000 - 500), y = center.y + (bgOffset % 800 - 400)),
                    radius = size.minDimension * 1.5f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2979FF).copy(alpha = 0.12f), Color.Transparent),
                    center = center.copy(x = center.x - (bgOffset % 1200 - 600), y = center.y - (bgOffset % 600 - 300)),
                    radius = size.minDimension * 1.2f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isSmall) 16.dp else 48.dp,
                    vertical = if (isSmall) 16.dp else 32.dp
                )
        ) {
            Text(
                text = "FAVORITES",
                style = if (isSmall) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = if (isSmall) 2.sp else 4.sp
            )

            Spacer(modifier = Modifier.height(if (isSmall) 16.dp else 32.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada saluran favorit",
                            style = if (isSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    TvChannelGrid(
                        channels = favorites,
                        onChannelClick = onChannelClick,
                        columns = columns
                    )
                }
            }
        }
    }
}
