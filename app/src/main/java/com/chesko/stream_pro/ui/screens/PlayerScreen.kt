package com.chesko.stream_pro.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn as AndroidOptIn
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.player.VideoPlayer
import android.content.ClipData
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import kotlinx.coroutines.launch
import androidx.compose.ui.text.AnnotatedString
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.PlayerUtils
import com.chesko.stream_pro.core.utils.NetworkObserver
import com.chesko.stream_pro.ui.components.VlcVideoPlayer
import com.chesko.stream_pro.ui.components.shimmerEffect
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.pm.ApplicationInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private fun isDebugMode(context: Context): Boolean {
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@AndroidOptIn(UnstableApi::class)
@Composable
fun ChannelInfoBar(
    currentChannel: IptvChannel,
    currentProgram: com.chesko.stream_pro.core.data.model.EpgProgram?,
    nextProgram: com.chesko.stream_pro.core.data.model.EpgProgram?,
    showFullControls: Boolean,
    isFullscreen: Boolean = false,
    exoPlayer: ExoPlayer?,
    vlcPlayer: org.videolan.libvlc.MediaPlayer?,
    onSeek: (Long) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(exoPlayer, vlcPlayer) {
        while (true) {
            if (!isDragging) {
                if (exoPlayer != null) {
                    playbackPosition = exoPlayer.currentPosition
                    playbackDuration = exoPlayer.duration
                } else if (vlcPlayer != null) {
                    playbackPosition = vlcPlayer.time
                    playbackDuration = vlcPlayer.length
                }
            }
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (showFullControls) (if (isFullscreen) 44.dp else 54.dp) else 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    AsyncImage(
                        model = currentChannel.logo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.app_icon_android),
                        placeholder = painterResource(R.drawable.app_icon_android)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (currentProgram != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.player_on_air),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                        Text(
                            currentProgram.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            currentChannel.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Next Program Info
            if (nextProgram != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.player_next, nextProgram.title),
                            color = Color.White.copy(0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = timeFormatter.format(Date(nextProgram.startTime)),
                            color = Color.White.copy(0.4f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar (VOD or EPG)
            if (playbackDuration > 0) {
                Column {
                    val currentProgress = if (isDragging) dragPosition else (playbackPosition.toFloat() / playbackDuration.toFloat()).coerceIn(0f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .pointerInput(playbackDuration) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        dragPosition = (offset.x / size.width).coerceIn(0f, 1f)
                                    },
                                    onDragEnd = {
                                        onSeek((dragPosition * playbackDuration).toLong())
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                                    }
                                )
                            }
                            .pointerInput(playbackDuration) {
                                detectTapGestures { offset ->
                                    val newPos = (offset.x / size.width).coerceIn(0f, 1f)
                                    onSeek((newPos * playbackDuration).toLong())
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            PlayerUtils.formatTime(if (isDragging) (dragPosition * playbackDuration).toLong() else playbackPosition),
                            color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            PlayerUtils.formatTime(playbackDuration),
                            color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            } else if (currentProgram != null) {
                val currentTime = System.currentTimeMillis()
                val progress = ((currentTime - currentProgram.startTime).toFloat() /
                               (currentProgram.endTime - currentProgram.startTime).toFloat()).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(0.1f)
                )
            }
        }
    }
}



@Composable
fun PlayerTheme(accentColor: Color, content: @Composable () -> Unit) {
    val playerColorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        surface = Color(0xFF1A1A1A),
        onSurface = Color(0xFFE1E1E1),
        background = Color(0xFF0A0A0A),
        onBackground = Color(0xFFE1E1E1),
        surfaceVariant = Color(0xFF242424),
        onSurfaceVariant = Color(0xFFBDBDBD),
        primaryContainer = Color(0xFF003D96),
        onPrimaryContainer = Color(0xFFD1E4FF),
        error = Color(0xFFCF6679),
        onError = Color.Black
    )

    MaterialTheme(
        colorScheme = playerColorScheme,
        content = content
    )
}

@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    channel: IptvChannel,
    windowSize: WindowSizeClass,
    onBack: () -> Unit
) {
    val accentColorInt by viewModel.accentColor.collectAsState()
    val accentColor = Color(accentColorInt)
    
    PlayerTheme(accentColor = accentColor) {
        PlayerScreenContent(viewModel, channel, windowSize, onBack)
    }
}

@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreenContent(
    viewModel: MainViewModel,
    channel: IptvChannel,
    windowSize: WindowSizeClass,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var showControls by remember { mutableStateOf(true) }
    var isClosing by remember { mutableStateOf(false) }

    val setSystemBarsVisibility = remember {
        { visible: Boolean ->
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (visible) {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    LaunchedEffect(showControls) {
        setSystemBarsVisibility(showControls)
    }

    DisposableEffect(Unit) {
        onDispose {
            setSystemBarsVisibility(true)
        }
    }

    val isDebug = remember { isDebugMode(context) }

    val allChannels by viewModel.filteredChannels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    var currentChannel by remember { mutableStateOf(channel) }

    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val audioBoost by viewModel.audioBoost.collectAsState()
    val playerEngineSetting by viewModel.playerEngine.collectAsState()

    val isExoOnly = remember(currentChannel.url, currentChannel.drmType, currentChannel.drmConfig) {
        val url = currentChannel.url.lowercase().trim()
        url.contains(".mpd") || url.contains(".m3u8") || 
        !currentChannel.drmType.isNullOrBlank() || 
        !currentChannel.drmConfig.isNullOrBlank()
    }

    val initialEngine = remember(playerEngineSetting, currentChannel.url) { 
        val url = currentChannel.url.lowercase().trim()
        when {
            isExoOnly -> "EXO"
            
            url.startsWith("rtsp://") || url.startsWith("rtmp://") || 
            url.startsWith("udp://") || url.startsWith("rtp://") -> "VLC"

            url.contains(".ts") || url.contains("mpegts") -> "VLC"

            else -> playerEngineSetting
        }
    }

    var activeEngine by remember(initialEngine) { mutableStateOf(initialEngine) }
    
    val hwAcceleration by viewModel.hwAcceleration.collectAsState()
    val bufferSize by viewModel.bufferSize.collectAsState()
    val maxVideoHeight by viewModel.maxVideoHeight.collectAsState()

    val isFavorite by remember(currentChannel.url, favoriteChannels) {
        derivedStateOf {
            val trimmedUrl = currentChannel.url.trim()
            favoriteChannels.any { it.url.trim() == trimmedUrl }
        }
    }

    var isLocked by remember { mutableStateOf(false) }
    var showChannelList by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var vlcPlayer by remember { mutableStateOf<org.videolan.libvlc.MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer?.let {
                it.stop()
                it.release()
            }
            vlcPlayer?.let {
                try {
                    it.stop()
                    it.detachViews()
                } catch (_: Exception) {}
            }
        }
    }

    var brightness by remember {
        val initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        val startVal = if (initialBrightness < 0) {
            try {
                android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f
            } catch (e: Exception) { 0.5f }
        } else initialBrightness
        mutableFloatStateOf(startVal)
    }
    
    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }
    var gestureType by remember { mutableStateOf<String?>(null) }
    
    var seekPosition by remember { mutableLongStateOf(0L) }
    var seekTarget by remember { mutableLongStateOf(0L) }
    var seekDuration by remember { mutableLongStateOf(0L) }

    var isBuffering by remember { mutableStateOf(false) }
    var isPlaybackStuck by remember { mutableStateOf(false) }
    var isPlayingState by remember { mutableStateOf(false) }

    // Robust resource cleanup during engine switching
    LaunchedEffect(activeEngine) {
        if (activeEngine == "VLC") {
            exoPlayer?.let {
                it.stop()
                it.release()
                exoPlayer = null
            }
        } else {
            vlcPlayer?.let {
                try {
                    it.stop()
                    it.detachViews()
                } catch (_: Exception) {}
                vlcPlayer = null
            }
        }
    }

    val currentProgram by viewModel.getCurrentProgram(currentChannel).collectAsState(initial = null)
    val nextProgram by viewModel.getNextProgram(currentChannel).collectAsState(initial = null)

    val currentGroupChannels = remember(currentChannel.group, allChannels) {
        if (currentChannel.group.isNullOrEmpty()) {
            allChannels
        } else {
            allChannels.filter { it.group == currentChannel.group }
        }
    }

    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showChannelInfo by remember { mutableStateOf(false) }

    var zappingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val changeChannel = { nextChan: IptvChannel ->
        zappingJob?.cancel()
        currentChannel = nextChan
        viewModel.markAsPlayed(nextChan)
        
        zappingJob = scope.launch {
            delay(1500)
        }
    }

    val reloadVideo: () -> Unit = {
        if (activeEngine == "VLC") {
            try {
                vlcPlayer?.stop()
                vlcPlayer?.play()
            } catch (_: Exception) {
            }
        } else {
            exoPlayer?.let {
                it.stop()
                it.prepare()
                it.play()
            }
        }
        isPlaybackStuck = false
        isBuffering = true
        scope.launch {
            delay(3000)
            isBuffering = false
        }
    }

    val playerLoadingDefault = stringResource(R.string.player_loading_default)
    var loadingStatus by remember { mutableStateOf(playerLoadingDefault) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val networkStatus by viewModel.networkStatus.collectAsState()
    LaunchedEffect(networkStatus) {
        if (networkStatus is NetworkObserver.NetworkStatus.Available && (isPlaybackStuck || errorMessage != null)) {
            reloadVideo()
        }
    }

    LaunchedEffect(currentChannel.url, activeEngine) {
        errorMessage = null
        isPlaybackStuck = false
        isBuffering = true
        loadingStatus = playerLoadingDefault
        
        showChannelInfo = true
        delay(5000)
        showChannelInfo = false
    }

    LaunchedEffect(isPlayingState) {
        if (isPlayingState) {
            loadingStatus = ""
            isBuffering = false
        }
    }

    val playerLoadingVideo = stringResource(R.string.player_loading_video)
    val playerLoadingStream = stringResource(R.string.player_loading_stream)
    val playerPreparing = stringResource(R.string.player_preparing)
    val playerEnded = stringResource(R.string.player_ended)
    val playerFallbackVlc = stringResource(R.string.player_fallback_vlc)
    val playerErrorLoad = stringResource(R.string.player_error_load)

    DisposableEffect(exoPlayer, currentChannel) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    if (loadingStatus == playerLoadingVideo) loadingStatus = ""
                } else if (player.playbackState == Player.STATE_READY) {
                    loadingStatus = playerLoadingVideo
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlayingState = player.isPlaying
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        if (loadingStatus.isEmpty()) loadingStatus = playerLoadingStream
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        if (player.isPlaying) {
                            loadingStatus = ""
                        }
                        isPlaybackStuck = false
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                        if (loadingStatus.isEmpty()) loadingStatus = playerPreparing
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        loadingStatus = playerEnded
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState = isPlaying
                if (isPlaying) {
                    isBuffering = false
                    loadingStatus = ""
                    isPlaybackStuck = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }

                loadingStatus = playerErrorLoad
                isBuffering = false
                isPlaybackStuck = true
            }
        }

        player.addListener(listener)
        
        val monitorJob = scope.launch {
            var bufferCount = 0
            var lastPosCheck = -1L
            var stuckCount = 0
            
            while (true) {
                delay(2000)
                if (player.playbackState == Player.STATE_BUFFERING) {
                    bufferCount++
                    if (bufferCount >= 5) {
                        isPlaybackStuck = true
                    }
                    if (bufferCount == 8) {
                        reloadVideo()
                    }
                } else if (player.isPlaying && player.playbackState == Player.STATE_READY) {
                    bufferCount = 0
                    val currentPos = player.currentPosition
                    if (currentPos == lastPosCheck && currentPos > 0) {
                        stuckCount++
                        if (stuckCount >= 5) {
                            isPlaybackStuck = true
                        }
                    } else {
                        isPlaybackStuck = false
                        stuckCount = 0
                        lastPosCheck = currentPos
                    }
                } else {
                    bufferCount = 0
                    stuckCount = 0
                }
            }
        }

        onDispose {
            player.removeListener(listener)
            monitorJob.cancel()
        }
    }

    val handleBack = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInteractionTime > 800) {
            lastInteractionTime = currentTime
            if (isFullscreen) {
                isFullscreen = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                isClosing = true
                exoPlayer?.stop()
                vlcPlayer?.stop()
                onBack()
            }
        }
    }

    val toggleFullscreen = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInteractionTime > 1000) {
            lastInteractionTime = currentTime
            isFullscreen = !isFullscreen
            activity?.requestedOrientation = if (isFullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
    BackHandler {
        handleBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(isBuffering, loadingStatus, isPlaybackStuck) {
        if (isBuffering || loadingStatus.isNotEmpty() || isPlaybackStuck) {
            showControls = true
        }
    }

    LaunchedEffect(showControls, isLocked, isBuffering, loadingStatus, isPlaybackStuck, showChannelList, showAudioDialog, showSubtitleDialog, showResolutionDialog, showDebugDialog) {
        if (showControls && !isLocked && !isBuffering && loadingStatus.isEmpty() && !isPlaybackStuck && !showChannelList && !showAudioDialog && !showSubtitleDialog && !showResolutionDialog && !showDebugDialog) {
            delay(5000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        if (!isClosing) {
            AnimatedContent(
                targetState = currentChannel to activeEngine,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                label = "VideoTransition"
            ) { (targetChannel, engine) ->
            val playerLoadingVlc = stringResource(R.string.player_loading_vlc)
            val playerErrorVlc = stringResource(R.string.player_error_vlc)
            val playerExoFallback = stringResource(R.string.player_exo_fallback)

            if (engine == "VLC") {
                VlcVideoPlayer(
                    channel = targetChannel,
                    modifier = Modifier.fillMaxSize(),
                    hwAcceleration = hwAcceleration,
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    onPlayerInit = { player ->
                        if (player != null) vlcPlayer = player
                    },
                    onSuccess = {
                        android.util.Log.d("PlayerAnalytics", "VLC Success: ${targetChannel.name}")
                    },
                    onBuffering = { buffering -> 
                        isBuffering = buffering
                        if (!buffering) {
                            loadingStatus = ""
                        } else if (loadingStatus.isEmpty() || loadingStatus == playerLoadingDefault) {
                            loadingStatus = playerLoadingVlc
                        }
                    },
                    onPlayingChanged = { playing -> 
                        isPlayingState = playing
                        if (playing) {
                            loadingStatus = ""
                            isBuffering = false
                            isPlaybackStuck = false
                        }
                    },
                    onError = { 
                        errorMessage = it
                        isPlaybackStuck = true 
                        loadingStatus = context.getString(R.string.player_error_vlc, it)
                    }
                )
            } else {
                VideoPlayer(
                    channel = targetChannel,
                    modifier = Modifier.fillMaxSize(),
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    audioBoost = audioBoost,
                    hwAcceleration = hwAcceleration,
                    bufferSize = bufferSize,
                    maxVideoHeight = maxVideoHeight,
                    onPlayerInit = { player ->
                        if (player != null) exoPlayer = player
                    },
                    onSuccess = {
                        loadingStatus = ""
                        android.util.Log.d("PlayerAnalytics", "Exo Success: ${targetChannel.name}")
                    },
                    onError = { 
                        errorMessage = it
                        isPlaybackStuck = true 
                    },
                    onEngineSwitch = {
                        activeEngine = it
                    }
                )
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isLocked, showChannelList) {
                if (isLocked || showChannelList) return@pointerInput

                detectVerticalDragGestures(
                    onDragEnd = { gestureType = null },
                    onDragCancel = { gestureType = null },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / size.height
                        if (change.position.x < size.width / 2) {
                            gestureType = "Brightness"
                            brightness = (brightness + delta).coerceIn(0.01f, 1f)
                            activity?.let {
                                val lp = it.window.attributes
                                lp.screenBrightness = brightness
                                it.window.attributes = lp
                            }
                        } else {
                            gestureType = "Volume"
                            volume = (volume + delta).coerceIn(0f, 1f)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVol).toInt(), 0)
                        }
                    }
                )
            }
            .pointerInput(isLocked, showChannelList) {
                detectTapGestures(
                    onTap = {
                        if (!showChannelList) showControls = !showControls
                    }
                )
            }
    ) {
        // Overlay logic...
    }


        if (isPlaybackStuck && !isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SignalWifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: stringResource(R.string.player_connection_lost),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (errorMessage != null) stringResource(R.string.player_link_dead) else stringResource(R.string.player_stopped),
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

        AnimatedVisibility(
            visible = isLocked && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = { isLocked = false; showControls = true },
                modifier = Modifier.size(80.dp).background(Color.Black.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        AnimatedVisibility(
            visible = (showChannelInfo || showControls) && !isLocked && !showChannelList,
            enter = fadeIn(tween(400)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(400)) + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            ChannelInfoBar(
                currentChannel = currentChannel,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                showFullControls = showControls,
                isFullscreen = isFullscreen,
                exoPlayer = exoPlayer,
                vlcPlayer = vlcPlayer,
                onSeek = { position ->
                    if (activeEngine == "VLC") {
                        vlcPlayer?.time = position
                    } else {
                        exoPlayer?.seekTo(position)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showControls && !isLocked && !showChannelList,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            ControlOverlay(
                currentChannel = currentChannel,
                isFavorite = isFavorite,
                onBack = handleBack,
                onToggleFavorite = {
                    viewModel.toggleFavorite(currentChannel)
                },
                onShowChannels = { showChannelList = true },
                onLock = { isLocked = true },
                onPrev = {
                    val idx = currentGroupChannels.indexOfFirst { it.url == currentChannel.url }
                    if (idx > 0) {
                        changeChannel(currentGroupChannels[idx - 1])
                    }
                },
                onNext = {
                    val idx = currentGroupChannels.indexOfFirst { it.url == currentChannel.url }
                    if (idx < currentGroupChannels.size - 1) {
                        changeChannel(currentGroupChannels[idx + 1])
                    }
                },
                onFullscreenToggle = toggleFullscreen,
                isFullscreen = isFullscreen,
                onShowAudio = { showAudioDialog = true },
                onShowSubtitle = { showSubtitleDialog = true },
                onShowResolution = { showResolutionDialog = true },
                onSwitchEngine = {
                    activeEngine = if (activeEngine == "VLC") "EXO" else "VLC"
                },
                canSwitchEngine = !isExoOnly,
                onShowDebug = { if (isDebug) showDebugDialog = true },
                exoPlayer = exoPlayer,
                vlcPlayer = vlcPlayer,
                playerEngine = activeEngine,
                isPlayingState = isPlayingState,
                isDebug = isDebug,
                isPlaybackStuck = isPlaybackStuck,
                isBuffering = isBuffering,
                onReload = reloadVideo,
                loadingStatus = loadingStatus
            )
        }

        GestureHUD(gestureType, brightness, volume, seekPosition, seekTarget, seekDuration)

        // Floating Menus (Audio, CC, Quality) positioned above buttons
        val menuModifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(bottom = 80.dp)

        // Tap background to dismiss
        if (showAudioDialog || showSubtitleDialog || showResolutionDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showAudioDialog = false
                            showSubtitleDialog = false
                            showResolutionDialog = false
                        }
                    )
            )
        }

        AnimatedVisibility(
            visible = showAudioDialog,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart).then(menuModifier).padding(start = 24.dp)
        ) {
            TrackSelectionMenu(
                title = stringResource(R.string.player_menu_audio),
                exoPlayer = exoPlayer,
                vlcPlayer = if (activeEngine == "VLC") vlcPlayer else null,
                trackType = C.TRACK_TYPE_AUDIO,
                onDismiss = { showAudioDialog = false },
                showAudioBoost = true,
                audioBoost = audioBoost,
                onAudioBoostToggle = { viewModel.setAudioBoost(it) }
            )
        }

        AnimatedVisibility(
            visible = showSubtitleDialog,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart).then(menuModifier).padding(start = 24.dp)
        ) {
            TrackSelectionMenu(
                title = stringResource(R.string.player_menu_subtitle),
                exoPlayer = exoPlayer,
                vlcPlayer = if (activeEngine == "VLC") vlcPlayer else null,
                trackType = C.TRACK_TYPE_TEXT,
                onDismiss = { showSubtitleDialog = false }
            )
        }

        AnimatedVisibility(
            visible = showResolutionDialog,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).then(menuModifier).padding(end = 24.dp)
        ) {
            TrackSelectionMenu(
                title = stringResource(R.string.player_menu_quality),
                exoPlayer = exoPlayer,
                vlcPlayer = null,
                trackType = C.TRACK_TYPE_VIDEO,
                onDismiss = { showResolutionDialog = false }
            )
        }

        if (showDebugDialog && isDebug) {
            DebugInfoDialog(
                channel = currentChannel,
                onDismiss = { showDebugDialog = false }
            )
        }

        AnimatedVisibility(
            visible = showChannelList,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            QuickChannelList(
                allChannels = allChannels,
                groups = groups,
                currentChannel = currentChannel,
                onChannelSelect = {
                    currentChannel = it
                    viewModel.markAsPlayed(it)
                    showChannelList = false
                },
                onClose = { showChannelList = false }
            )
        }
    }
}

@Composable
fun GestureHUD(type: String?, brightness: Float, volume: Float, seekPosition: Long, seekTarget: Long, seekDuration: Long) {
    var visible by remember { mutableStateOf(false) }
    var currentType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(type) {
        if (type != null) {
            currentType = type
            visible = true
        } else {
            delay(1000)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        val displayType = currentType ?: return@AnimatedVisibility
        
        if (displayType == "Seek") {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        val isForward = seekTarget >= seekPosition
                        Icon(
                            imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                PlayerUtils.formatTime(seekTarget),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                " / ${PlayerUtils.formatTime(seekDuration)}",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        val diff = seekTarget - seekPosition
                        Text(
                            text = "${if (diff >= 0) "+" else ""}${PlayerUtils.formatTime(Math.abs(diff))}",
                            color = if (isForward) Color.Green else Color.Red,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { (seekTarget.toFloat() / seekDuration.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.width(200.dp).height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(0.1f)
                        )
                    }
                }
            }
        } else {
            Box(contentAlignment = if (displayType == "Brightness") Alignment.CenterStart else Alignment.CenterEnd) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Icon(
                        imageVector = if (displayType == "Brightness") Icons.Default.Brightness6 else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(150.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                    ) {
                        val progress = if (displayType == "Brightness") brightness else volume
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progress)
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@AndroidOptIn(UnstableApi::class)
@Composable
fun TrackSelectionMenu(
    title: String,
    exoPlayer: ExoPlayer?,
    vlcPlayer: org.videolan.libvlc.MediaPlayer?,
    trackType: Int,
    onDismiss: () -> Unit,
    showAudioBoost: Boolean = false,
    audioBoost: Boolean = false,
    onAudioBoostToggle: ((Boolean) -> Unit)? = null
) {
    // Logic for ExoPlayer
    val tracks = exoPlayer?.currentTracks ?: Tracks.EMPTY
    val trackGroups = tracks.groups.filter { it.type == trackType }

    val exoTrackItems = remember(tracks, trackType) {
        val list = mutableListOf<Pair<Tracks.Group, Int>>()
        trackGroups.forEach { group ->
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    list.add(group to i)
                }
            }
        }
        if (trackType == C.TRACK_TYPE_VIDEO) {
            list.sortBy { it.first.getTrackFormat(it.second).height }
        }
        list
    }

    // Logic for VLC
    val vlcTracks = remember(vlcPlayer, trackType) {
        when (trackType) {
            C.TRACK_TYPE_AUDIO -> vlcPlayer?.audioTracks
            C.TRACK_TYPE_TEXT -> vlcPlayer?.spuTracks
            else -> null
        }
    }
    val currentVlcTrackId = remember(vlcPlayer, trackType) {
        when (trackType) {
            C.TRACK_TYPE_AUDIO -> vlcPlayer?.audioTrack ?: -1
            C.TRACK_TYPE_TEXT -> vlcPlayer?.spuTrack ?: -1
            else -> -1
        }
    }

    Surface(
        modifier = Modifier.width(200.dp).padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E1E).copy(alpha = 0.85f),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, Color.White.copy(0.15f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (trackType == C.TRACK_TYPE_AUDIO && vlcPlayer != null) {
                var currentDelay by remember { mutableLongStateOf(vlcPlayer.audioDelay / 1000) } // ms
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.05f))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.player_audio_delay),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.6f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { 
                                currentDelay -= 50
                                vlcPlayer.audioDelay = currentDelay * 1000
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${currentDelay}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { 
                                currentDelay += 50
                                vlcPlayer.audioDelay = currentDelay * 1000
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (showAudioBoost && onAudioBoostToggle != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { onAudioBoostToggle(!audioBoost) }
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        stringResource(R.string.player_boost),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    Switch(
                        checked = audioBoost,
                        onCheckedChange = onAudioBoostToggle,
                        modifier = Modifier.scale(0.45f).size(32.dp, 16.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            LazyColumn(modifier = Modifier.heightIn(max = 180.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (vlcPlayer != null && vlcTracks != null) {
                    // VLC Track List
                    item {
                        val label = when (trackType) {
                            C.TRACK_TYPE_AUDIO -> stringResource(R.string.player_mute)
                            C.TRACK_TYPE_TEXT -> stringResource(R.string.player_no_subtitle)
                            else -> stringResource(R.string.player_disable)
                        }
                        TrackItem(
                            label = label,
                            isSelected = currentVlcTrackId == -1,
                            onClick = {
                                if (trackType == C.TRACK_TYPE_AUDIO) vlcPlayer.audioTrack = -1
                                else vlcPlayer.spuTrack = -1
                                onDismiss()
                            }
                        )
                    }
                    items(vlcTracks.toList()) { track ->
                        if (track.id != -1) {
                            TrackItem(
                                label = track.name ?: "Track ${track.id}",
                                isSelected = track.id == currentVlcTrackId,
                                onClick = {
                                    if (trackType == C.TRACK_TYPE_AUDIO) vlcPlayer.audioTrack = track.id
                                    else vlcPlayer.spuTrack = track.id
                                    onDismiss()
                                }
                            )
                        }
                    }
                } else {
                    // ExoPlayer Track List
                    item {
                        val isAuto = exoPlayer?.trackSelectionParameters?.overrides?.values?.none {
                            it.type == trackType
                        } ?: true

                        val label = when (trackType) {
                            C.TRACK_TYPE_VIDEO -> stringResource(R.string.player_auto)
                            C.TRACK_TYPE_AUDIO -> stringResource(R.string.player_default)
                            C.TRACK_TYPE_TEXT -> stringResource(R.string.player_no_subtitle)
                            else -> stringResource(R.string.player_disable)
                        }

                        TrackItem(
                            label = label,
                            isSelected = isAuto,
                            onClick = {
                                exoPlayer?.let {
                                    it.trackSelectionParameters = it.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(trackType)
                                        .build()
                                }
                                onDismiss()
                            }
                        )
                    }

                    items(exoTrackItems) { (group, trackIndex) ->
                        val format = group.getTrackFormat(trackIndex)
                        val isExplicitlySelected = exoPlayer?.trackSelectionParameters?.overrides?.get(group.mediaTrackGroup)?.trackIndices?.contains(trackIndex) ?: false

                        val label = when (trackType) {
                            C.TRACK_TYPE_VIDEO -> "${format.height}p"
                            C.TRACK_TYPE_AUDIO -> format.language?.uppercase() ?: format.label ?: "Audio ${trackIndex + 1}"
                            C.TRACK_TYPE_TEXT -> format.language?.uppercase() ?: format.label ?: "Subtitle ${trackIndex + 1}"
                            else -> "Track ${trackIndex + 1}"
                        }

                        TrackItem(
                            label = label,
                            isSelected = isExplicitlySelected,
                            onClick = {
                                exoPlayer?.let {
                                    it.trackSelectionParameters = it.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                        .build()
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            label, 
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DebugInfoDialog(
    channel: IptvChannel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.player_debug_title), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item { DebugInfoItem("Channel Name", channel.name) }
                    item { DebugInfoItem("Stream URL", channel.url) }
                    item { DebugInfoItem("Raw DRM Config", channel.drmConfig ?: "None") }

                    item {
                        val mediaItem = PlayerUtils.buildMediaItem(channel)
                        val drmConfig = mediaItem.localConfiguration?.drmConfiguration

                        if (drmConfig != null) {
                            val schemeName = when(drmConfig.scheme) {
                                C.WIDEVINE_UUID -> "Widevine"
                                C.CLEARKEY_UUID -> "ClearKey"
                                C.PLAYREADY_UUID -> "PlayReady"
                                else -> drmConfig.scheme.toString()
                            }
                            DebugInfoItem("DRM Status", "ACTIVE (Type: $schemeName)")
                            DebugInfoItem("License URI (JWK/URL)", drmConfig.licenseUri?.toString() ?: "None")
                        } else {
                            DebugInfoItem("DRM Status", "INACTIVE")
                        }
                    }

                    item { DebugInfoItem("User Agent", channel.userAgent ?: "Default") }
                    item { DebugInfoItem("Referrer", channel.referrer ?: "None") }
                    item { DebugInfoItem("TVG ID", channel.tvgId ?: "None") }
                    item { DebugInfoItem("Group", channel.group ?: "Default") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@Composable
fun DebugInfoItem(label: String, value: String) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                scope.launch {
                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText(label, value)))
                }
            }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(12.dp))
        }
        Text(
            value,
            color = Color.White.copy(0.8f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.White.copy(0.1f))
    }
}



@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlOverlay(
    currentChannel: IptvChannel,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowChannels: () -> Unit,
    onLock: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit = {},
    onShowAudio: () -> Unit,
    onShowSubtitle: () -> Unit = {},
    onShowResolution: () -> Unit,
    onSwitchEngine: () -> Unit,
    canSwitchEngine: Boolean = true,
    onShowDebug: () -> Unit,
    exoPlayer: ExoPlayer? = null,
    vlcPlayer: org.videolan.libvlc.MediaPlayer? = null,
    playerEngine: String = "ExoPlayer",
    isPlayingState: Boolean = false,
    isDebug: Boolean = false,
    isPlaybackStuck: Boolean = false,
    isBuffering: Boolean = false,
    onReload: () -> Unit = {},
    loadingStatus: String = ""
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerControlAction(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
            
            Spacer(modifier = Modifier.weight(1f))

            // Engine Switcher (Top Position)
            Surface(
                onClick = if (canSwitchEngine) onSwitchEngine else ({}),
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(19.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (canSwitchEngine) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SettingsInputComponent, 
                        null, 
                        tint = if (canSwitchEngine) Color.White else Color.White.copy(0.3f), 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canSwitchEngine) playerEngine else "$playerEngine",
                        color = if (canSwitchEngine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.5f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            PlayerControlAction(icon = Icons.Default.LockOpen, onClick = onLock)
            Spacer(modifier = Modifier.width(12.dp))
            PlayerControlAction(icon = Icons.AutoMirrored.Filled.FormatListBulleted, onClick = onShowChannels)
        }

        // Center Controls (Skip/Play/Forward)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = if (isFullscreen) 400.dp else 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            IconButton(
                onClick = onPrev, 
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(0.3f), CircleShape)
                    .border(1.dp, Color.White.copy(0.05f), CircleShape)
            ) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // Play/Pause Center Action & Loading Status
            Box(
                modifier = Modifier.height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                val isError = isPlaybackStuck || loadingStatus.contains("Gagal", true) || loadingStatus.contains("Error", true)
                
                Box(contentAlignment = Alignment.Center) {
                    // Reduced glow size
                    if (isPlayingState && !isBuffering) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(MaterialTheme.colorScheme.primary.copy(0.2f), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )
                    }

                    Surface(
                        onClick = { 
                            if (isError) onReload() 
                            else {
                                if (playerEngine == "VLC") {
                                    vlcPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                                } else {
                                    exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                                }
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.2f), Color.Transparent))),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Refresh else if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(0.05f)
                        )
                    }
                }
                
                if (loadingStatus.isNotEmpty() && !isPlaybackStuck) {
                    Text(
                        text = loadingStatus.uppercase(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 0.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Next Button
            IconButton(
                onClick = onNext, 
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(0.3f), CircleShape)
                    .border(1.dp, Color.White.copy(0.05f), CircleShape)
            ) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Bottom Actions & Engine Switcher
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerControlAction(icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, tint = if (isFavorite) Color(0xFFFFD700) else Color.White, onClick = onToggleFavorite)
                    PlayerControlAction(icon = Icons.Default.ClosedCaption, onClick = onShowSubtitle)
                    PlayerControlAction(icon = Icons.AutoMirrored.Filled.VolumeUp, onClick = onShowAudio)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerControlAction(icon = Icons.Default.HighQuality, onClick = onShowResolution)
                    if (isDebug) PlayerControlAction(icon = Icons.Default.BugReport, onClick = onShowDebug)
                    PlayerControlAction(icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, onClick = onFullscreenToggle)
                }
            }
        }
    }
}

@Composable
fun PlayerControlAction(icon: ImageVector, tint: Color = Color.White, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "scale")
    
    Surface(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun QuickChannelList(
    allChannels: List<IptvChannel>,
    groups: List<String>,
    currentChannel: IptvChannel,
    onChannelSelect: (IptvChannel) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current channel
    LaunchedEffect(allChannels) {
        val index = allChannels.indexOfFirst { it.url == currentChannel.url }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp),
        color = Color(0xFF0D0D0F).copy(alpha = 0.92f),
        tonalElevation = 12.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.player_channels_title).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 2.sp
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Channel List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allChannels) { channel ->
                    val isCurrent = channel.url == currentChannel.url
                    Surface(
                        onClick = { onChannelSelect(channel) },
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.08f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Indicator
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp, 16.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            AsyncImage(
                                model = channel.logo,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp, 32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(0.03f)),
                                contentScale = ContentScale.Fit,
                                error = painterResource(R.drawable.app_icon_android),
                                placeholder = painterResource(R.drawable.app_icon_android)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(0.8f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val groupName = channel.group
                                if (!groupName.isNullOrEmpty()) {
                                    Text(
                                        text = groupName.uppercase(Locale.getDefault()),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isCurrent) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
