package com.chesko.stream_pro.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.chesko.stream_pro.R
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.ui.components.ShimmerHomeScreen
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    windowSize: WindowSizeClass,
    onLogout: () -> Unit,
    onOpenEpg: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit,
    shouldOpenDrawer: Boolean = false
) {
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val randomCarousel by viewModel.randomCarouselChannels.collectAsState()

    val backgroundType by viewModel.backgroundType.collectAsState()
    val backgroundColorInt by viewModel.backgroundColor.collectAsState()
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()

    val context = LocalContext.current
    val favGroup = stringResource(com.chesko.stream_pro.core.R.string.group_favorites)
    val historyGroup = stringResource(com.chesko.stream_pro.core.R.string.group_recently_played)
    
    var showExitDialog by remember { mutableStateOf(false) }
    var isGroupsExpanded by remember { mutableStateOf(false) }
    
    var isShuttingDown by remember { mutableStateOf(false) }
    val shutdownProgress = remember { Animatable(0f) }

    val drawerState = rememberDrawerState(initialValue = if (shouldOpenDrawer) DrawerValue.Open else DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Local loading for playlist switching effect
    var isSwitchingPlaylist by remember { mutableStateOf(false) }

    // Handle system back navigation
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            selectedGroup != null -> viewModel.setSelectedGroup(null)
            isGroupsExpanded -> isGroupsExpanded = false
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        Dialog(onDismissRequest = { showExitDialog = false }) {
            Surface(
                modifier = Modifier.width(280.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.exit_confirm_msg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showExitDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(R.string.btn_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = {
                                showExitDialog = false
                                scope.launch {
                                    isShuttingDown = true
                                    shutdownProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                                    )
                                    delay(150)
                                    (context as? Activity)?.finish()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                stringResource(R.string.btn_exit),
                                fontWeight = FontWeight.Bold
                            )
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

    LaunchedEffect(shouldOpenDrawer) {
        if (shouldOpenDrawer && drawerState.isClosed) {
            drawerState.open()
        }
    }

    val lazyListState = rememberLazyListState()
    val groupLazyListState = rememberLazyListState()

    // Smart Caching: Prefetch EPG for visible items indirectly handled by startSmartCaching in ViewModel

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
            val drawerWidthFraction = when (windowSize.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 0.65f
                WindowWidthSizeClass.Medium -> 0.45f
                else -> 0.30f
            }
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(drawerWidthFraction)
                    .statusBarsPadding(),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(MaterialTheme.colorScheme.primary.copy(0.12f), Color.Transparent)
                                    )
                                )
                                .clickable {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        onOpenProfile()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(60.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                    shadowElevation = 10.dp
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
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        userName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                stringResource(R.string.premium_badge).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    thickness = 1.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (allPlaylists.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.drawer_group_playlists),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )

                                DrawerMenuItem(
                                    icon = Icons.Default.Cloud,
                                    label = stringResource(R.string.group_all),
                                    isSelected = selectedPlaylistId == null,
                                    onClick = {
                                        if (selectedPlaylistId != null) {
                                            scope.launch {
                                                isSwitchingPlaylist = true
                                                viewModel.setSelectedPlaylistId(null)
                                                drawerState.close()
                                                delay(800)
                                                isSwitchingPlaylist = false
                                            }
                                        } else {
                                            scope.launch { drawerState.close() }
                                        }
                                    }
                                )

                                allPlaylists.forEach { playlist ->
                                    DrawerMenuItem(
                                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                        label = playlist.name,
                                        isSelected = selectedPlaylistId == playlist.id,
                                        onClick = {
                                            if (selectedPlaylistId != playlist.id) {
                                                scope.launch {
                                                    isSwitchingPlaylist = true
                                                    viewModel.setSelectedPlaylistId(playlist.id)
                                                    drawerState.close()
                                                    delay(800)
                                                    isSwitchingPlaylist = false
                                                }
                                            } else {
                                                scope.launch { drawerState.close() }
                                            }
                                        }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), modifier = Modifier.padding(horizontal = 12.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                stringResource(R.string.drawer_group_main),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.DateRange,
                                label = stringResource(R.string.menu_epg),
                                onClick = {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        onOpenEpg()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), modifier = Modifier.padding(horizontal = 12.dp))
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                stringResource(R.string.drawer_group_settings),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Settings,
                                label = stringResource(R.string.menu_settings),
                                onClick = {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        onOpenSettings()
                                    }
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Sync,
                                label = stringResource(R.string.menu_sync),
                                onClick = {
                                    viewModel.refreshPlaylist()
                                    scope.launch { drawerState.close() }
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Backup,
                                label = stringResource(R.string.menu_backup),
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                        launcher.launch("iptv_backup_$timestamp.xml")
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), modifier = Modifier.padding(horizontal = 12.dp))
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                stringResource(R.string.drawer_group_support),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )

                            DrawerMenuItem(
                                icon = Icons.AutoMirrored.Filled.HelpOutline,
                                label = stringResource(R.string.menu_help),
                                onClick = {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        onOpenHelp()
                                    }
                                }
                            )

                            DrawerMenuItem(
                                icon = Icons.Default.Info,
                                label = stringResource(R.string.menu_about),
                                onClick = {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        onOpenAbout()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .clickable {
                                    scope.launch {
                                        drawerState.close()
                                        isShuttingDown = true
                                        shutdownProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                                        )
                                        delay(150)
                                        onLogout()
                                    }
                                },
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(R.string.menu_logout), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                            }
                        }

                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isShuttingDown) {
                        val p = shutdownProgress.value
                        alpha = 1f - p
                        scaleX = 1f - (p * 0.05f)
                        scaleY = 1f - (p * 0.05f)
                    }
                }
        ) {
            // Background Layer
            when (backgroundType) {
                "color" -> Box(modifier = Modifier.fillMaxSize().background(Color(backgroundColorInt)))
                "image" -> {
                    if (backgroundImageUri != null) {
                        AsyncImage(
                            model = backgroundImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isSystemInDarkTheme()) 0.6f else 0.3f
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
                    }
                }
                else -> Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
            }

            if (isLoading || allChannels.isEmpty() || isSwitchingPlaylist) {
                ShimmerHomeScreen()
                
                // Overlay for switching playlist to make it feel more "official"
                if (isSwitchingPlaylist) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.msg_switching_server),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Spacer(modifier = Modifier.statusBarsPadding().height(60.dp))
                    }

                    if (searchQuery.isEmpty()) {
                        if (selectedGroup == null) {
                            if (randomCarousel.isNotEmpty()) {
                                item {
                                    EnhancedHeroCarousel(
                                        channels = randomCarousel,
                                        onPlayClick = {
                                            viewModel.setSelectedGroup(null)
                                            viewModel.markAsPlayed(it)
                                            onSelectChannel(it)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            } else if (filteredChannels.isNotEmpty()) {
                                item {
                                    EnhancedHeroCarousel(
                                        channels = filteredChannels.take(5),
                                        onPlayClick = {
                                            viewModel.setSelectedGroup(null)
                                            viewModel.markAsPlayed(it)
                                            onSelectChannel(it)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }

                        if (groups.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    LazyRow(
                                        state = groupLazyListState,
                                        modifier = Modifier
                                            .widthIn(max = 1200.dp)
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        item {
                                            GroupChip(
                                                text = stringResource(R.string.group_all),
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
                    }

                    // Content Area
                    if (selectedGroup != null || searchQuery.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val title = when {
                                    searchQuery.isNotEmpty() -> stringResource(R.string.search_results)
                                    selectedGroup == "Favorit" -> stringResource(R.string.row_favorite)
                                    selectedGroup == "Terakhir Ditonton" -> stringResource(R.string.row_history)
                                    selectedGroup != null -> selectedGroup!!
                                    else -> ""
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                                )
                            }
                        }

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
                                            .widthIn(max = 600.dp)
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
                                            stringResource(R.string.search_not_found_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            stringResource(R.string.search_not_found_msg),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            val columns = when (windowSize.widthSizeClass) {
                                WindowWidthSizeClass.Compact -> 2
                                WindowWidthSizeClass.Medium -> 3
                                else -> 4
                            }
                            val chunkedChannels = filteredChannels.chunked(columns)
                            items(chunkedChannels) { chunk ->
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Row(
                                        modifier = Modifier
                                            .widthIn(max = 1200.dp)
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
                                        repeat(columns - chunk.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val onChannelClick: (IptvChannel, String?) -> Unit = { channel, _ ->
                            viewModel.markAsPlayed(channel)
                            onSelectChannel(channel)
                        }

                        if (favoriteChannels.isNotEmpty()) {
                            item {
                                ContentRow(
                                    viewModel = viewModel,
                                    title = stringResource(R.string.row_favorite),
                                    channels = favoriteChannels,
                                    onSeeAllClick = { viewModel.setSelectedGroup(favGroup) },
                                    onChannelSelected = { onChannelClick(it, favGroup) }
                                )
                            }
                        }

                        if (recentlyPlayed.isNotEmpty()) {
                            item {
                                ContentRow(
                                    viewModel = viewModel,
                                    title = stringResource(R.string.row_history),
                                    channels = recentlyPlayed,
                                    onSeeAllClick = { viewModel.setSelectedGroup(historyGroup) },
                                    onChannelSelected = { onChannelClick(it, historyGroup) }
                                )
                            }
                        }

                        groups.take(20).forEach { group ->
                            val groupChannels = allChannels.filter { 
                                val matchesPlaylist = selectedPlaylistId == null || it.playlistId == selectedPlaylistId
                                matchesPlaylist && it.group == group 
                            }
                            if (groupChannels.isNotEmpty()) {
                                item(key = group) {
                                    ContentRow(
                                        viewModel = viewModel,
                                        title = group,
                                        channels = groupChannels,
                                        onSeeAllClick = { viewModel.setSelectedGroup(group) },
                                        onChannelSelected = { onChannelClick(it, group) }
                                    )
                                }
                            }
                        }

                        if (groups.size > 15) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 1200.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                RoundedCornerShape(24.dp)
                                            )
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                RoundedCornerShape(24.dp)
                                            )
                                            .padding(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    stringResource(R.string.other_groups).uppercase(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 1.5.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .width(24.dp)
                                                        .height(2.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                )
                                            }
                                            Surface(
                                                onClick = { isGroupsExpanded = !isGroupsExpanded },
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        if (isGroupsExpanded) stringResource(R.string.see_less) else stringResource(R.string.see_all),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Icon(
                                                        if (isGroupsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))

                                        val remainingGroups = if (isGroupsExpanded) groups.drop(15) else groups.drop(15).take(6)
                                        val otherGroupColumns = when (windowSize.widthSizeClass) {
                                            WindowWidthSizeClass.Compact -> 2
                                            WindowWidthSizeClass.Medium -> 3
                                            else -> 4
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            remainingGroups.chunked(otherGroupColumns).forEach { chunk ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    chunk.forEach { group ->
                                                        Surface(
                                                            onClick = { viewModel.setSelectedGroup(group) },
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .background(MaterialTheme.colorScheme.primary.copy(0.12f), CircleShape),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Folder,
                                                                        null,
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Text(
                                                                    group,
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                    repeat(otherGroupColumns - chunk.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
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

            // SIMPLE SHUTDOWN ANIMATION OVERLAY
            if (isShuttingDown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = shutdownProgress.value))
                        .zIndex(1000f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.departure_msg).uppercase(),
                        color = Color.White.copy(alpha = (shutdownProgress.value * 2f).coerceIn(0f, 1f)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
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
                            contentDescription = stringResource(R.string.content_desc_scroll_top),
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
    val carouselHeight = (configuration.screenHeightDp * 0.25f).coerceAtMost(210f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(carouselHeight),
        contentAlignment = Alignment.Center
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
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .fillMaxSize(),
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
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    clip = true
                                ),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                        ) {
                            AsyncImage(
                                model = channel.logo,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                contentScale = ContentScale.Fit,
                                error = painterResource(R.drawable.app_icon_android),
                                placeholder = painterResource(R.drawable.app_icon_android)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = (channel.group?.uppercase() ?: stringResource(R.string.carousel_rec)),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 18.sp,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onPlayClick(channel) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.carousel_watch_now),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
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
                .padding(bottom = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val displayCount = channels.size.coerceAtMost(8)
                repeat(displayCount) { iteration ->
                    val isSelected = pagerState.currentPage % displayCount == iteration
                    val indicatorColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    val indicatorWidth = if (isSelected) 20.dp else 6.dp

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                            .width(indicatorWidth)
                            .height(6.dp)
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
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var localSearchQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery) {
        localSearchQuery = searchQuery
    }

    Surface(
        color = if (isSearchExpanded) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemInDarkTheme()) alpha.coerceIn(0f, 0.95f) else (alpha * 0.9f).coerceIn(0f, 0.98f)),
        modifier = modifier.fillMaxWidth(),
        tonalElevation = if (isSystemInDarkTheme()) 4.dp else 1.dp,
        shadowElevation = if (alpha > 0.5f) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1200.dp)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSearchExpanded) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(Icons.Default.Menu, stringResource(R.string.content_desc_menu), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Image(
                        painter = painterResource(id = R.drawable.app_icon_android),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.brand_name),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            stringResource(R.string.brand_slogan),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderActionButton(Icons.Default.Search, stringResource(R.string.content_desc_search)) { isSearchExpanded = true }
                        HeaderActionButton(Icons.Default.Refresh, stringResource(R.string.content_desc_refresh)) { onRefresh() }
                    }
                } else {
                    TextField(
                        value = localSearchQuery,
                        onValueChange = {
                            localSearchQuery = it
                            onSearchQueryChange(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder = {
                            Text(
                                stringResource(R.string.search_placeholder),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (localSearchQuery.isNotEmpty()) {
                                        localSearchQuery = ""
                                        onSearchQueryChange("")
                                    } else {
                                        isSearchExpanded = false
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = if (localSearchQuery.isNotEmpty())
                                        stringResource(R.string.content_desc_close_search)
                                    else stringResource(R.string.content_desc_close_search),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
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
}

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val finalContainerColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else containerColor
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(finalContainerColor)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
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
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.widthIn(max = 1200.dp).padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Scroll Affordance Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(width = (4 + (i * 2)).dp, height = 2.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f + (i * 0.2f)),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color.Transparent)
                                ),
                                CircleShape
                            )
                            .alpha(if (isSystemInDarkTheme()) 1f else 0.7f)
                    )
                }
                Surface(
                    onClick = onSeeAllClick,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.07f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.row_see_all, channels.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels, key = { it.url }) { channel ->
                        ChannelModernItem(
                            viewModel = viewModel,
                            channel = channel,
                            modifier = Modifier.width(160.dp),
                            onClick = { onChannelSelected(channel) }
                        )
                    }
                }
                
                // Subtle edge fade to indicate more content
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(32.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
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
        targetValue = if (isPressed) 0.96f else 1f,
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
                    elevation = if (isPressed) 2.dp else 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(
                0.5.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
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
                            .padding(6.dp),
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
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Live Badge
                if (currentProgram != null) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            stringResource(R.string.status_live),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp
                        )
                    }
                }

                if (!channel.drmConfig.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = CircleShape,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.content_desc_drm),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        val programText = currentProgram?.title ?: stringResource(R.string.no_program_info)
        Text(
            text = programText,
            color = if (currentProgram != null)
                MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (currentProgram != null) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 2.dp)
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
            .height(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        label = "content"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
