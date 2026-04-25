package com.chesko.stream_pro_tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.2f),
        Color.White.copy(alpha = 0.05f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim.value - 600f, y = translateAnim.value - 600f),
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    background(brush)
}

@Composable
fun ShimmerHomeScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Gunakan background yang sama persis dengan HomeScreen
        PremiumTvBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            // Shimmer untuk Group Selector
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(44.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .shimmerEffect()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shimmer untuk Channel Grid (6 Kolom agar sinkron)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) { // Tampilkan 3 baris shimmer
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) { // 6 Kolom
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }
    }
}
