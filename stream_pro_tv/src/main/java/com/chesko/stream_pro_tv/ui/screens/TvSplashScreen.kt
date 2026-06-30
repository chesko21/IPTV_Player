package com.chesko.stream_pro_tv.ui.screens

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
import com.google.accompanist.permissions.*
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
fun CinematicBackground(
    primaryColor: Color,
    intensity: Float,
    time: Float
) {
    val particles = remember {
        List(60) {
            PremiumParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.002f + 0.001f,
                radius = Random.nextFloat() * 1.5f + 0.5f,
                alpha = Random.nextFloat() * 0.5f + 0.1f,
                phase = Random.nextFloat() * PI.toFloat() * 2f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF000208))

        val animX = w * 0.5f + cos(time * 0.5f) * w * 0.1f
        val animY = h * 0.4f + sin(time * 0.3f) * h * 0.05f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.12f * intensity), Color.Transparent),
                center = Offset(animX, animY),
                radius = w * 0.8f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A237E).copy(alpha = 0.08f * intensity), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.8f),
                radius = w * 0.6f
            )
        )

        particles.forEach { p ->
            p.y -= p.speed
            if (p.y < -0.1f) p.y = 1.1f
            
            val pAlpha = p.alpha * intensity * (0.5f + 0.5f * sin(time * 2f + p.phase))
            if (pAlpha > 0.05f) {
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    radius = p.radius,
                    center = Offset(
                        (p.x + sin(time + p.phase) * 0.02f) * w,
                        p.y * h
                    )
                )
            }
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f * intensity)),
                center = center,
                radius = w * 0.9f
            )
        )
    }
}

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
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()

        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            style = Stroke(width = 1f)
        )

        rotate(rotation) {
            drawArc(
                color = primaryColor.copy(alpha = 0.8f * progress),
                startAngle = -90f,
                sweepAngle = 120f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val angleRad = Math.toRadians(-90.0 + 120.0 * progress).toFloat()
            val r = size.width / 2
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(center.x + r * cos(angleRad), center.y + r * sin(angleRad))
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TvSplashScreen(onNextScreen: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600 || configuration.screenHeightDp < 500
    val primaryColor = Color(0xFFE50914)

    val storagePermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val barProgress = remember { Animatable(0f) }
    val time = remember { Animatable(0f) }
    val titleY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        launch {
            time.animateTo(100f, infiniteRepeatable(tween(100000, easing = LinearEasing)))
        }

        launch {
            logoAlpha.animateTo(1f, tween(1200, easing = EaseOutCubic))
        }
        launch {
            logoScale.animateTo(1f, tween(1500, easing = EaseOutBack))
        }
        
        delay(800)
        
        launch {
            contentAlpha.animateTo(1f, tween(1000))
            titleY.animateTo(0f, tween(1000, easing = EaseOutCubic))
        }

        delay(500)
        barProgress.animateTo(1f, tween(4000, easing = EaseInOutQuart))
    }

    LaunchedEffect(storagePermissionState.status.isGranted) {
        if (!storagePermissionState.status.isGranted) {
            storagePermissionState.launchPermissionRequest()
        }
        delay(6000)
        onNextScreen()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000208)),
        contentAlignment = Alignment.Center
    ) {
        CinematicBackground(primaryColor, contentAlpha.value, time.value)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(if (isSmallScreen) 160.dp else 240.dp)
            ) {
                RotatingRing(
                    primaryColor = primaryColor,
                    progress = contentAlpha.value,
                    modifier = Modifier.size(if (isSmallScreen) 120.dp else 180.dp)
                )

                Image(
                    painter = painterResource(R.drawable.app_icon_androidtv),
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (isSmallScreen) 80.dp else 120.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            fontSize = if (isSmallScreen) 32.sp else 54.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            fontSize = if (isSmallScreen) 32.sp else 54.sp
                        ),
                        color = primaryColor
                    )
                }
                
                Text(
                    text = stringResource(R.string.branding_explore).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 12.sp,
                        fontWeight = FontWeight.Light,
                        fontSize = if (isSmallScreen) 10.sp else 14.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Box(
                modifier = Modifier
                    .width(if (isSmallScreen) 180.dp else 300.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
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
            }
        }

        Text(
            text = stringResource(R.string.branding_developed).uppercase(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = contentAlpha.value * 0.8f },
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 6.sp,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraLight
            ),
            color = Color.White
        )
    }
}
