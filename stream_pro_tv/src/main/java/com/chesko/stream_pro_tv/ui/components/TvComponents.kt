package com.chesko.stream_pro_tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.focusRequester
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro_tv.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush

@Composable
fun PremiumTvBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset"
    )
    val bgOffset = bgOffsetState.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
        ) {
            val offset1X = bgOffset % 1000f - 500f
            val offset1Y = bgOffset % 800f - 400f
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE50914).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center.copy(
                        x = center.x + offset1X,
                        y = center.y + offset1Y
                    ),
                    radius = size.minDimension * 1.5f
                )
            )
            
            val offset2X = bgOffset % 1200f - 600f
            val offset2Y = bgOffset % 600f - 300f
            
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2979FF).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center.copy(
                        x = center.x - offset2X,
                        y = center.y - offset2Y
                    ),
                    radius = size.minDimension * 1.2f
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PositionFocusedItemInLazyLayout(
    parentFraction: Float = 0.3f,
    childFraction: Float = 0f,
    content: @Composable () -> Unit,
) {
    val bringIntoViewSpec = remember(parentFraction, childFraction) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,       // Item's initial position
                size: Float,         // Item's size
                containerSize: Float // Container's size
            ): Float {
                // Calculate the offset position of the item's leading edge.
                val initialTargetForLeadingEdge =
                    parentFraction * containerSize - (childFraction * size)
                // If the item fits in the container, and scrolling would cause
                // its trailing edge to be clipped, adjust targetForLeadingEdge
                // to prevent over-scrolling near the end of list.
                val targetForLeadingEdge = if (size <= containerSize &&
                    (containerSize - initialTargetForLeadingEdge) < size) {
                    // If clipped, align the item's trailing edge with the
                    // container's trailing edge.
                    containerSize - size
                } else {
                    initialTargetForLeadingEdge
                }
                // Return scroll distance relative to initial item position.
                return offset - targetForLeadingEdge
            }
        }
    }

    // Apply the spec to all scrollables in the hierarchy
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides bringIntoViewSpec,
        content = content,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelTvGridItem(
    channel: IptvChannel,
    onClick: (IptvChannel) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    Surface(
        onClick = { onClick(channel) },
        modifier = modifier
            .aspectRatio(1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White.copy(alpha = 0.8f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                elevation = 15.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        error = painterResource(id = R.drawable.app_icon_androidtv),
                        placeholder = painterResource(id = R.drawable.app_icon_androidtv)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (LocalContentColor.current == Color.Black) 
                                Color.Transparent 
                            else 
                                Color(0xFF081A38).copy(alpha = 0.8f)
                        )
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // DRM Lock Indicator
            if (!channel.drmConfig.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.content_desc_drm),
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .background(Color(0xFFFFD600), CircleShape)
                        .padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GroupTvItem(
    groupName: String, 
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.extraLarge),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.extraLarge
            ),
            border = Border(
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent),
                shape = MaterialTheme.shapes.extraLarge
            )
        )
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = groupName, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
