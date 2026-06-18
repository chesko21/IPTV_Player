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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.*
import com.chesko.stream_pro_tv.R
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UniverseBackground(primaryColor: Color, glowAlpha: Float) {
    val isLowEnd = android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.M
    
    val infiniteTransition = rememberInfiniteTransition(label = "universe")

    val starAlpha by if (isLowEnd) {
        mutableStateOf(0.7f)
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "starAlpha"
        )
    }

    val nebulaOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isLowEnd) 120000 else 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nebulaOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF00020A))

        val starCount = if (isLowEnd) 50 else 150
        val random = java.util.Random(42)
        repeat(starCount) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val starSize = random.nextFloat() * (if (isLowEnd) 1.dp.toPx() else 2.dp.toPx())
            val individualAlpha = (random.nextFloat() * 0.5f + 0.5f) * starAlpha
            
            drawCircle(
                color = Color.White.copy(alpha = individualAlpha),
                radius = starSize,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.12f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.2f + (nebulaOffset % 200),
                    size.height * 0.3f
                ),
                radius = size.minDimension * (if (isLowEnd) 0.6f else 0.8f)
            )
        )

        if (!isLowEnd) {
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
        }
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D47A1).copy(alpha = 0.08f * glowAlpha), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.5f,
                    size.height * 0.5f + (nebulaOffset % 100 - 50)
                ),
                radius = size.minDimension * (if (isLowEnd) 0.8f else 1.0f)
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

    val configuration = LocalConfiguration.current
    val isSmallHeight = configuration.screenHeightDp < 500
    
    val logoSize = if (isSmallHeight) 80.dp else 110.dp
    val titleSize = if (isSmallHeight) 32.sp else 48.sp
    val subtitleSize = if (isSmallHeight) 9.sp else 11.sp
    val subtitleSpacing = if (isSmallHeight) 2.sp else 4.sp
    val mainSpacerHeight = if (isSmallHeight) 16.dp else 24.dp
    val bottomSpacerHeight = if (isSmallHeight) 24.dp else 40.dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        UniverseBackground(primaryColor, glowAlpha.value)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
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

                Image(
                    painter = painterResource(R.drawable.app_icon_androidtv),
                    contentDescription = null,
                    modifier = Modifier.size(logoSize)
                )
            }

            Spacer(modifier = Modifier.height(mainSpacerHeight))

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
                            letterSpacing = if (isSmallHeight) (-1).sp else (-2).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = titleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (isSmallHeight) (-1).sp else (-2).sp
                        ),
                        color = primaryColor
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isSmallHeight) 6.dp else 12.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = if (isSmallHeight) 8.dp else 12.dp, vertical = if (isSmallHeight) 2.dp else 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.branding_explore),
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

            Spacer(modifier = Modifier.height(bottomSpacerHeight))

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
        Text(
            text = stringResource(R.string.branding_developed),
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
