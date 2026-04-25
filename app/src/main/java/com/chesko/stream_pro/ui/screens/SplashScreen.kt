package com.chesko.stream_pro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesko.stream_pro.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNextScreen: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    
    val bgOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset1"
    )

    val bgOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset2"
    )

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val logoRotate = remember { Animatable(-20f) }
    val glowAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }
        launch {
            alpha.animateTo(1f, tween(1200, easing = EaseOutQuart))
        }
        launch {
            logoRotate.animateTo(0f, tween(1800, easing = EaseOutBack))
        }
        launch {
            delay(400)
            glowAlpha.animateTo(1f, tween(2000, easing = LinearEasing))
        }
        launch {
            delay(1000)
            textAlpha.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        }
        
        delay(5000)
        onNextScreen()
    }

    // Standardized Branding Sizes (Uniform across all devices)
    val logoSize = 110.dp
    val logoPadding = 22.dp
    val titleSize = 48.sp
    val subtitleSize = 11.sp
    val subtitleSpacing = 4.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // 1. DYNAMIC AMBIENT BACKGROUND
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            // First Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.12f * glowAlpha.value), Color.Transparent)
                ),
                radius = size.maxDimension * 0.9f,
                center = center.copy(
                    x = center.x + (bgOffset1 * 200) - 100,
                    y = center.y + (bgOffset2 * 100) - 50
                )
            )
            // Second Glow (Opposite)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.08f * glowAlpha.value), Color.Transparent)
                ),
                radius = size.maxDimension * 0.7f,
                center = center.copy(
                    x = center.x - (bgOffset2 * 150) + 75,
                    y = center.y - (bgOffset1 * 150) + 75
                )
            )
        }

        // 2. MAIN CONTENT (Logo + Branding + Loading)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Logo Container with Outer Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    alpha = alpha.value,
                    rotationZ = logoRotate.value
                )
            ) {
                // Pulsing Logo Glow
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.5f,
                    targetValue = 2.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Canvas(modifier = Modifier.size(logoSize * pulseScale)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.15f * glowAlpha.value), Color.Transparent)
                        ),
                        radius = size.minDimension / 2
                    )
                }

                // The actual Logo Surface
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.3f))),
                            CircleShape
                        )
                        .padding(logoPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_android),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer(alpha = textAlpha.value)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STREAM",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = titleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = titleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp
                        ),
                        color = primaryColor
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "THE ULTIMATE TV EXPERIENCE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = subtitleSpacing,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 3. LOADING INDICATOR
            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .graphicsLayer(alpha = textAlpha.value),
                color = primaryColor,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )
        }

        // 4. FOOTER (Credit)
        Text(
            text = "BY CHESKO",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer(alpha = textAlpha.value),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            letterSpacing = 6.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
