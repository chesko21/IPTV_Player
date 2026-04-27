package com.chesko.stream_pro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal Cinematic SplashScreen
 * Implements the "Universe" aesthetic with cosmic depth and orbital animations
 */
@Composable
fun SplashScreen(viewModel: com.chesko.stream_pro.core.ui.MainViewModel, onNextScreen: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "universe_ambient")
    
    // Cosmic Background Motion
    val bgOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebula_1"
    )

    val bgOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebula_2"
    )

    // Cinematic Entrance Animations
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-30f) }
    val loadingProgress = remember { Animatable(0.1f) } // Start at 10%
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    LaunchedEffect(Unit) {
        launch {
            // Overshoot spring for cinematic pop
            logoScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
        launch {
            logoAlpha.animateTo(1f, tween(1500, easing = EaseOutQuart))
        }
        launch {
            logoRotation.animateTo(0f, tween(2000, easing = EaseOutBack))
        }
        launch {
            delay(800)
            contentAlpha.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        }
        launch {
            delay(1000)
            // Animate loading from 10% to 100%
            loadingProgress.animateTo(1f, tween(2500, easing = LinearOutSlowInEasing))
        }
        
        // Navigation Logic
        delay(4000) // Adjusted delay to sync with progress
        val currentPlaylist = viewModel.lastUrl.value
        if (currentPlaylist.isNotEmpty()) {
            onNextScreen("home")
        } else {
            onNextScreen("login")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020205)), // Deep Space Black
        contentAlignment = Alignment.Center
    ) {
        // 1. COSMIC LAYERS (Nebulas & Stars)
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
            // Primary Nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent)
                ),
                radius = size.maxDimension * 0.8f,
                center = center.copy(
                    x = center.x + (bgOffset1 * 300) - 150,
                    y = center.y + (bgOffset2 * 150) - 75
                )
            )
            // Secondary Nebula (Cyan/Tertiary)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.1f), Color.Transparent)
                ),
                radius = size.maxDimension * 0.6f,
                center = center.copy(
                    x = center.x - (bgOffset2 * 250) + 125,
                    y = center.y - (bgOffset1 * 200) + 100
                )
            )
        }

        // Starfield Particles
        StarField(starAlpha)

        // 2. MAIN LOGO COMPONENT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Logo with Atmospheric Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                    rotationZ = logoRotation.value
                }
            ) {
                // Pulsing Aura
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.2f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logo_aura"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp * pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                // High-End Logo Surface
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D0D14))
                        .border(
                            1.5.dp,
                            Brush.sweepGradient(
                                listOf(primaryColor, Color.Transparent, primaryColor)
                            ),
                            CircleShape
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_android),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STREAM",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = primaryColor,
                        modifier = Modifier.shimmerEffect() // Adding universe shimmer to PRO
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Universe Subtitle
                Text(
                    text = "EXPLORE THE DIGITAL UNIVERSE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Percentage Loading Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { alpha = contentAlpha.value }
                    .width(200.dp)
            ) {
                // Glow behind percentage
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${(loadingProgress.value * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sleek Cosmic Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loadingProgress.value)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(primaryColor.copy(alpha = 0.5f), primaryColor)
                                ),
                                CircleShape
                            )
                            // Progress Glow
                            .blur(if (loadingProgress.value > 0) 4.dp else 0.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "INITIALIZING SYSTEMS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        fontSize = 8.sp
                    ),
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }
        }

        // 3. FOOTER
        Text(
            text = "DESIGNED BY CHESKO",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = contentAlpha.value * 0.5f },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            letterSpacing = 8.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StarField(alpha: Float) {
    val starCount = 40
    val starCoords = remember {
        List(starCount) {
            Pair(Math.random().toFloat(), Math.random().toFloat())
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        starCoords.forEach { (x, y) ->
            drawCircle(
                color = Color.White.copy(alpha = alpha * (0.2f + (Math.random().toFloat() * 0.8f))),
                radius = 1.dp.toPx(),
                center = center.copy(
                    x = x * size.width,
                    y = y * size.height
                )
            )
        }
    }
}
