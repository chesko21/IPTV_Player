package com.chesko.stream_pro.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Shared horizontal scroll state for synchronization
    val horizontalScrollState = rememberScrollState()
    
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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari saluran...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                    contentDescription = "Kembali", 
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                } else {
                    TopAppBar(
                        title = { Text("Panduan TV", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (!isBackInvoked) {
                                    isBackInvoked = true
                                    onBack()
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Kembali", 
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Cari", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (groups.isNotEmpty() && !isSearchActive) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty()) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary
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
                                        group, 
                                        color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge
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
        Column(modifier = Modifier.padding(padding)) {
            // Horizontal Header for Time
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.width(channelColumnWidth).height(40.dp).background(MaterialTheme.colorScheme.surface))
                Row(
                    modifier = Modifier.horizontalScroll(horizontalScrollState).height(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    timeSlots.forEach { time ->
                        Box(modifier = Modifier.width(slotWidth), contentAlignment = Alignment.Center) {
                            Text(
                                text = timeFormatter.format(Date(time)), 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (isSearchActive) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredAllChannels, key = { it.id }) { channel ->
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
                    val group = groups[pageIndex]
                    val filteredChannels = remember(allChannels, group) {
                        if (group == "Other") allChannels.filter { it.group.isNullOrBlank() }
                        else allChannels.filter { it.group == group }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredChannels, key = { it.id }) { channel ->
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
    val programsState = viewModel.getProgramsForChannel(channel.tvgId).collectAsState(initial = emptyList())
    val programs = programsState.value
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        // Channel Column
        Column(
            modifier = Modifier
                .width(channelColumnWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onSelectChannel(channel) }
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = channel.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
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
        ) {
            Row(modifier = Modifier.fillMaxHeight()) {
                val filteredPrograms = remember(programs, startTime, endTime) {
                    programs.filter { it.endTime > startTime && it.startTime < endTime }
                }
                
                if (filteredPrograms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxHeight().width(2000.dp).padding(8.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Tidak ada jadwal tersedia", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), fontSize = 11.sp)
                    }
                } else {
                    filteredPrograms.forEach { program ->
                        val durationMs = program.endTime - program.startTime
                        val width = (durationMs / (30 * 60 * 1000f) * slotWidth.value).dp
                        
                        Box(
                            modifier = Modifier
                                .width(width)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (currentTime in program.startTime..program.endTime)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .clickable { onSelectChannel(channel) }
                                .padding(6.dp)
                        ) {
                            Column {
                                Text(
                                    text = program.title,
                                    color = if (currentTime in program.startTime..program.endTime)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${timeFormatter.format(Date(program.startTime))} - ${timeFormatter.format(Date(program.endTime))}",
                                    color = if (currentTime in program.startTime..program.endTime)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
