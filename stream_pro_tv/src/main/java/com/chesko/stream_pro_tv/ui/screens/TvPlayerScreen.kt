package com.chesko.stream_pro_tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn as AndroidOptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.*
import androidx.compose.foundation.focusGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.chesko.stream_pro.core.data.model.EpgProgram
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.PlayerUtils
import com.chesko.stream_pro_tv.player.TvVideoPlayer
import com.chesko.stream_pro_tv.ui.components.PositionFocusedItemInLazyLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerScreen(
    viewModel: MainViewModel,
    channel: IptvChannel,
    networkStatus: com.chesko.stream_pro.core.utils.NetworkObserver.NetworkStatus,
    onBack: () -> Unit
) {
    val allChannels by viewModel.filteredChannels.collectAsState()
    var currentChannel by remember { mutableStateOf(channel) }
    var showOverlay by remember { mutableStateOf(true) }
    var showZappingInfo by remember { mutableStateOf(false) }
    var zappingJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_BUFFERING) }
    val playButtonFocusRequester = remember { FocusRequester() }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var currentSidebarView by remember { mutableStateOf("MAIN") }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(showSettingsDialog, showOverlay) {
        if (!showSettingsDialog) {
            currentSidebarView = "MAIN"
            if (showOverlay) {
                repeat(10) { i ->
                    delay(50L)
                    try {
                        playButtonFocusRequester.requestFocus()
                    } catch (e: Exception) { }
                }
            }
        }
    }


    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val navigableChannels = remember(allChannels, selectedCategory) {
        if (selectedCategory == null) allChannels
        else allChannels.filter { it.group == selectedCategory }
    }

    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val isFavorite = remember(currentChannel.url, favoriteChannels) {
        favoriteChannels.any { it.url == currentChannel.url }
    }

    val currentProgram by viewModel.getCurrentProgram(currentChannel)
        .collectAsState(initial = null)
    val nextProgram by viewModel.getNextProgram(currentChannel).collectAsState(initial = null)

    val isAnyDialogOpen = showSettingsDialog

    LaunchedEffect(showOverlay, lastInteraction, isAnyDialogOpen) {
        if (showOverlay && !isAnyDialogOpen) {
            delay(5000)
            showOverlay = false
        }
    }

    val changeChannel = { offset: Int ->
        val index = navigableChannels.indexOfFirst { it.url == currentChannel.url }
        if (index != -1) {
            val nextIndex = (index + offset).let {
                if (it < 0) navigableChannels.size - 1
                else if (it >= navigableChannels.size) 0
                else it
            }
            currentChannel = navigableChannels[nextIndex]

            if (!showOverlay) {
                showZappingInfo = true
                zappingJob?.cancel()
                zappingJob = scope.launch {
                    delay(1000)
                    showZappingInfo = false
                }
            }

            lastInteraction = System.currentTimeMillis()
        }
    }

    LaunchedEffect(currentChannel) {
        lastInteraction = System.currentTimeMillis()
    }

    val resetTimer = { lastInteraction = System.currentTimeMillis() }
    val resetTimerRef = remember { resetTimer }

    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }

    val reloadVideo: () -> Unit = {
        exoPlayer?.let {
            it.stop()
            it.prepare()
            it.play()
        }
    }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                isPlaying = isPlayingParam
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
        }

        player.addListener(listener)
        isPlaying = player.playWhenReady
        playbackState = player.playbackState

        try {
            var bufferCount = 0
            while (true) {
                if (showOverlay) {
                    playbackPosition = player.currentPosition
                    playbackDuration = player.duration
                }
                
                if (player.playbackState == Player.STATE_BUFFERING) {
                    bufferCount++
                } else {
                    bufferCount = 0
                }

                delay(1000)
            }
        } finally {
            player.removeListener(listener)
        }
    }

    val hwAcceleration by viewModel.hwAcceleration.collectAsState()
    val autoQuality by viewModel.autoQuality.collectAsState()
    val audioBoost by viewModel.audioBoost.collectAsState()
    val maxVideoHeight by viewModel.maxVideoHeight.collectAsState()
    val bufferSize by viewModel.bufferSize.collectAsState()

    TvPlayerScreenContent(
        currentChannel = currentChannel,
        navigableChannels = navigableChannels,
        isFavorite = isFavorite,
        currentProgram = currentProgram,
        nextProgram = nextProgram,
        showOverlay = showOverlay,
        showZappingInfo = showZappingInfo,
        showSettingsDialog = showSettingsDialog,
        currentSidebarView = currentSidebarView,
        playbackState = playbackState,
        isPlaying = isPlaying,
        playbackPosition = playbackPosition,
        playbackDuration = playbackDuration,
        exoPlayer = exoPlayer,
        onBack = onBack,
        onToggleOverlay = { showOverlay = !showOverlay },
        onInteraction = resetTimerRef,
        onChangeChannel = changeChannel,
        onToggleFavorite = { viewModel.toggleFavorite(currentChannel) },
        onReloadVideo = reloadVideo,
        onTogglePlayPause = {
            exoPlayer?.let { if (it.playWhenReady) it.pause() else it.play() }
        },
        onSettingsClick = {
            currentSidebarView = "MAIN"
            showSettingsDialog = true
        },
        onChannelSelected = { selected ->
            currentChannel = selected
            showSettingsDialog = false
            showOverlay = true
            lastInteraction = System.currentTimeMillis()
        },
        onSidebarViewChange = { currentSidebarView = it },
        onDismissSettings = { showSettingsDialog = false },
        onPlayerInit = { exoPlayer = it },
        onPlayerError = { _ ->
            playbackState = Player.STATE_IDLE
        },
        onSeek = { offsetMs ->
            exoPlayer?.let {
                if (it.isCurrentMediaItemSeekable) {
                    val target = (it.currentPosition + offsetMs).coerceIn(0, if (it.duration > 0) it.duration else Long.MAX_VALUE)
                    it.seekTo(target)
                    playbackPosition = it.currentPosition
                }
            }
        },
        hwAcceleration = hwAcceleration,
        autoQuality = autoQuality,
        audioBoost = audioBoost,
        onToggleAudioBoost = { viewModel.setAudioBoost(!audioBoost) },
        maxVideoHeight = maxVideoHeight,
        bufferSize = bufferSize,
        playButtonFocusRequester = playButtonFocusRequester
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerScreenContent(
    currentChannel: IptvChannel,
    navigableChannels: List<IptvChannel>,
    isFavorite: Boolean,
    currentProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    showOverlay: Boolean,
    showZappingInfo: Boolean,
    showSettingsDialog: Boolean,
    currentSidebarView: String,
    playbackState: Int,
    isPlaying: Boolean,
    playbackPosition: Long,
    playbackDuration: Long,
    exoPlayer: ExoPlayer?,
    onBack: () -> Unit,
    onToggleOverlay: () -> Unit,
    onInteraction: () -> Unit,
    onChangeChannel: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onReloadVideo: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSettingsClick: () -> Unit,
    onChannelSelected: (IptvChannel) -> Unit,
    onSidebarViewChange: (String) -> Unit,
    onDismissSettings: () -> Unit,
    onPlayerInit: (ExoPlayer) -> Unit,
    onPlayerError: (String) -> Unit,
    onSeek: (Long) -> Unit,
    hwAcceleration: Boolean = true,
    autoQuality: Boolean = true,
    audioBoost: Boolean = false,
    onToggleAudioBoost: () -> Unit = {},
    maxVideoHeight: Int = 0,
    bufferSize: Int = 15,
    playButtonFocusRequester: FocusRequester
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    BackHandler {
        if (showSettingsDialog) {
            if (currentSidebarView != "MAIN") {
                onSidebarViewChange("MAIN")
            } else {
                onDismissSettings()
            }
        } else if (showOverlay) {
            onToggleOverlay()
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    if (showOverlay) {
                        onInteraction()
                    }

                    when (it.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            if (!showOverlay) {
                                onToggleOverlay()
                                onInteraction()
                                true
                            } else false
                        }

                        Key.DirectionUp -> {
                            if (!showOverlay) {
                                onChangeChannel(1)
                                true
                            } else false
                        }

                        Key.DirectionDown -> {
                            if (!showOverlay) {
                                onChangeChannel(-1)
                                true
                            } else false
                        }

                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (!showOverlay) {
                                onToggleOverlay()
                                onInteraction()
                                true
                            } else {
                                false
                            }
                        }

                        Key.MediaPlayPause -> {
                            onTogglePlayPause()
                            if (!showOverlay) onToggleOverlay()
                            onInteraction()
                            true
                        }

                        Key.MediaPlay -> {
                            if (!isPlaying) onTogglePlayPause()
                            if (!showOverlay) onToggleOverlay()
                            onInteraction()
                            true
                        }

                        Key.MediaPause -> {
                            if (isPlaying) onTogglePlayPause()
                            if (!showOverlay) onToggleOverlay()
                            onInteraction()
                            true
                        }

                        Key.MediaRewind -> {
                            onSeek(-10000L)
                            if (!showOverlay) onToggleOverlay()
                            onInteraction()
                            true
                        }

                        Key.MediaFastForward -> {
                            onSeek(10000L)
                            if (!showOverlay) onToggleOverlay()
                            onInteraction()
                            true
                        }

                        Key.MediaNext -> {
                            onChangeChannel(1)
                            onInteraction()
                            true
                        }

                        Key.MediaPrevious -> {
                            onChangeChannel(-1)
                            onInteraction()
                            true
                        }

                        Key.Back, Key.Escape -> {
                            if (showSettingsDialog) {
                                if (currentSidebarView != "MAIN") {
                                    onSidebarViewChange("MAIN")
                                } else {
                                    onDismissSettings()
                                }
                                true
                            } else if (showOverlay) {
                                onToggleOverlay()
                                true
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
                } else false
            }
            .clickable { onToggleOverlay() }
            .focusable()
    ) {
        TvVideoPlayer(
            channel = currentChannel,
            modifier = Modifier.fillMaxSize(),
            hwAcceleration = hwAcceleration,
            autoQuality = autoQuality,
            audioBoost = audioBoost,
            maxVideoHeight = maxVideoHeight,
            bufferSize = bufferSize,
            onPlayerInit = onPlayerInit,
            onError = onPlayerError
        )

        AnimatedVisibility(
            visible = !showOverlay && !showSettingsDialog,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val progress = remember(playbackPosition, playbackDuration, currentProgram) {
                if (playbackDuration > 0) {
                    (playbackPosition.toFloat() / playbackDuration.toFloat()).coerceIn(0f, 1f)
                } else if (currentProgram != null) {
                    val total = currentProgram.endTime - currentProgram.startTime
                    val current = System.currentTimeMillis() - currentProgram.startTime
                    if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
                } else 0f
            }

            val isBuffering = playbackState == Player.STATE_BUFFERING

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isBuffering) {
                    Text(
                        text = stringResource(R.string.player_syncing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = if (isSmallScreen) 7.sp else 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Program Title Mini-Info
                if (currentProgram != null) {
                    Text(
                        text = currentProgram.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = if (isSmallScreen) 7.sp else 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSmallScreen) 2.dp else 3.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.colorScheme.primary)
                                )
                            )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showZappingInfo && !showOverlay,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        ) {
            ZappingInfoBar(channel = currentChannel)
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.focusProperties {
                canFocus = !showSettingsDialog
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isSmallScreen) 16.dp else 32.dp,
                            vertical = if (isSmallScreen) 12.dp else 20.dp
                        )
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TvHeaderButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = stringResource(R.string.player_back),
                        onInteraction = onInteraction,
                        onClick = onBack
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(if (isSmallScreen) 36.dp else 48.dp)
                                .clip(RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp))
                                .padding(horizontal = if (isSmallScreen) 12.dp else 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = currentChannel.logo,
                                contentDescription = null,
                                placeholder = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                                error = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                                modifier = Modifier
                                    .size(if (isSmallScreen) 32.dp else 44.dp, if (isSmallScreen) 20.dp else 28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(if (isSmallScreen) 12.dp else 20.dp))
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = currentChannel.name.uppercase(),
                                    style = if (isSmallScreen) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    letterSpacing = 1.sp
                                )
                                currentChannel.group?.let {
                                    Text(
                                        text = it.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isSmallScreen) 7.sp else 9.sp,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    TvHeaderButton(
                        icon = Icons.Default.Menu,
                        label = stringResource(R.string.player_menu),
                        onInteraction = onInteraction,
                        onClick = onSettingsClick
                    )
                }

                val isError = (playbackState == Player.STATE_IDLE && exoPlayer?.playerError != null) || 
                             (playbackState == Player.STATE_IDLE && !isPlaying) ||
                             (playbackState == Player.STATE_ENDED)

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 24.dp)
                ) {
                    TvPlayerCenterAction(
                        icon = Icons.Default.SkipPrevious,
                        label = stringResource(R.string.player_prev),
                        onInteraction = onInteraction,
                        onClick = { onChangeChannel(-1) },
                        size = if (isSmallScreen) 28.dp else 36.dp,
                        iconSize = if (isSmallScreen) 14.dp else 18.dp
                    )

                    TvPlayerCenterAction(
                        icon = Icons.Default.Replay10,
                        label = stringResource(R.string.player_rewind),
                        onInteraction = onInteraction,
                        onClick = { onSeek(-10000L) },
                        size = if (isSmallScreen) 28.dp else 36.dp,
                        iconSize = if (isSmallScreen) 14.dp else 18.dp
                    )

                    Box(contentAlignment = Alignment.Center) {
                        val isBuffering = playbackState == Player.STATE_BUFFERING
                        
                        if (isBuffering || isError) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(if (isSmallScreen) 54.dp else 64.dp),
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }

                        TvPlayerCenterAction(
                            icon = when {
                                isError -> Icons.Default.Refresh
                                isPlaying -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            },
                            label = when {
                                isError -> stringResource(R.string.player_reload)
                                isPlaying -> stringResource(R.string.player_pause)
                                else -> stringResource(R.string.player_play)
                            },
                            iconSize = if (isSmallScreen) 20.dp else 24.dp,
                            modifier = Modifier
                                .size(if (isSmallScreen) 40.dp else 48.dp)
                                .focusRequester(playButtonFocusRequester),
                            onInteraction = onInteraction,
                            onClick = {
                                if (isError) {
                                    onReloadVideo()
                                } else {
                                    onTogglePlayPause()
                                }
                            }
                        )
                    }

                    TvPlayerCenterAction(
                        icon = Icons.Default.Forward10,
                        label = stringResource(R.string.player_forward),
                        onInteraction = onInteraction,
                        onClick = { onSeek(10000L) },
                        size = if (isSmallScreen) 28.dp else 36.dp,
                        iconSize = if (isSmallScreen) 14.dp else 18.dp
                    )

                    TvPlayerCenterAction(
                        icon = Icons.Default.SkipNext,
                        label = stringResource(R.string.player_next),
                        onInteraction = onInteraction,
                        onClick = { onChangeChannel(1) },
                        size = if (isSmallScreen) 28.dp else 36.dp,
                        iconSize = if (isSmallScreen) 14.dp else 18.dp
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = if (isSmallScreen) 16.dp else 40.dp,
                            vertical = if (isSmallScreen) 12.dp else 24.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 16.dp)
                ) {
                    // Modern Integrated EPG & Seekbar Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (isSmallScreen) 16.dp else 24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(if (isSmallScreen) 16.dp else 24.dp))
                            .padding(
                                horizontal = if (isSmallScreen) 16.dp else 28.dp,
                                vertical = if (isSmallScreen) 10.dp else 20.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (playbackDuration > 0) {
                            // VOD MODE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.player_now_streaming),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        fontSize = if (isSmallScreen) 8.sp else 10.sp
                                    )
                                    Text(
                                        text = currentChannel.name.uppercase(),
                                        style = if (isSmallScreen) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${PlayerUtils.formatTime(playbackPosition)} / ${PlayerUtils.formatTime(playbackDuration)}",
                                    style = if (isSmallScreen) MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp) else MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (currentProgram != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.player_current_program),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        fontSize = if (isSmallScreen) 8.sp else 10.sp
                                    )
                                    Text(
                                        text = currentProgram.title.uppercase(),
                                        style = if (isSmallScreen) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${timeFormat.format(Date(currentProgram.startTime))} - ${timeFormat.format(Date(currentProgram.endTime))}",
                                        style = if (isSmallScreen) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (nextProgram != null) {
                                    Spacer(Modifier.width(16.dp))
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.player_coming_up),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            fontSize = if (isSmallScreen) 7.sp else 9.sp
                                        )
                                        Text(
                                            text = nextProgram.title.uppercase(),
                                            style = if (isSmallScreen) MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp) else MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = timeFormat.format(Date(nextProgram.startTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = if (isSmallScreen) 7.sp else 8.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.player_no_program),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 8.dp else 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val progress = if (playbackDuration > 0) {
                                (playbackPosition.toFloat() / playbackDuration.toFloat()).coerceIn(0f, 1f)
                            } else if (currentProgram != null) {
                                val total = currentProgram.endTime - currentProgram.startTime
                                val current = System.currentTimeMillis() - currentProgram.startTime
                                if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isSmallScreen) 3.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(if (isSmallScreen) 3.dp else 4.dp)
                                    .align(Alignment.CenterStart)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), MaterialTheme.colorScheme.primary)
                                        )
                                    )
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSettingsDialog,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.65f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.5f)
                                )
                            )
                            .clickable {
                                onDismissSettings()
                            }
                    )
                }

                AnimatedVisibility(
                    visible = showSettingsDialog,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(500)
                    ) + fadeIn(animationSpec = tween(500)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(500)
                    ) + fadeOut(animationSpec = tween(500)),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    TvSettingsSidebar(
                        exoPlayer = exoPlayer,
                        isFavorite = isFavorite,
                        audioBoost = audioBoost,
                        onToggleAudioBoost = onToggleAudioBoost,
                        onToggleFavorite = onToggleFavorite,
                        channels = navigableChannels,
                        currentChannel = currentChannel,
                        onChannelSelected = onChannelSelected,
                        currentView = currentSidebarView,
                        onViewChange = onSidebarViewChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsSidebar(
    exoPlayer: ExoPlayer?,
    isFavorite: Boolean,
    audioBoost: Boolean,
    onToggleAudioBoost: () -> Unit,
    onToggleFavorite: () -> Unit,
    channels: List<IptvChannel>,
    currentChannel: IptvChannel,
    onChannelSelected: (IptvChannel) -> Unit,
    currentView: String,
    onViewChange: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600
    
    val channelFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }
    val qualityFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }

    var lastMainFocus by remember { mutableStateOf("CHANNELS") }

    LaunchedEffect(currentView) {
        if (currentView == "MAIN") {
            delay(100)
            when (lastMainFocus) {
                "CHANNELS" -> channelFocusRequester.requestFocus()
                "FAVORITE" -> favoriteFocusRequester.requestFocus()
                "QUALITY" -> qualityFocusRequester.requestFocus()
                "AUDIO" -> audioFocusRequester.requestFocus()
                else -> channelFocusRequester.requestFocus()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                vertical = if (isSmallScreen) 12.dp else 24.dp, 
                horizontal = if (isSmallScreen) 12.dp else 24.dp
            )
            .fillMaxWidth(if (isSmallScreen) 0.7f else 0.22f)
            .clip(RoundedCornerShape(if (isSmallScreen) 16.dp else 24.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(if (isSmallScreen) 12.dp else 16.dp)
            .focusGroup()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when(currentView) {
                        "MAIN" -> stringResource(R.string.player_menu)
                        "QUALITY" -> stringResource(R.string.player_sidebar_quality)
                        "AUDIO" -> stringResource(R.string.player_sidebar_audio)
                        "CHANNELS" -> stringResource(R.string.player_sidebar_channels)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    fontSize = if (isSmallScreen) 9.sp else 10.sp
                )
            }

            Crossfade(
                targetState = currentView, 
                label = "SidebarNavigation",
                animationSpec = tween(300)
            ) { view ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (view) {
                        "MAIN" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SidebarItem(
                                    label = stringResource(R.string.player_sidebar_channels),
                                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    onClick = { onViewChange("CHANNELS") },
                                    modifier = Modifier
                                        .focusRequester(channelFocusRequester)
                                        .onFocusChanged { if (it.isFocused) lastMainFocus = "CHANNELS" }
                                )
                                SidebarItem(
                                    label = if (isFavorite) stringResource(R.string.player_sidebar_favorite_remove) else stringResource(R.string.player_sidebar_favorite_add),
                                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    onClick = onToggleFavorite,
                                    isFavorite = true,
                                    modifier = Modifier
                                        .focusRequester(favoriteFocusRequester)
                                        .onFocusChanged { if (it.isFocused) lastMainFocus = "FAVORITE" }
                                )
                                SidebarItem(
                                    label = stringResource(R.string.player_sidebar_quality),
                                    icon = Icons.Default.HighQuality,
                                    onClick = { onViewChange("QUALITY") },
                                    modifier = Modifier
                                        .focusRequester(qualityFocusRequester)
                                        .onFocusChanged { if (it.isFocused) lastMainFocus = "QUALITY" }
                                )
                                SidebarItem(
                                    label = stringResource(R.string.player_sidebar_audio),
                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                    onClick = { onViewChange("AUDIO") },
                                    modifier = Modifier
                                        .focusRequester(audioFocusRequester)
                                        .onFocusChanged { if (it.isFocused) lastMainFocus = "AUDIO" }
                                )
                            }
                        }
                        "QUALITY" -> {
                            TrackSelectionList(
                                exoPlayer = exoPlayer,
                                trackType = androidx.media3.common.C.TRACK_TYPE_VIDEO,
                                onSelection = { onViewChange("MAIN") }
                            )
                        }
                        "AUDIO" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SidebarItem(
                                    label = stringResource(
                                        R.string.player_sidebar_audio_boost,
                                        if (audioBoost) stringResource(R.string.label_on) else stringResource(R.string.label_off)
                                    ),
                                    icon = if (audioBoost) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                                    onClick = onToggleAudioBoost,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.player_sidebar_audio_tracks),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TrackSelectionList(
                                    exoPlayer = exoPlayer,
                                    trackType = androidx.media3.common.C.TRACK_TYPE_AUDIO,
                                    onSelection = { onViewChange("MAIN") }
                                )
                            }
                        }
                        "CHANNELS" -> {
                            ChannelListSidebar(
                                channels = channels,
                                currentChannel = currentChannel,
                                onChannelSelected = onChannelSelected
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelListSidebar(
    channels: List<IptvChannel>,
    currentChannel: IptvChannel,
    onChannelSelected: (IptvChannel) -> Unit
) {
    val scrollState = rememberLazyListState()
    val currentIndex = channels.indexOfFirst { it.url == currentChannel.url }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        scrollState.scrollToItem(currentIndex)
    }

    PositionFocusedItemInLazyLayout(parentFraction = 0.5f) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(channels.size) { index ->
                val channel = channels[index]
                val isCurrent = index == currentIndex
                val itemFocusRequester = remember { FocusRequester() }

                Surface(
                    onClick = { onChannelSelected(channel) },
                    modifier = Modifier
                        .focusRequester(itemFocusRequester)
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White,
                        contentColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = CircleShape
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                    ) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            placeholder = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                            error = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                            modifier = Modifier
                                .size(24.dp, 14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent) {
                            Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (isCurrent) {
                    LaunchedEffect(Unit) { itemFocusRequester.requestFocus() }
                }
            }
        }
    }
}



@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isFavorite: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White,
            contentColor = if (isFavorite && (label.startsWith("Remove") || label.startsWith("Hapus"))) Color.Red else Color.White,
            focusedContentColor = if (isFavorite && (label.startsWith("Remove") || label.startsWith("Hapus"))) Color.Red else Color.Black
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = CircleShape
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@AndroidOptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrackSelectionList(
    exoPlayer: ExoPlayer?,
    trackType: Int,
    onSelection: () -> Unit,
    backFocusRequester: FocusRequester? = null
) {
    if (exoPlayer == null) return
    val tracks = exoPlayer.currentTracks
    val groups = tracks.groups.filter { it.type == trackType }

    val hasOverride = remember(exoPlayer.trackSelectionParameters, tracks) {
        exoPlayer.trackSelectionParameters.overrides.keys.any { group ->
            tracks.groups.any { it.mediaTrackGroup == group && it.type == trackType }
        }
    }

    val trackItems = remember(groups) {
        val list = mutableListOf<Pair<androidx.media3.common.Tracks.Group, Int>>()
        groups.forEach { group ->
            for (i in 0 until group.length) {
                list.add(group to i)
            }
        }
        list
    }

    val scrollState = rememberLazyListState()
    val initialIndex = remember(trackItems, hasOverride) {
        if (trackType == C.TRACK_TYPE_VIDEO) {
            if (!hasOverride) 0
            else {
                val idx = trackItems.indexOfFirst { it.first.isTrackSelected(it.second) }
                if (idx != -1) idx + 1 else 0
            }
        } else {
            val idx = trackItems.indexOfFirst { it.first.isTrackSelected(it.second) }
            if (idx != -1) idx else 0
        }
    }

    LaunchedEffect(Unit) {
        scrollState.scrollToItem(initialIndex)
    }

    PositionFocusedItemInLazyLayout(parentFraction = 0.5f) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (trackType == C.TRACK_TYPE_VIDEO) {
                val isAutoSelected = !hasOverride
                item {
                    TrackItem(
                        label = stringResource(R.string.player_sidebar_auto),
                        isSelected = isAutoSelected,
                        icon = Icons.Default.AutoFixHigh,
                        onClick = {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(trackType)
                                .build()
                            onSelection()
                        },
                        autoFocus = isAutoSelected,
                        modifier = Modifier.focusProperties {
                            if (backFocusRequester != null) {
                                up = backFocusRequester
                            }
                        }
                    )
                }
            }

            items(trackItems.size) { index ->
                val (group, trackIndex) = trackItems[index]
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex) && hasOverride

                TrackItem(
                    label = buildTrackName(trackType, format, trackIndex),
                    isSelected = isSelected,
                    onClick = {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                            .build()
                        onSelection()
                    },
                    autoFocus = isSelected || (trackType != C.TRACK_TYPE_VIDEO && !hasOverride && index == 0),
                    modifier = if (index == 0 && trackType != C.TRACK_TYPE_AUDIO) {
                        Modifier.focusProperties {
                            if (backFocusRequester != null) {
                                up = backFocusRequester
                            }
                        }
                    } else Modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrackItem(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(36.dp).focusRequester(focusRequester),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = CircleShape
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(14.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@AndroidOptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun buildTrackName(trackType: Int, format: androidx.media3.common.Format, index: Int): String {
    return when (trackType) {
        C.TRACK_TYPE_VIDEO -> {
            val height = if (format.height != -1) "${format.height}p" else ""
            val frameRate = if (format.frameRate > 0) " ${format.frameRate.toInt()}fps" else ""
            val label = format.label ?: height
            if (label.isEmpty()) stringResource(R.string.player_sidebar_quality_label, index + 1) else "$label$frameRate"
        }
        C.TRACK_TYPE_AUDIO -> {
            val lang = format.language?.uppercase() ?: stringResource(R.string.player_sidebar_audio_label)
            val label = format.label?.let { " - $it" } ?: ""
            val channels = if (format.channelCount != -1) " (${format.channelCount}ch)" else ""
            "$lang$label$channels"
        }
        else -> format.label ?: "Track $index"
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ZappingInfoBar(channel: IptvChannel) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp, 36.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                placeholder = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                error = painterResource(com.chesko.stream_pro_tv.R.drawable.app_icon_androidtv),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(18.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            channel.group?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHeaderButton(
    icon: ImageVector,
    label: String,
    onInteraction: () -> Unit = {},
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600

    Surface(
        onClick = {
            onInteraction()
            onClick()
        },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        modifier = Modifier
            .onFocusChanged { if (it.isFocused) onInteraction() }
    ) {
        Box(modifier = Modifier.size(if (isSmallScreen) 28.dp else 36.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(if (isSmallScreen) 14.dp else 16.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerCenterAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp
) {
    Surface(
        onClick = {
            onInteraction()
            onClick()
        },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.25f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                elevation = 20.dp
            )
        ),
        modifier = modifier
            .size(size)
            .onFocusChanged { if (it.isFocused) onInteraction() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize))
        }
    }
}
