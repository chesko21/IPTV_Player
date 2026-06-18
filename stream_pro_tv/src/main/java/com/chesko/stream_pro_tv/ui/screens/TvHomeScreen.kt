package com.chesko.stream_pro_tv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import androidx.tv.material3.*
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro_tv.ui.components.ChannelTvGridItem
import com.chesko.stream_pro_tv.ui.components.GroupTvItem
import com.chesko.stream_pro_tv.ui.components.PositionFocusedItemInLazyLayout
import com.chesko.stream_pro_tv.ui.components.ShimmerHomeScreen

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    showGroupSelector: Boolean = true,
    onChannelClick: (IptvChannel) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallHeight = configuration.screenHeightDp < 580
    val columns = if (isSmallHeight) 7 else 6

    val channels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()

    val lastFocusedChannelRequester = remember { FocusRequester() }

    LaunchedEffect(channels) {
        if (selectedChannel != null && channels.any { it.url == selectedChannel?.url }) {
            try {
                lastFocusedChannelRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00020A))) {
        UniverseBackground(
            primaryColor = MaterialTheme.colorScheme.primary,
            glowAlpha = 0.3f
        )

        Crossfade(targetState = isLoading, label = "loading_transition") { loading ->
            if (loading) {
                ShimmerHomeScreen(showBackground = false)
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    TvClock(
                        isSmallHeight = isSmallHeight,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                end = if (isSmallHeight) 24.dp else 48.dp, 
                                top = if (isSmallHeight) 16.dp else 32.dp
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = if (isSmallHeight) 24.dp else 32.dp, 
                                end = if (isSmallHeight) 24.dp else 48.dp, 
                                top = if (isSmallHeight) 16.dp else 32.dp, 
                                bottom = if (isSmallHeight) 16.dp else 32.dp
                            )
                    ) {
                        Column(modifier = Modifier.padding(bottom = if (isSmallHeight) 12.dp else 24.dp)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = if (isSmallHeight) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (isSmallHeight) 2.sp else 4.sp
                            )
                            Text(
                                text = if (selectedGroup == null) stringResource(R.string.home_explore) else selectedGroup!!.uppercase(),
                                style = if (isSmallHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        }

                        if (showGroupSelector) {
                            TvGroupSelector(
                                groups = groups,
                                selectedGroup = selectedGroup,
                                onGroupSelected = { viewModel.setSelectedGroup(it) },
                                isSmallHeight = isSmallHeight
                            )
                            Spacer(modifier = Modifier.height(if (isSmallHeight) 12.dp else 24.dp))
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = channels,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(500)) togetherWith 
                                    fadeOut(animationSpec = tween(300))
                                },
                                label = "content_transition"
                            ) { currentChannels ->
                                if (currentChannels.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Search, 
                                                contentDescription = null, 
                                                tint = Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier.size(if (isSmallHeight) 60.dp else 100.dp)
                                            )
                                            Text(
                                                stringResource(R.string.home_no_charts), 
                                                style = if (isSmallHeight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    TvChannelGrid(
                                        channels = currentChannels,
                                        selectedChannel = selectedChannel,
                                        focusRequester = lastFocusedChannelRequester,
                                        onChannelClick = onChannelClick,
                                        columns = columns
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvClock(
    isSmallHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = Calendar.getInstance()
            delay(1000)
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = timeFormatter.format(currentTime.time),
            style = if (isSmallHeight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = if (isSmallHeight) (-1).sp else (-2).sp
        )
        Text(
            text = dateFormatter.format(currentTime.time).uppercase(),
            style = if (isSmallHeight) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (isSmallHeight) 1.sp else 2.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvGroupSelector(
    groups: List<String>,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    isSmallHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    PositionFocusedItemInLazyLayout(parentFraction = 0.1f) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(if (isSmallHeight) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            item {
                GroupTvItem(
                    groupName = stringResource(R.string.home_all_channels),
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

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TvChannelGrid(
    channels: List<IptvChannel>,
    selectedChannel: IptvChannel?,
    focusRequester: FocusRequester,
    onChannelClick: (IptvChannel) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    PositionFocusedItemInLazyLayout(parentFraction = 0.3f) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 26.dp),
            modifier = modifier
                .fillMaxSize()
                .focusProperties {
                   onExit = { FocusRequester.Default }
                }
        ) {
            items(channels, key = { it.url }) { channel ->
                val isSelected = selectedChannel?.url == channel.url
                ChannelTvGridItem(
                    channel = channel,
                    onClick = onChannelClick,
                    focusRequester = if (isSelected) focusRequester else null,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
