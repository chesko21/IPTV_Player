package com.chesko.stream_pro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Universal Modern Shimmer Effect
 * Inspired by high-end glassmorphic designs with enhanced cinematic feel
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "universe_shimmer")

    val translateAnim by transition.animateFloat(
        initialValue = -1500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translation"
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
    )

    this
        .graphicsLayer { alpha = pulseAlpha }
        .background(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateAnim - 400f, translateAnim - 600f),
                end = Offset(translateAnim + 400f, translateAnim + 600f)
            )
        )
}

@Composable
fun ShimmerEpgScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shimmerEffect()
                )
            }
        }

        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerPosterItem() {
    Column(modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

@Composable
fun ShimmerContentRow() {
    Column(modifier = Modifier.padding(vertical = 20.dp)) {
        Box(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                .width(160.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .shimmerEffect()
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                ShimmerPosterItem()
            }
        }
    }
}

@Composable
fun ShimmerHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier
            .statusBarsPadding()
            .height(64.dp))

        // Large Modern Hero Shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(32.dp))
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Futuristic Tab/Chip Shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
            }
        }

        repeat(2) {
            ShimmerContentRow()
        }
    }
}
