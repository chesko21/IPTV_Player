package com.chesko.stream_pro_tv.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.chesko.stream_pro_tv.R
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UniverseBackground(primaryColor: Color, glowAlpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "universe")
    
    // Stars animation
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    // Nebula movement
    val nebulaOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nebulaOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Deep Space Background
        drawRect(Color(0xFF00020A))

        // Drawing stars
        val starCount = 150
        val random = java.util.Random(42) // Fixed seed for consistency
        repeat(starCount) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val starSize = random.nextFloat() * 2.dp.toPx()
            val individualAlpha = (random.nextFloat() * 0.5f + 0.5f) * starAlpha
            
            drawCircle(
                color = Color.White.copy(alpha = individualAlpha),
                radius = starSize,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }

        // Cosmic Nebulas (Big blurred spots)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.15f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.2f + (nebulaOffset % 200),
                    size.height * 0.3f
                ),
                radius = size.minDimension * 0.8f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6A1B9A).copy(alpha = 0.1f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.8f - (nebulaOffset % 150),
                    size.height * 0.7f
                ),
                radius = size.minDimension * 0.7f
            )
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D47A1).copy(alpha = 0.1f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.5f,
                    size.height * 0.5f + (nebulaOffset % 100 - 50)
                ),
                radius = size.minDimension * 1.0f
            )
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TvSplashScreen(onNextScreen: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val storagePermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val logoRotate = remember { Animatable(-15f) }
    val glowAlpha = remember { Animatable(0f) }
    val barWidth = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
        launch {
            alpha.animateTo(1f, tween(1500, easing = EaseOutQuart))
        }
        launch {
            logoRotate.animateTo(0f, tween(2000, easing = EaseOutBack))
        }
        launch {
            delay(500)
            glowAlpha.animateTo(1f, tween(2500, easing = LinearEasing))
        }
        launch {
            delay(1200)
            textAlpha.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
        }
        launch {
            delay(2000)
            barWidth.animateTo(1f, tween(2000, easing = FastOutSlowInEasing))
        }
        
        if (!storagePermissionState.status.isGranted) {
            storagePermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(storagePermissionState.status.isGranted) {
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. UNIVERSE THEMED BACKGROUND
        UniverseBackground(primaryColor, glowAlpha.value)

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
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.2f,
                    targetValue = 2.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Canvas(modifier = Modifier.size(logoSize * pulseScale)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f * glowAlpha.value),
                                Color(0xFF6A1B9A).copy(alpha = 0.1f * glowAlpha.value),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2
                    )
                }

                // The actual Logo
                Image(
                    painter = painterResource(R.drawable.app_icon_androidtv),
                    contentDescription = null,
                    modifier = Modifier.size(logoSize)
                )
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
                        color = Color.White
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
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EXPLORE THE CINEMATIC UNIVERSE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = subtitleSpacing,
                            textAlign = TextAlign.Center
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 3. LOADING INDICATOR (Stardust Path style)
            Box(contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    progress = { barWidth.value },
                    modifier = Modifier
                        .width(200.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .graphicsLayer(alpha = textAlpha.value),
                    color = primaryColor,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
            }
        }

        // 4. FOOTER (Credit)
        Text(
            text = "BY CHESKO",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer(alpha = textAlpha.value),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.3f),
            letterSpacing = 6.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
