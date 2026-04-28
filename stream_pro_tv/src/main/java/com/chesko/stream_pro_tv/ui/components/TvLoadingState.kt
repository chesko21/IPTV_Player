package com.chesko.stream_pro_tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.chesko.stream_pro_tv.ui.screens.UniverseBackground

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingState(message: String = "Memuat Saluran...") {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Universe Background
        UniverseBackground(
            primaryColor = MaterialTheme.colorScheme.primary,
            glowAlpha = glowAlpha
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cosmic Loading Indicator
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    // Outer glow circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFBB86FC).copy(alpha = 0.3f * glowAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 1.5f
                    )
                }
                
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = message.uppercase(),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFBB86FC).copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }
    }
}
