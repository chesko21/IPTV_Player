package com.chesko.stream_pro_tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro_tv.ui.components.ChannelTvGridItem
import com.chesko.stream_pro_tv.ui.components.GroupTvItem
import com.chesko.stream_pro_tv.ui.components.PositionFocusedItemInLazyLayout
import com.chesko.stream_pro_tv.ui.components.ShimmerHomeScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    showGroupSelector: Boolean = true,
    onChannelClick: (IptvChannel) -> Unit
) {
    val columns = 6

    val channels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    
    if (isLoading) {
        ShimmerHomeScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00020A))) {

            UniverseBackground(
                primaryColor = MaterialTheme.colorScheme.primary,
                glowAlpha = 0.3f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = "StreamPro TV",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = if (selectedGroup == null) "Explore TV Stream" else selectedGroup!!.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                }

                if (showGroupSelector) {
                    TvGroupSelector(
                        groups = groups,
                        selectedGroup = selectedGroup,
                        onGroupSelected = { viewModel.setSelectedGroup(it) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (channels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null, 
                                    tint = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(100.dp)
                                )
                                Text(
                                    "No star-charts discovered in this sector", 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        TvChannelGrid(
                            channels = channels,
                            onChannelClick = onChannelClick,
                            columns = columns
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvGroupSelector(
    groups: List<String>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    PositionFocusedItemInLazyLayout(parentFraction = 0.1f) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            item {
                GroupTvItem(
                    groupName = "ALL Channel",
                    isSelected = selectedGroup == null,
                    onClick = { onGroupSelected(null) }
                )
            }

            items(groups) { group ->
                GroupTvItem(
                    groupName = group.uppercase(),
                    isSelected = selectedGroup == group,
                    onClick = { onGroupSelected(group) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvChannelGrid(
    channels: List<IptvChannel>,
    onChannelClick: (IptvChannel) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier
) {
    PositionFocusedItemInLazyLayout(parentFraction = 0.3f) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(channels, key = { it.url }) { channel ->
                ChannelTvGridItem(channel, onChannelClick)
            }
        }
    }
}
