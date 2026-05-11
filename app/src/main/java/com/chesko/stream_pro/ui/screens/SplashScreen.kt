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
fun SplashScreen(
    viewModel: com.chesko.stream_pro.core.ui.MainViewModel,
    onNextScreen: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic")

    // Background animation
    val nebulaRotate by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "nebula"
    )

    val cosmicPulse by infiniteTransition.animateFloat(
        0.8f, 1.2f,
        infiniteRepeatable(
            tween(4000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // NEW: starfield parallax
    val starOffset by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "starMove"
    )

    // Animations
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-10f) }
    val contentAlpha = remember { Animatable(0f) }
    val loadingProgress = remember { Animatable(0.0f) }

    LaunchedEffect(Unit) {
        launch {
            delay(300)
            logoScale.animateTo(1f, spring(dampingRatio = 0.6f))
        }
        launch {
            delay(300)
            logoRotation.animateTo(0f, spring(stiffness = 200f))
        }
        launch { delay(300); logoAlpha.animateTo(1f, tween(800)) }

        launch {
            delay(900)
            contentAlpha.animateTo(1f, tween(800))
        }

        launch {
            delay(1200)
            loadingProgress.animateTo(1f, tween(2000))
        }

        delay(3000)
        val currentPlaylist = viewModel.lastUrl.value
        onNextScreen(if (currentPlaylist.isNotEmpty()) "home" else "login")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        // Nebula
        Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
            val angleRad = Math.toRadians(nebulaRotate.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent)
                ),
                radius = size.maxDimension * 0.5f * cosmicPulse,
                center = Offset(
                    size.width / 2 + (cos(angleRad) * 150).toFloat(),
                    size.height / 2 + (sin(angleRad) * 100).toFloat()
                )
            )
        }

        // UPDATED Starfield
        MinimalStarField(starOffset)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            // LOGO
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                    rotationZ = logoRotation.value
                }
            ) {

                // Glow aura
                Box(
                    modifier = Modifier
                        .size(110.dp * cosmicPulse)
                        .background(
                            Brush.radialGradient(
                                listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.5.dp, primaryColor.copy(alpha = 0.6f), CircleShape)
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

            // TEXT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.brand_name).substringBefore("PRO"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
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
                    letterSpacing = 4.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // LOADING
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { alpha = contentAlpha.value }
                    .width(160.dp)
            ) {

                Text(
                    text = "${(loadingProgress.value * 100).toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // IMPROVED PROGRESS BAR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loadingProgress.value)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        primaryColor,
                                        primaryColor.copy(alpha = 0.5f)
                                    )
                                ),
                                CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.splash_initializing),
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = primaryColor.copy(alpha = 0.7f)
                )
            }
        }

        // FOOTER
        Text(
            text = stringResource(R.string.splash_designed_by),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .graphicsLayer { alpha = contentAlpha.value * 0.3f },
            fontSize = 7.sp,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun MinimalStarField(offset: Float) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "twinkle")

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

    // ✅ Buat list animasi di luar Canvas
    val twinkles = stars.map { star ->
        infiniteTransition.animateFloat(
            initialValue = star.alpha,
            targetValue = star.alpha + 0.3f,
            animationSpec = infiniteRepeatable(
                tween(2000, easing = LinearEasing),
                RepeatMode.Reverse
            ),
            label = "twinkle"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { index, star ->
            val twinkle = twinkles[index].value

            drawCircle(
                color = Color.White.copy(alpha = twinkle),
                radius = with(density) { star.size.dp.toPx() },
                center = Offset(
                    star.x * size.width,
                    ((star.y + offset * 0.05f) % 1f) * size.height
                )
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