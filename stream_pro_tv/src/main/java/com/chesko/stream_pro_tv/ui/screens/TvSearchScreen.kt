package com.chesko.stream_pro_tv.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro_tv.ui.components.ChannelTvGridItem
import com.chesko.stream_pro_tv.ui.components.PositionFocusedItemInLazyLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.input.key.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSearchScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onChannelClick: (IptvChannel) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmall = screenWidth < 600.dp

    val columns = when {
        screenWidth < 600.dp -> 4
        screenWidth < 1240.dp -> 6
        else -> 8
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .onKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyDown) {
                    onBack()
                    true
                } else {
                    false
                }
            }
    ) {
        // Animated Background
        val infiniteTransition = rememberInfiniteTransition(label = "bg")
        val bgOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2000f,
            animationSpec = infiniteRepeatable(
                animation = tween(40000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bgOffset"
        )

        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE50914).copy(alpha = 0.15f), Color.Transparent),
                    center = center.copy(x = center.x + (bgOffset % 1000f - 500f), y = center.y + (bgOffset % 800f - 400f)),
                    radius = size.minDimension * 1.5f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2979FF).copy(alpha = 0.12f), Color.Transparent),
                    center = center.copy(x = center.x - (bgOffset % 1200f - 600f), y = center.y - (bgOffset % 600f - 300f)),
                    radius = size.minDimension * 1.2f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isSmall) 12.dp else 32.dp, // Padding lebih minimalis
                    vertical = if (isSmall) 12.dp else 24.dp
                )
        ) {
            // Header - Lebih Kecil
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White,
                        contentColor = Color.White,
                        focusedContentColor = Color.Black
                    )
                ) {
                    Box(modifier = Modifier.size(if (isSmall) 32.dp else 38.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "PENCARIAN",
                    style = if (isSmall) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(if (isSmall) 12.dp else 16.dp))

            // Search Bar Row - Lebih Ramping
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isSmall) 44.dp else 52.dp) // Tinggi dikurangi
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                scope.launch {
                                    delay(300)
                                    keyboardController?.show()
                                }
                            }
                        },
                    placeholder = { 
                        Text(
                            "Ketik nama channel...", 
                            color = Color.Gray, 
                            fontSize = 14.sp 
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                if (searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = { 
                            viewModel.setSearchQuery("") 
                            focusRequester.requestFocus()
                        },
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            focusedContentColor = Color.White
                        ),
                        modifier = Modifier.size(if (isSmall) 44.dp else 52.dp) // Ukuran disamakan dengan input
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Clear",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmall) 12.dp else 16.dp))

            // Results - Grid Lebih Kecil & Rapat
            if (filteredChannels.isEmpty() && searchQuery.isNotEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada hasil ditemukan", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredChannels) { channel ->
                        ChannelTvGridItem(channel, onChannelClick)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Reset pencarian saat meninggalkan layar
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setSearchQuery("")
        }
    }
}
