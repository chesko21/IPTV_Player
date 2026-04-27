package com.chesko.stream_pro.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
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
    onOpenEpg: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit
) {
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val userName by viewModel.userName.collectAsState()
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
    var showExitDialog by remember { mutableStateOf(false) }
    var isGroupsExpanded by remember { mutableStateOf(false) }

    // Handle system back navigation
    BackHandler {
        when {
            searchQuery.isNotEmpty() -> viewModel.setSearchQuery("")
            selectedGroup != null -> viewModel.setSelectedGroup(null)
            isGroupsExpanded -> isGroupsExpanded = false
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        Dialog(onDismissRequest = { showExitDialog = false }) {
            Surface(
                modifier = Modifier.width(240.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Konfirmasi Keluar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Yakin ingin keluar?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showExitDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("BATAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Button(
                            onClick = { (context as? Activity)?.finish() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("KELUAR", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
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

    var isShuttingDown by remember { mutableStateOf(false) }
    val shutdownProgress = remember { Animatable(0f) }

    val lazyListState = rememberLazyListState()
    val groupLazyListState = rememberLazyListState()

    // Auto-scroll to selected group chip
    LaunchedEffect(selectedGroup, groups) {
        if (groups.isNotEmpty()) {
            val index = if (selectedGroup == null) 0 else groups.indexOf(selectedGroup) + 1
            if (index >= 0) {
                groupLazyListState.animateScrollToItem(index)
            }
        }
    }

    val headerAlpha = remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val scroll = lazyListState.firstVisibleItemScrollOffset.toFloat()
                (scroll / 300f).coerceIn(0f, 1f)
            }
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
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.app_icon_android),
                                        placeholder = painterResource(id = R.drawable.app_icon_android)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.app_icon_android),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                userName,
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
                            Text(
                                userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
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
                        .clickable {
                            scope.launch {
                                drawerState.close()
                                isShuttingDown = true
                                shutdownProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(1200, easing = LinearEasing)
                                )
                                onLogout()
                            }
                        },
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
                        Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold)
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
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (searchQuery.isEmpty()) {
                        if (selectedGroup == null) {
                            if (randomCarousel.isNotEmpty()) {
                                item {
                                    EnhancedHeroCarousel(
                                        channels = randomCarousel,
                                        onPlayClick = {
                                            viewModel.markAsPlayed(it)
                                            onSelectChannel(it)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            } else if (filteredChannels.isNotEmpty()) {
                                item {
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
                        }

                        if (groups.isNotEmpty()) {
                            item {
                                LazyRow(
                                    state = groupLazyListState,
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
                    }

                    // Content Area refactored for LazyColumn performance
                    if (selectedGroup != null || searchQuery.isNotEmpty()) {
                        item {
                            val title = when {
                                searchQuery.isNotEmpty() -> "Hasil Pencarian"
                                selectedGroup != null -> selectedGroup!!
                                else -> ""
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                        
                        // Use grid for selected group or search (2 columns)
                        if (filteredChannels.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                RoundedCornerShape(24.dp)
                                            )
                                            .padding(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            null,
                                            modifier = Modifier.size(80.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(0.2f)
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            "Oops! Saluran tidak ditemukan",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Coba kata kunci lain atau periksa koneksi Anda",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            val chunkedChannels = filteredChannels.chunked(2)
                            items(chunkedChannels) { chunk ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    chunk.forEach { channel ->
                                        ChannelModernItem(
                                            viewModel = viewModel,
                                            channel = channel,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                viewModel.markAsPlayed(channel)
                                                onSelectChannel(channel)
                                            }
                                        )
                                    }
                                    if (chunk.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        val onChannelClick: (IptvChannel) -> Unit = { channel ->
                            viewModel.markAsPlayed(channel)
                            onSelectChannel(channel)
                        }

                        // Regular horizontal rows when no group selected
                        if (favoriteChannels.isNotEmpty()) {
                            item { 
                                ContentRow(
                                    viewModel = viewModel, 
                                    title = "Favorit", 
                                    channels = favoriteChannels, 
                                    onSeeAllClick = { viewModel.setSelectedGroup("Favorit") },
                                    onChannelSelected = onChannelClick
                                ) 
                            }
                        }

                        if (recentlyPlayed.isNotEmpty()) {
                            item { 
                                ContentRow(
                                    viewModel = viewModel, 
                                    title = "Terakhir Ditonton", 
                                    channels = recentlyPlayed, 
                                    onSeeAllClick = { viewModel.setSelectedGroup("Terakhir Ditonton") },
                                    onChannelSelected = onChannelClick
                                ) 
                            }
                        }

                        groups.take(15).forEach { group ->
                            val groupChannels = allChannels.filter { it.group == group }
                            if (groupChannels.isNotEmpty()) {
                                item(key = group) { 
                                    ContentRow(
                                        viewModel = viewModel, 
                                        title = group, 
                                        channels = groupChannels, 
                                        onSeeAllClick = { viewModel.setSelectedGroup(group) },
                                        onChannelSelected = onChannelClick
                                    ) 
                                }
                            }
                        }
                        
                        // Remaining groups if many
                        if (groups.size > 15) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "GRUP LAINNYA",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary.copy(0.7f),
                                            letterSpacing = 1.2.sp
                                        )
                                        Text(
                                            if (isGroupsExpanded) "LIHAT SEDIKIT" else "LIHAT SEMUA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { isGroupsExpanded = !isGroupsExpanded }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))

                                    val remainingGroups = if (isGroupsExpanded) groups.drop(15) else groups.drop(15).take(6)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        remainingGroups.chunked(2).forEach { chunk ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                            ) {
                                                chunk.forEach { group ->
                                                    Card(
                                                        onClick = { viewModel.setSelectedGroup(group) },
                                                        modifier = Modifier.weight(1f, fill = chunk.size > 1),
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                        ),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(54.dp)
                                                                    .background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape)
                                                                    .padding(10.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Image(
                                                                    painter = painterResource(id = R.drawable.app_icon_android),
                                                                    contentDescription = null,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = ContentScale.Fit
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Text(
                                                                group,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                                if (chunk.size < 2) Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }

            HeaderSection(
                alpha = headerAlpha.value,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onRefresh = { viewModel.refreshPlaylist() },
                onMenuClick = { scope.launch { drawerState.open() } },
                modifier = Modifier.statusBarsPadding()
            )

            // 1. UNIVERSAL SHUTDOWN ANIMATION OVERLAY
            if (isShuttingDown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = shutdownProgress.value.coerceIn(0f, 1f)))
                ) {
                    // CRT Shutdown Line Effect
                    val lineScaleY = (1f - (shutdownProgress.value * 1.2f)).coerceAtLeast(0.002f)
                    val lineAlpha = (1f - shutdownProgress.value * 0.8f).coerceAtLeast(0f)

                    // Horizontal Energy Beam
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(2.dp)
                            .graphicsLayer {
                                scaleY = lineScaleY * 100f
                                alpha = lineAlpha
                            }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.primary, Color.White, MaterialTheme.colorScheme.primary, Color.Transparent)
                                )
                            )
                    )

                    // Central Singularity Collapse
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(100.dp)
                            .graphicsLayer {
                                scaleX = (1f - shutdownProgress.value * 1.5f).coerceAtLeast(0f)
                                scaleY = (1f - shutdownProgress.value * 1.5f).coerceAtLeast(0f)
                                alpha = (1f - shutdownProgress.value * 2f).coerceAtLeast(0f)
                            }
                            .background(Color.White, CircleShape)
                            .blur((20.dp * shutdownProgress.value).coerceAtLeast(0.1.dp))
                    )

                    // Cinematic Status Text
                    Text(
                        text = "DEPARTING FROM UNIVERSE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 180.dp)
                            .graphicsLayer {
                                alpha = (1f - shutdownProgress.value * 2.5f).coerceAtLeast(0f)
                                scaleX = 1f + shutdownProgress.value * 0.5f
                                translationY = shutdownProgress.value * 50f
                            }
                    )
                }
            }

            // Optimized Scroll to Top FAB
            val showFab by remember {
                derivedStateOf {
                    lazyListState.firstVisibleItemIndex > 4
                }
            }

            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 50.dp, end = 20.dp)
            ) {
                Surface(
                    onClick = {
                        scope.launch {
                            if (lazyListState.firstVisibleItemIndex > 15) {
                                lazyListState.scrollToItem(0)
                            } else {
                                lazyListState.animateScrollToItem(0)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
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
    val carouselHeight = (configuration.screenHeightDp * 0.28f).coerceAtMost(260f).dp

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
                        .graphicsLayer { 
                            alpha = 0.35f
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    30f, 30f, android.graphics.Shader.TileMode.DECAL
                                ).asComposeRenderEffect()
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.8f),
                                        Color.Black
                                    )
                                )
                            )
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            pageSpacing = 16.dp
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
                                    .padding(8.dp),
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
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = (channel.group?.uppercase() ?: "REKOMENDASI"),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    fontSize = 9.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 28.sp,
                                lineHeight = 32.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { onPlayClick(channel) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WATCH NOW", fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
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
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var localSearchQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery) {
        localSearchQuery = searchQuery
    }

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = (if (isSearchExpanded) 1f else alpha).coerceIn(0f, 0.98f)),
        modifier = modifier.fillMaxWidth(),
        tonalElevation = if (alpha > 0.1f) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSearchExpanded) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "STREAMPRO",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Premium Entertainment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeaderActionButton(Icons.Default.Search, "Search") { isSearchExpanded = true }
                    HeaderActionButton(Icons.Default.Refresh, "Refresh") { onRefresh() }
                }
            } else {
                IconButton(
                    onClick = {
                        isSearchExpanded = false
                        localSearchQuery = ""
                        onSearchQueryChange("")
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Close, "Close Search", tint = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = localSearchQuery,
                    onValueChange = { 
                        localSearchQuery = it
                        onSearchQueryChange(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = { 
                        Text(
                            "Cari saluran atau grup...", 
                            fontSize = 14.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        ) 
                    },
                    trailingIcon = {
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                localSearchQuery = ""
                                onSearchQueryChange("")
                            }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearchQueryChange(localSearchQuery)
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
fun ContentRow(
    viewModel: MainViewModel,
    title: String,
    channels: List<IptvChannel>,
    onSeeAllClick: () -> Unit,
    onChannelSelected: (IptvChannel) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            Surface(
                onClick = onSeeAllClick,
                color = Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SEE ALL (${channels.size})",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(channels, key = { it.url }) { channel ->
                ChannelModernItem(
                    viewModel = viewModel,
                    channel = channel,
                    modifier = Modifier.width(150.dp),
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
        targetValue = if (isPressed) 0.94f else 1f,
        label = "scale"
    )

    var isImageLoading by remember { mutableStateOf(true) }
    
    val currentProgram by remember(channel.tvgId, channel.name) {
        viewModel.getCurrentProgram(channel)
    }.collectAsState(initial = null)

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
                .aspectRatio(16f/9f)
                .shadow(
                    elevation = if (isPressed) 4.dp else 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(
                1.dp, 
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!channel.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
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
                        Icon(
                            Icons.Default.Tv, 
                            null, 
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.1f), 
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Live Badge
                if (currentProgram != null) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            "LIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    }
                }

                if (!channel.drmConfig.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = CircleShape,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "DRM",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        val programText = currentProgram?.title ?: "Tidak ada info program"
        Text(
            text = programText,
            color = if (currentProgram != null) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (currentProgram != null) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun GroupChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "chipScale")
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "chipColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContentColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp, 
            if (isSelected) MaterialTheme.colorScheme.primary.copy(0.5f) 
            else MaterialTheme.colorScheme.onSurface.copy(0.1f)
        ),
        modifier = Modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label, 
                color = MaterialTheme.colorScheme.onSurface, 
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
