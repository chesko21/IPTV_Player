package com.chesko.stream_pro_tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro_tv.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PremiumTvBackground(primaryColor: Color = Color(0xFFE50914)) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing)
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        drawRect(Color(0xFF02040C))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w * 0.5f + cos(phase) * w * 0.1f, h * 0.4f + sin(phase) * h * 0.05f),
                radius = w * 0.9f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A237E).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(w * 0.2f + sin(phase * 0.5f) * w * 0.1f, h * 0.8f),
                radius = w * 0.7f
            )
        )
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
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                val initialTargetForLeadingEdge =
                    parentFraction * containerSize - (childFraction * size)
                val targetForLeadingEdge = if (size <= containerSize &&
                    (containerSize - initialTargetForLeadingEdge) < size) {
                    containerSize - size
                } else {
                    initialTargetForLeadingEdge
                }
                return offset - targetForLeadingEdge
            }
        }
    }

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
    focusRequester: FocusRequester? = null
) {
    val internalFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    
    Surface(
        onClick = { 
            internalFocusRequester.requestFocus()
            onClick(channel) 
        },
        modifier = modifier
            .aspectRatio(1f)
            .focusRequester(focusRequester ?: internalFocusRequester)
            .onFocusChanged { /* Trigger visual states */ }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                (focusRequester ?: internalFocusRequester).requestFocus()
                onClick(channel)
            },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF151515).copy(alpha = 0.6f),
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White.copy(alpha = 0.8f),
            focusedContentColor = Color.Black
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
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
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 9.sp
                    )
                }
            }

            if (!channel.drmConfig.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.content_desc_drm),
                    tint = Color.Black,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp).background(Color(0xFFFFD600), CircleShape).padding(3.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

    Surface(
        onClick = {
            focusRequester.requestFocus()
            onClick()
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
                onClick()
            },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White,
            contentColor = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f),
            focusedContentColor = Color.Black
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = CircleShape),
            border = Border(border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)), shape = CircleShape)
        )
    ) {
        Box(
            modifier = Modifier.height(40.dp).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = groupName, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}
