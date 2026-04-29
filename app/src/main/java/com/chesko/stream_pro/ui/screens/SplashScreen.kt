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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro.R
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SplashScreen(viewModel: com.chesko.stream_pro.core.ui.MainViewModel, onNextScreen: (String) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic")

    // Simplified cosmic background
    val nebulaRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "nebula"
    )

    val cosmicPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // Minimal entrance animations
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val loadingProgress = remember { Animatable(0.0f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.6f)) }
        launch { logoAlpha.animateTo(1f, tween(1000)) }
        launch { delay(500); contentAlpha.animateTo(1f, tween(1000)) }
        launch { delay(800); loadingProgress.animateTo(1f, tween(2500)) }

        delay(3500)
        val currentPlaylist = viewModel.lastUrl.value
        onNextScreen(if (currentPlaylist.isNotEmpty()) "home" else "login")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020205)),
        contentAlignment = Alignment.Center
    ) {
        // Simplified Nebula Background
        Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
            val angleRad = Math.toRadians(nebulaRotate.toDouble())
            val centerX = size.width / 2
            val centerY = size.height / 2

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent)
                ),
                radius = size.maxDimension * 0.5f * cosmicPulse,
                center = Offset(
                    centerX + (cos(angleRad) * 150).toFloat(),
                    centerY + (sin(angleRad) * 100).toFloat()
                )
            )
        }

        // Minimal Starfield
        MinimalStarField()

        // Main Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
            ) {
                // Simple Aura
                Box(
                    modifier = Modifier
                        .size(100.dp * cosmicPulse)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.1f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                // Logo Container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D0D14))
                        .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_android),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Minimal Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.brand_name).substringBefore("PRO"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "PRO",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.shimmerEffect()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.splash_universe_subtitle),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Minimal Loading Indicator with Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { alpha = contentAlpha.value }
                    .width(160.dp)
            ) {

                // Simple Percentage
                Text(
                    text = "${(loadingProgress.value * 100).toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Minimal Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loadingProgress.value)
                            .fillMaxHeight()
                            .background(primaryColor, CircleShape)
                    )
                }
                // Loading Text (Minimal)
                Text(
                    text = stringResource(R.string.splash_initializing),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = primaryColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

            }
        }

        // Minimal Footer
        Text(
            text = stringResource(R.string.splash_designed_by),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .graphicsLayer { alpha = contentAlpha.value * 0.3f },
            fontSize = 7.sp,
            letterSpacing = 4.sp,
            color = Color.White
        )
    }
}

@Composable
fun MinimalStarField() {
    val density = LocalDensity.current
    val stars = remember {
        List(80) {
            StarMinimal(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 1.5f + 0.5f,
                alpha = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = with(density) { star.size.dp.toPx() },
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}

data class StarMinimal(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float
)