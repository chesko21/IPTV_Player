package com.chesko.stream_pro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal Cosmic Loading State
 * A high-end cinematic loading experience
 */
@Composable
fun LoadingState(message: String = "Menjelajahi Semesta...") {
    val infiniteTransition = rememberInfiniteTransition(label = "universe_loading")

    // Background cosmic pulse
    val bgPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f * bgPulse),
                        Color(0xFF020205) // Deep Space Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CosmicOrbitalLoader()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Glowing Breathing Text
            val textAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "text_glow"
            )

            Text(
                text = message.uppercase(),
                color = Color.White,
                letterSpacing = 4.sp,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .graphicsLayer { alpha = textAlpha }
            )
        }
    }
}

@Composable
fun CosmicOrbitalLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    
    // Rotation of different orbits
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "inner"
    )

    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing)
        ),
        label = "outer"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Outer Glowing Ring
            drawArc(
                color = primaryColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic Outer Orbit
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.5f to primaryColor,
                    1f to Color.Transparent
                ),
                startAngle = rotationOuter,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner Pulsing Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor, Color.Transparent)
                ),
                radius = 12.dp.toPx()
            )

            // Orbital Particles
            val radius = 40.dp.toPx()
            val angleRad = Math.toRadians(rotationInner.toDouble())
            val particlePos = Offset(
                x = center.x + radius * cos(angleRad).toFloat(),
                y = center.y + radius * sin(angleRad).toFloat()
            )
            
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = particlePos
            )
            
            // Particle Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.6f), Color.Transparent)
                ),
                radius = 8.dp.toPx(),
                center = particlePos
            )
        }
    }
}
