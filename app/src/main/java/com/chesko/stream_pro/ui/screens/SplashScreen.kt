package com.chesko.stream_pro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

data class PremiumParticle(
    var x: Float,
    var y: Float,
    var speed: Float,
    var radius: Float,
    var alpha: Float,
    var phase: Float
)

@Composable
fun RotatingRing(
    primaryColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()

        drawCircle(
            color = primaryColor.copy(alpha = 0.1f),
            style = Stroke(width = 0.5.dp.toPx())
        )

        rotate(rotation) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.8f * progress),
                        Color.White.copy(alpha = 0.9f * progress),
                        primaryColor.copy(alpha = 0.8f * progress),
                        Color.Transparent
                    )
                ),
                startAngle = -90f,
                sweepAngle = 180f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val angleRad = Math.toRadians(-90.0 + 180.0 * progress).toFloat()
            val r = size.width / 2
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(center.x + r * cos(angleRad), center.y + r * sin(angleRad))
            )
        }
    }
}

@Composable
fun CinematicBackground(
    intensity: Float,
    time: Float
) {
    val particles = remember {
        List(40) {
            PremiumParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.0015f + 0.0005f,
                radius = Random.nextFloat() * 2f + 0.5f,
                alpha = Random.nextFloat() * 0.4f + 0.1f,
                phase = Random.nextFloat() * PI.toFloat() * 2f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF000208))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E1B4B).copy(alpha = 0.08f * intensity), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.2f),
                radius = w * 0.8f
            )
        )

        particles.forEach { p ->
            p.y -= p.speed
            if (p.y < -0.05f) p.y = 1.05f
            
            val pAlpha = p.alpha * intensity * (0.5f + 0.5f * sin(time * 1.5f + p.phase))
            if (pAlpha > 0.02f) {
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    radius = p.radius,
                    center = Offset(
                        (p.x + sin(time + p.phase) * 0.01f) * w,
                        p.y * h
                    )
                )
            }
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f * intensity)),
                center = center,
                radius = w * 1.0f
            )
        )
    }
}

@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    onNextScreen: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val barProgress = remember { Animatable(0f) }
    val titleY = remember { Animatable(30f) }
    val time = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            time.animateTo(100f, infiniteRepeatable(tween(100000, easing = LinearEasing)))
        }

        launch {
            logoAlpha.animateTo(1f, tween(1200, easing = EaseOutExpo))
        }
        launch {
            logoScale.animateTo(1.1f, tween(1000, easing = EaseOutBack))
            logoScale.animateTo(1f, tween(400, easing = EaseInOutSine))
        }
        
        delay(800)
        
        launch {
            contentAlpha.animateTo(1f, tween(1500, easing = EaseOutQuart))
            titleY.animateTo(0f, tween(1000, easing = EaseOutCubic))
        }

        delay(500)
        barProgress.animateTo(1f, tween(4500, easing = EaseInOutQuart))

        delay(4000)
        val currentPlaylist = viewModel.lastUrl.value
        onNextScreen(if (currentPlaylist.isNotEmpty()) "home" else "login")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000208)),
        contentAlignment = Alignment.Center
    ) {
        CinematicBackground(contentAlpha.value, time.value)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                RotatingRing(
                    primaryColor = primaryColor,
                    progress = contentAlpha.value,
                    modifier = Modifier.size(160.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.app_icon_android),
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                            rotationZ = sin(time.value * 2f) * 2f
                        }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { 
                    alpha = contentAlpha.value
                    translationY = titleY.value
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STREAM",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp,
                            fontSize = 32.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp,
                            fontSize = 32.sp
                        ),
                        color = primaryColor
                    )
                }
                
                Text(
                    text = stringResource(R.string.splash_universe_subtitle).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 12.sp,
                        fontWeight = FontWeight.Light,
                        fontSize = 10.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(200.dp)
                    .graphicsLayer { alpha = contentAlpha.value }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(barProgress.value)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, primaryColor, Color.White)
                                )
                            )
                    )

                    if (barProgress.value > 0.01f && barProgress.value < 1f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = (200.dp * barProgress.value) - 2.dp)
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "${(barProgress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = primaryColor.copy(alpha = 0.6f)
                )
            }
        }

        Text(
            text = stringResource(R.string.splash_designed_by).uppercase(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = contentAlpha.value * 0.8f },
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 4.sp,
                fontWeight = FontWeight.ExtraLight,
                fontSize = 9.sp
            ),
            color = Color.White
        )
    }
}
