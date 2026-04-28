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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import coil.compose.AsyncImage
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
    onBack: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isBackInvoked by remember { mutableStateOf(false) }

    val filteredAllChannels = remember(allChannels, searchQuery) {
        if (searchQuery.isBlank()) allChannels
        else allChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currentTime = System.currentTimeMillis()
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { groups.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState()
    
    LaunchedEffect(pagerState.currentPage, groups, allChannels) {
        if (groups.isNotEmpty()) {
            val group = groups[pagerState.currentPage]
            val filteredChannels = if (group == "Other") {
                allChannels.filter { it.group.isNullOrBlank() }
            } else {
                allChannels.filter { it.group == group }
            }
            viewModel.prefetchEpgForChannels(filteredChannels.take(50))
        }
    }
    
    val startTime = currentTime - (2 * 60 * 60 * 1000)
    val endTime = startTime + (24 * 60 * 60 * 1000)
    
    val timeSlots = remember {
        val slots = mutableListOf<Long>()
        var currentSlot = startTime - (startTime % (30 * 60 * 1000))
        while (currentSlot < endTime) {
            slots.add(currentSlot)
            currentSlot += 30 * 60 * 1000
        }
        slots
    }

    val channelColumnWidth = 100.dp
    val slotWidth = 200.dp
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    LaunchedEffect(timeSlots) {
        val firstSlot = timeSlots.firstOrNull() ?: return@LaunchedEffect
        val timeDiff = currentTime - firstSlot
        val slotDurationMs = 30 * 60 * 1000L
        val scrollPositionPx = (timeDiff.toFloat() / slotDurationMs * slotWidth.value * density).toInt()
        val peekOffset = (50 * density).toInt()
        horizontalScrollState.scrollTo((scrollPositionPx - peekOffset).coerceAtLeast(0))
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.epg_search_placeholder), color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Text(
                                stringResource(R.string.epg_title), 
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                style = MaterialTheme.typography.titleMedium,
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }

                if (groups.isNotEmpty() && !isSearchActive) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .padding(horizontal = 12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    ) {
                        groups.forEachIndexed { index, group ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { 
                                    Text(
                                        group.uppercase(), 
                                        color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Black else FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
        ) {
            Column {
                // Time Header with Universe Style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .width(channelColumnWidth)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(horizontalScrollState)
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        timeSlots.forEach { time ->
                            Box(modifier = Modifier.width(slotWidth), contentAlignment = Alignment.Center) {
                                Text(
                                    text = timeFormatter.format(Date(time)), 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                if (groups.isEmpty() && allChannels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    if (isSearchActive) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredAllChannels, key = { it.url }) { channel ->
                                EpgChannelRow(
                                    channel = channel,
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
                            val filteredChannels = remember(allChannels, group) {
                                if (group == "Other") allChannels.filter { it.group.isNullOrBlank() }
                                else allChannels.filter { it.group == group }
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredChannels, key = { it.url }) { channel ->
                                    EpgChannelRow(
                                        channel = channel,
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
            
            // Bottom shadow for better depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(0.2f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
fun EpgChannelRow(
    channel: IptvChannel,
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
    val programsState = viewModel.getProgramsForChannel(channel).collectAsState(initial = emptyList())
    val programs = programsState.value
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        // Channel Column
        Column(
            modifier = Modifier
                .width(channelColumnWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .clickable { onSelectChannel(channel) }
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f))
            ) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.app_icon_android),
                    placeholder = painterResource(R.drawable.app_icon_android)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = channel.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Synchronized Programs Row
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.fillMaxHeight()) {
                val filteredPrograms = remember(programs, startTime, endTime) {
                    programs.filter { it.endTime > startTime && it.startTime < endTime }
                }
                
                if (filteredPrograms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxHeight().width(2000.dp).padding(16.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            stringResource(R.string.epg_no_program),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.2f), 
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
                                .padding(3.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.4f)
                                else MaterialTheme.colorScheme.onSurface.copy(0.05f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onSelectChannel(channel) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = program.title,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${timeFormatter.format(Date(program.startTime))} - ${timeFormatter.format(Date(program.endTime))}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
