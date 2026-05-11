package com.chesko.stream_pro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.data.model.EpgProgram
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpgScreen(
    viewModel: MainViewModel,
    windowSize: WindowSizeClass,
    onBack: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val epgCache by viewModel.epgCache.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isBackInvoked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Optimized: Cache filtered channels
    val filteredAllChannels = remember(allChannels, searchQuery) {
        if (searchQuery.isBlank()) allChannels
        else allChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Optimized: Use single formatter instance
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currentTime = remember { System.currentTimeMillis() }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { groups.size.coerceAtLeast(1) }
    )
    val scope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState()

    // Optimized: Only prefetch visible channels
    LaunchedEffect(pagerState.currentPage, groups) {
        if (groups.isNotEmpty()) {
            val group = groups[pagerState.currentPage]
            val filteredChannels = if (group == "Other") {
                allChannels.filter { it.group.isNullOrBlank() }
            } else {
                allChannels.filter { it.group == group }
            }
            // Prefetch more channels for smoother scrolling
            viewModel.prefetchEpgForChannels(filteredChannels.take(50))
        }
    }

    // Optimized: Time calculation with caching
    val timeData = remember {
        val startTime = currentTime - (2 * 60 * 60 * 1000)
        val endTime = startTime + (24 * 60 * 60 * 1000)
        val slots = mutableListOf<Long>()
        var currentSlot = startTime - (startTime % (30 * 60 * 1000))
        while (currentSlot < endTime) {
            slots.add(currentSlot)
            currentSlot += 30 * 60 * 1000
        }
        Triple(startTime, endTime, slots)
    }
    val (startTime, endTime, timeSlots) = timeData

    val channelColumnWidth = when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 100.dp
        WindowWidthSizeClass.Medium -> 120.dp
        else -> 140.dp
    }
    val slotWidth = when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 180.dp
        WindowWidthSizeClass.Medium -> 220.dp
        else -> 260.dp
    }
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    // Optimized: Smoother scroll positioning
    LaunchedEffect(timeSlots, currentTime) {
        val firstSlot = timeSlots.firstOrNull() ?: return@LaunchedEffect
        val timeDiff = currentTime - firstSlot
        val slotDurationMs = 30 * 60 * 1000L
        val scrollPositionPx = (timeDiff.toFloat() / slotDurationMs * slotWidth.value * density).toInt()
        val peekOffset = (50 * density).toInt()
        horizontalScrollState.scrollTo((scrollPositionPx - peekOffset).coerceAtLeast(0))
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.epg_search_placeholder),
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.epg_title),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (!isBackInvoked) {
                                    isBackInvoked = true
                                    onBack()
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }

                // Optimized: Only show tabs when not searching and groups exist
                if (groups.isNotEmpty() && !isSearchActive) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = {
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(pagerState.currentPage)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .padding(horizontal = 12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    ) {
                        groups.forEachIndexed { index, group ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                        // Reset scroll position on tab change
                                        horizontalScrollState.scrollTo(0)
                                    }
                                },
                                text = {
                                    Text(
                                        group.uppercase(),
                                        color = if (pagerState.currentPage == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                        fontSize = 11.sp,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Black else FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            Column {
                // Time Header with improved performance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .width(channelColumnWidth)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(horizontalScrollState)
                            .height(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        timeSlots.forEach { time ->
                            Box(
                                modifier = Modifier.width(slotWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = timeFormatter.format(Date(time)),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Optimized: Show loading only when necessary
                if (groups.isEmpty() && allChannels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    // Optimized: Use remember for list state
                    val listState = rememberLazyListState()

                    if (isSearchActive) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                                items(
                                items = filteredAllChannels,
                                key = { it.url }
                            ) { channel ->
                                EpgChannelRow(
                                    channel = channel,
                                    programs = epgCache[channel.url] ?: emptyList(),
                                    viewModel = viewModel,
                                    horizontalScrollState = horizontalScrollState,
                                    startTime = startTime,
                                    endTime = endTime,
                                    slotWidth = slotWidth,
                                    channelColumnWidth = channelColumnWidth,
                                    currentTime = currentTime,
                                    timeFormatter = timeFormatter,
                                    onSelectChannel = onSelectChannel
                                )
                            }
                        }
                    } else {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top
                        ) { pageIndex ->
                            val group = groups.getOrNull(pageIndex) ?: return@HorizontalPager

                            // Optimized: Cache filtered channels per group
                            val filteredChannels = remember(allChannels, group) {
                                if (group == "Other")
                                    allChannels.filter { it.group.isNullOrBlank() }
                                else
                                    allChannels.filter { it.group == group }
                            }

                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = filteredChannels,
                                    key = { it.url }
                                ) { channel ->
                                    EpgChannelRow(
                                        channel = channel,
                                        programs = epgCache[channel.url] ?: emptyList(),
                                        viewModel = viewModel,
                                        horizontalScrollState = horizontalScrollState,
                                        startTime = startTime,
                                        endTime = endTime,
                                        slotWidth = slotWidth,
                                        channelColumnWidth = channelColumnWidth,
                                        currentTime = currentTime,
                                        timeFormatter = timeFormatter,
                                        onSelectChannel = onSelectChannel
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

@Composable
fun EpgChannelRow(
    channel: IptvChannel,
    programs: List<EpgProgram>,
    viewModel: MainViewModel,
    horizontalScrollState: ScrollState,
    startTime: Long,
    endTime: Long,
    slotWidth: androidx.compose.ui.unit.Dp,
    channelColumnWidth: androidx.compose.ui.unit.Dp,
    currentTime: Long,
    timeFormatter: SimpleDateFormat,
    onSelectChannel: (IptvChannel) -> Unit
) {
    val context = LocalContext.current

    // Trigger prefetch if missing
    LaunchedEffect(channel.url) {
        if (programs.isEmpty()) {
            viewModel.prefetchEpgForChannels(listOf(channel))
        }
    }

    // Optimized: Cache filtered programs
    val filteredPrograms = remember(programs, startTime, endTime) {
        programs.filter { it.endTime > startTime && it.startTime < endTime }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        // Channel Column - Optimized
        Column(
            modifier = Modifier
                .width(channelColumnWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                .clickable { onSelectChannel(channel) }
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optimized: Use Surface dengan fixed size
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logo)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.app_icon_android)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = channel.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Programs Row - Optimized
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            Row(modifier = Modifier.fillMaxHeight()) {
                if (filteredPrograms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2000.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            stringResource(R.string.epg_no_program),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.2f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    filteredPrograms.forEach { program ->
                        val isCurrent = currentTime in program.startTime..program.endTime
                        val durationMs = program.endTime - program.startTime
                        val width = (durationMs / (30 * 60 * 1000f) * slotWidth.value).dp

                        Card(
                            modifier = Modifier
                                .width(width)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clickable { onSelectChannel(channel) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = null, // Remove border for performance
                            elevation = CardDefaults.cardElevation(0.dp) // No elevation
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = program.title,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${timeFormatter.format(Date(program.startTime))} - ${timeFormatter.format(Date(program.endTime))}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}