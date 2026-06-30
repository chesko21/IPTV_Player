package com.chesko.stream_pro_tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun UniverseBackground(
    primaryColor: Color,
    glowAlpha: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "universe_bg")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing)
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF00020A))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.15f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    w * 0.5f + cos(phase) * w * 0.15f,
                    h * 0.5f + sin(phase) * h * 0.1f
                ),
                radius = w * 0.9f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.12f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    w * 0.2f + sin(phase * 0.5f) * w * 0.2f,
                    h * 0.3f + cos(phase * 0.5f) * h * 0.15f
                ),
                radius = w * 0.7f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.1f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    w * 0.8f + cos(phase * 0.7f) * w * 0.1f,
                    h * 0.7f + sin(phase * 0.7f) * h * 0.2f
                ),
                radius = w * 0.8f
            )
        )
    }
}
