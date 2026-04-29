package com.chesko.stream_pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chesko.stream_pro.core.ui.MainViewModel

@Composable
fun AppBackground(viewModel: MainViewModel, content: @Composable () -> Unit) {
    val backgroundType by viewModel.backgroundType.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (backgroundType) {
            "color" -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color(backgroundColor))
                )
            }
            "image" -> {
                AsyncImage(
                    model = backgroundImageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(8.dp),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        if (backgroundType != "default") {
            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val overlayColor = if (isDarkTheme) {
                Color.Black.copy(alpha = 0.6f)
            } else {
                Color.White.copy(alpha = 0.8f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }

        content()
    }
}
