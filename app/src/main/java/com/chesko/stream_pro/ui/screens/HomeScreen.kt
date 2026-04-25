package com.chesko.stream_pro.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.ui.components.ShimmerHomeScreen
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenEpg: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenFavorites: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit
) {
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val randomCarousel by viewModel.randomCarouselChannels.collectAsState()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            // Tampilkan status bar saat masuk ke HomeScreen
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                // Sembunyikan kembali status bar saat keluar dari HomeScreen
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/xml"),
        onResult = { uri ->
            uri?.let { viewModel.saveBackupToUri(it) }
        }
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    val headerAlpha = remember {
        derivedStateOf {
            val scroll = scrollState.value.toFloat()
            (scroll / 300f).coerceIn(0f, 1f)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .statusBarsPadding(),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(0.15f), Color.Transparent)
                            )
                        )
                        .clickable {
                            scope.launch {
                                drawerState.snapTo(DrawerValue.Closed)
                                onOpenProfile()
                            }
                        }
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (profileImageUri != null) {
                                    AsyncImage(
                                        model = profileImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.app_icon_android),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Stream Pro User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "PREMIUM",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "UTAMA",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Favorite,
                        label = "Favorit",
                        onClick = {
                            scope.launch {
                                drawerState.snapTo(DrawerValue.Closed)
                                onOpenFavorites()
                            }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.DateRange,
                        label = "Panduan",
                        onClick = {
                            scope.launch {
                                drawerState.snapTo(DrawerValue.Closed)
                                onOpenEpg()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "PENGATURAN",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Pengaturan",
                        onClick = {
                            scope.launch {
                                drawerState.snapTo(DrawerValue.Closed)
                                onOpenSettings()
                            }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Sync,
                        label = "Sinkronisasi",
                        onClick = {
                            viewModel.refreshPlaylist()
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Backup,
                        label = "Cadangkan",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                launcher.launch("iptv_backup_$timestamp.xml")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "DUKUNGAN",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    DrawerMenuItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        label = "Bantuan",
                        onClick = {
                            scope.launch {
                                drawerState.snapTo(DrawerValue.Closed)
                                onOpenHelp()
                            }
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Info,
                        label = "Tentang",
                        onClick = {
                            scope.launch {
                                drawerState.snapTo(DrawerValue.Closed)
                                onOpenAbout()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { onLogout() },
                    color = Color.Red.copy(0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Keluar", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (isLoading || allChannels.isEmpty()) {
                ShimmerHomeScreen()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (searchQuery.isEmpty()) {
                        if (selectedGroup == null) {
                            if (randomCarousel.isNotEmpty()) {
                                EnhancedHeroCarousel(
                                    channels = randomCarousel,
                                    onPlayClick = {
                                        viewModel.markAsPlayed(it)
                                        onSelectChannel(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            } else if (filteredChannels.isNotEmpty()) {
                                EnhancedHeroCarousel(
                                    channels = filteredChannels.take(5),
                                    onPlayClick = {
                                        viewModel.markAsPlayed(it)
                                        onSelectChannel(it)
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        if (groups.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    GroupChip(
                                        text = "Semua",
                                        isSelected = selectedGroup == null,
                                        onClick = { viewModel.setSelectedGroup(null) }
                                    )
                                }
                                items(groups) { group ->
                                    GroupChip(
                                        text = group,
                                        isSelected = selectedGroup == group,
                                        onClick = { viewModel.setSelectedGroup(group) }
                                    )
                                }
                            }
                        }
                    }

                    ContentArea(
                        viewModel = viewModel,
                        channels = filteredChannels,
                        recentlyPlayed = recentlyPlayed,
                        favorites = favoriteChannels,
                        groups = groups,
                        selectedGroup = selectedGroup,
                        onChannelSelected = {
                            viewModel.markAsPlayed(it)
                            onSelectChannel(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            HeaderSection(
                alpha = headerAlpha.value,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onRefresh = { viewModel.refreshPlaylist() },
                onMenuClick = { scope.launch { drawerState.open() } },
                onExit = onNavigateBack,
                modifier = Modifier.statusBarsPadding()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedHeroCarousel(
    channels: List<IptvChannel>,
    onPlayClick: (IptvChannel) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { channels.size }
    )

    val scope = rememberCoroutineScope()
    var isAutoScrollEnabled by remember { mutableStateOf(true) }

    // Auto-scroll
    LaunchedEffect(isAutoScrollEnabled) {
        if (isAutoScrollEnabled && channels.size > 1) {
            delay(6000)
            val nextPage = (pagerState.currentPage + 1) % channels.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val carouselHeight = (configuration.screenHeightDp * 0.3).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(carouselHeight)
    ) {
        // Dynamic Background based on current page
        val currentChannel = channels.getOrNull(pagerState.currentPage)

        Box(modifier = Modifier.fillMaxSize()) {
            if (currentChannel != null) {
                AsyncImage(
                    model = currentChannel.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp)
                        .scale(1.2f)
                        .alpha(0.4f)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.8f),
                                        Color.Black
                                    ),
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                        },
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.app_icon_android),
                    placeholder = painterResource(R.drawable.app_icon_android)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
            pageSpacing = 24.dp
        ) { page ->
            val channel = channels[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = abs(pageOffset).coerceIn(0f, 1f)

            val scale = 0.9f + (1f - 0.9f) * (1f - absOffset)
            val alpha = 0.7f + (1f - 0.7f) * (1f - absOffset)
            val rotationY = pageOffset * 15f

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.rotationY = rotationY
                        cameraDistance = 12f
                        shape = RoundedCornerShape(28.dp)
                        clip = true
                    }
                    .clickable {
                        onPlayClick(channel)
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Background Image (Logo)
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.2f)
                            .blur(10.dp),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.app_icon_android),
                        placeholder = painterResource(R.drawable.app_icon_android)
                    )

                    // Gradient Overlay to ensure text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(120.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    clip = true
                                ),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            )
                        ) {
                            AsyncImage(
                                model = channel.logo,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentScale = ContentScale.Fit,
                                error = painterResource(R.drawable.app_icon_android),
                                placeholder = painterResource(R.drawable.app_icon_android)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = (channel.group?.uppercase() ?: "REKOMENDASI"),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 24.sp,
                                lineHeight = 28.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // Indicators
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val displayCount = channels.size.coerceAtMost(8)
                repeat(displayCount) { iteration ->
                    val isSelected = pagerState.currentPage % displayCount == iteration
                    val indicatorColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    val indicatorWidth = if (isSelected) 28.dp else 8.dp

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                            .width(indicatorWidth)
                            .height(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    alpha: Float,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var localSearchQuery by remember { mutableStateOf(searchQuery) }

    // Update local state if external state changes (e.g., cleared from outside)
    LaunchedEffect(searchQuery) {
        localSearchQuery = searchQuery
    }

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = if (isSearchExpanded) 1f else alpha),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSearchExpanded) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon_android),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "STREAMPRO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderActionButton(Icons.Default.Search, "Search") { isSearchExpanded = true }
                    HeaderActionButton(Icons.Default.Refresh, "Refresh") { onRefresh() }
                    HeaderActionButton(Icons.AutoMirrored.Filled.Logout, "Exit", containerColor = MaterialTheme.colorScheme.primary.copy(0.2f)) { onExit() }
                }
            } else {
                IconButton(
                    onClick = {
                        isSearchExpanded = false
                        localSearchQuery = ""
                        onSearchQueryChange("")
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close Search", tint = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = localSearchQuery,
                    onValueChange = { localSearchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    placeholder = { Text("Cari saluran...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)) },
                    trailingIcon = {
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                localSearchQuery = ""
                                onSearchQueryChange("")
                            }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchQueryChange(localSearchQuery)
                            isSearchExpanded = false
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val finalContainerColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else containerColor
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(finalContainerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ContentArea(
    viewModel: MainViewModel,
    channels: List<IptvChannel>,
    recentlyPlayed: List<IptvChannel>,
    favorites: List<IptvChannel>,
    groups: List<String>,
    selectedGroup: String?,
    onChannelSelected: (IptvChannel) -> Unit
) {
    if (selectedGroup != null) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = selectedGroup,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ChannelGrid(viewModel, channels, onChannelSelected)
        }
    } else {
        Column {
            if (favorites.isNotEmpty()) {
                ContentRow(viewModel, "Favorit", favorites, onChannelSelected)
            }

            if (recentlyPlayed.isNotEmpty()) {
                ContentRow(viewModel, "Terakhir Ditonton", recentlyPlayed, onChannelSelected)
            }

            groups.forEach { group ->
                val groupChannels = channels.filter { it.group == group }
                if (groupChannels.isNotEmpty()) {
                    ContentRow(viewModel, group, groupChannels, onChannelSelected)
                }
            }

            if (groups.isEmpty() && channels.isNotEmpty()) {
                ContentRow(viewModel, "Saluran Populer", channels, onChannelSelected)
            }
        }
    }
}

@Composable
fun ChannelGrid(
    viewModel: MainViewModel,
    channels: List<IptvChannel>,
    onChannelSelected: (IptvChannel) -> Unit
) {
    val columns = 2
    val chunks = channels.chunked(columns)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        chunks.forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                chunk.forEach { channel ->
                    ChannelModernItem(
                        viewModel = viewModel,
                        channel = channel,
                        modifier = Modifier.weight(1f),
                        onClick = { onChannelSelected(channel) }
                    )
                }
                if (chunk.size < columns) {
                    repeat(columns - chunk.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ContentRow(
    viewModel: MainViewModel,
    title: String,
    channels: List<IptvChannel>,
    onChannelSelected: (IptvChannel) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${channels.size} Saluran",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels) { channel ->
                ChannelModernItem(
                    viewModel = viewModel,
                    channel = channel,
                    modifier = Modifier.width(140.dp),
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelModernItem(
    viewModel: MainViewModel,
    channel: IptvChannel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    var isImageLoading by remember { mutableStateOf(true) }
    val currentProgram by viewModel.getCurrentProgram(channel.tvgId).collectAsState(initial = null)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!channel.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onLoading = { isImageLoading = true },
                        onSuccess = { isImageLoading = false },
                        onError = { isImageLoading = false },
                        error = painterResource(R.drawable.app_icon_android),
                        placeholder = painterResource(R.drawable.app_icon_android)
                    )
                    if (isImageLoading) {
                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tv, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.2f), modifier = Modifier.size(40.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (currentProgram != null) {
            Text(
                text = currentProgram?.title ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = "Tidak ada info program",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun GroupChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(0.1f)),
        modifier = Modifier.height(40.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(0.9f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}