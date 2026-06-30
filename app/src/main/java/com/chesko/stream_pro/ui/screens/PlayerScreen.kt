package com.chesko.stream_pro.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import com.google.android.gms.cast.framework.CastContext
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.player.VideoPlayer
import android.content.ClipData
import androidx.compose.ui.platform.LocalClipboardManager
import kotlinx.coroutines.launch
import androidx.compose.ui.text.AnnotatedString
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.PlayerUtils
import com.chesko.stream_pro.core.utils.NetworkObserver
import com.chesko.stream_pro.ui.components.shimmerEffect
import com.chesko.stream_pro.ui.theme.OffWhiteSolar
import com.chesko.stream_pro.ui.theme.DarkSurface
import com.chesko.stream_pro.ui.theme.DarkOnSurface
import com.chesko.stream_pro.ui.theme.CinematicBlack
import com.chesko.stream_pro.ui.theme.DarkSurfaceVariant
import com.chesko.stream_pro.ui.theme.DarkOnSurfaceVariant
import com.chesko.stream_pro.ui.theme.PrimaryGoldDark
import com.chesko.stream_pro.ui.theme.CosmicError
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

private fun getSystemBrightness(context: Context): Float {
    return try {
        android.provider.Settings.System.getInt(
            context.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS
        ) / 255f
    } catch (e: Exception) {
        0.5f
    }
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
    castPlayer: CastPlayer?,
    onSeek: (Long) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Smart EPG Transition: Pre-fetch or refresh EPG data when current program is about to end
    LaunchedEffect(currentProgram, currentTimeMillis) {
        currentProgram?.let {
            val remainingTime = it.endTime - currentTimeMillis
            // If less than 30 seconds remaining, and we haven't refreshed, could poke the VM
            if (remainingTime in 1..30000) {
                // The Flow in ViewModel will automatically emit the next program once currentTime passes endTime
                // because repository.getCurrentProgram uses System.currentTimeMillis() internally.
            }
        }
    }

    LaunchedEffect(exoPlayer, castPlayer) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            if (!isDragging) {
                if (castPlayer?.isCastSessionAvailable == true) {
                    playbackPosition = castPlayer.currentPosition
                    playbackDuration = castPlayer.duration
                } else if (exoPlayer != null) {
                    playbackPosition = exoPlayer.currentPosition
                    playbackDuration = exoPlayer.duration
                }
            }
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (showFullControls) (if (isFullscreen) 78.dp else 88.dp) else 24.dp)
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
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
                                letterSpacing = 1.5.sp,
                                fontSize = 9.sp
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
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = timeFormatter.format(Date(nextProgram.startTime)),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
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
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            PlayerUtils.formatTime(if (isDragging) (dragPosition * playbackDuration).toLong() else playbackPosition),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        Text(
                            PlayerUtils.formatTime(playbackDuration),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            } else if (currentProgram != null) {
                val progress = ((currentTimeMillis - currentProgram.startTime).toFloat() /
                               (currentProgram.endTime - currentProgram.startTime).toFloat()).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                )
            }
        }
    }
}



@Composable
fun PlayerTheme(accentColor: Color, content: @Composable () -> Unit) {
    val playerColorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color.Black,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        background = Color.Black, // True black untuk pengalaman menonton maksimal
        onBackground = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        primaryContainer = PrimaryGoldDark,
        onPrimaryContainer = Color.Black,
        error = CosmicError,
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

@SuppressLint("SourceLockedOrientationActivity")
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

    LaunchedEffect(Unit) {
        viewModel.setSearchQuery("")
    }

    DisposableEffect(Unit) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        onDispose { }
    }

    var showControls by remember { mutableStateOf(true) }
    var isClosing by remember { mutableStateOf(false) }
    val isInPipMode by viewModel.isInPipMode.collectAsState()

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

    // Keep ViewModel in sync with current channel for Casting and EPG
    LaunchedEffect(currentChannel) {
        viewModel.setSelectedChannel(currentChannel)
        
        // Optimization: Auto-select current channel's group if none selected
        // to avoid loading "All Channels" which can be heavy.
        if (viewModel.selectedGroup.value == null && !currentChannel.group.isNullOrBlank()) {
            viewModel.setSelectedGroup(currentChannel.group)
        }
    }

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
    
    val castPlayer by viewModel.castPlayer.collectAsState()
    val isCasting by viewModel.isCasting.collectAsState()
    val availableRoutes by viewModel.availableRoutes.collectAsState()
    var showCastDiscovery by remember { mutableStateOf(false) }
    var isConnectingToCast by remember { mutableStateOf(false) }

    val lastCastPosition by viewModel.lastCastPosition.collectAsState()

    LaunchedEffect(isConnectingToCast) {
        if (isConnectingToCast) {
            delay(15000) // 15 seconds timeout
            if (isConnectingToCast) {
                isConnectingToCast = false
                viewModel.setErrorMessage("Gagal menghubungkan ke TV (Timeout)")
            }
        }
    }

    // Resume local playback when casting ends from external source (e.g. notification or TV)
    LaunchedEffect(isCasting) {
        if (!isCasting && !isClosing && !isConnectingToCast) {
            exoPlayer?.let {
                if (!it.isPlaying) {
                    it.seekTo(lastCastPosition)
                    it.play()
                }
            }
        }
    }

    val handleStopCasting: () -> Unit = {
        val lastPos = viewModel.stopCasting()
        isConnectingToCast = false
        exoPlayer?.let {
            it.seekTo(lastPos)
            it.play()
        }
        Unit
    }

    // Auto-transfer media item to CastPlayer when casting starts or channel changes
    LaunchedEffect(isCasting, currentChannel) {
        if (isCasting) {
            delay(1000)
            val currentPos = exoPlayer?.currentPosition ?: 0L

            android.util.Log.d("PlayerScreen", "Transferring ${currentChannel.name} to Cast at position $currentPos")
            viewModel.transferCurrentMediaToCast(currentPos)

            exoPlayer?.pause()
            isConnectingToCast = false
            showCastDiscovery = false
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var brightness by remember {
        val initial = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (initial < 0) getSystemBrightness(context) else initial)
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



    val currentProgram by viewModel.getCurrentProgram(currentChannel).collectAsState(initial = null)
    val nextProgram by viewModel.getNextProgram(currentChannel).collectAsState(initial = null)

    val currentGroupChannels = allChannels

    var showAudioDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showChannelInfo by remember { mutableStateOf(false) }

    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            showControls = false
            showChannelList = false
            showAudioDialog = false
            showResolutionDialog = false
            showDebugDialog = false
            showChannelInfo = false
        }
    }

    var zappingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val changeChannel = { nextChan: IptvChannel ->
        zappingJob?.cancel()
        currentChannel = nextChan
        viewModel.markAsPlayed(nextChan)

        if (isClosing) {
            exoPlayer?.stop()
        }

        zappingJob = scope.launch {
            delay(1500)
        }
    }

    val reloadVideo: () -> Unit = {
        exoPlayer?.let {
            it.stop()
            it.prepare()
            it.play()
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

    LaunchedEffect(currentChannel.url) {
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
                        if (player.isCurrentMediaItemLive) {
                            scope.launch {
                                isBuffering = true
                                loadingStatus = playerLoadingStream
                                delay(3000)
                                reloadVideo()
                            }
                        } else {
                            loadingStatus = playerEnded
                        }
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
                    if (bufferCount >= 10) {
                        reloadVideo()
                        bufferCount = 0
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
                exoPlayer?.let {
                    it.playWhenReady = false
                    it.stop()
                    it.release()
                }
                exoPlayer = null
                viewModel.clearError()
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

    LaunchedEffect(showControls, isLocked, isBuffering, loadingStatus, isPlaybackStuck, showChannelList, showAudioDialog, showResolutionDialog, showDebugDialog) {
        if (showControls && !isLocked && !isBuffering && loadingStatus.isEmpty() && !isPlaybackStuck && !showChannelList && !showAudioDialog && !showResolutionDialog && !showDebugDialog) {
            delay(5000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        if (!isClosing) {
            VideoPlayer(
                channel = currentChannel,
                modifier = Modifier.fillMaxSize(),
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                audioBoost = audioBoost,
                hwAcceleration = hwAcceleration,
                bufferSize = bufferSize,
                maxVideoHeight = maxVideoHeight,
                isInPipMode = isInPipMode,
                onPlayerInit = { player ->
                    if (player != null) exoPlayer = player
                },
                onSuccess = {
                    loadingStatus = ""
                    android.util.Log.d("PlayerAnalytics", "Exo Success: ${currentChannel.name}")
                },
                onError = {
                    errorMessage = it
                    isPlaybackStuck = true
                }
            )

            if (isCasting && !isInPipMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = handleStopCasting,
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CastConnected,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    stringResource(R.string.player_casting_active),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.btn_stop_casting),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isInPipMode) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, showChannelList) {
                    if (isLocked || showChannelList) return@pointerInput

                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val currentWinBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                            if (currentWinBrightness < 0) {
                                brightness = getSystemBrightness(context)
                            }
                        },
                        onDragEnd = { gestureType = null },
                        onDragCancel = { gestureType = null },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / size.height
                            val half = size.width / 2f

                            val topThreshold = if (size.height > size.width) size.height * 0.30f else size.height * 0.15f
                            val bottomThreshold = if (size.height > size.width) size.height * 0.70f else size.height * 0.85f

                            if (change.position.y < topThreshold || change.position.y > bottomThreshold) {
                                gestureType = null
                                return@detectVerticalDragGestures
                            }

                            if (change.position.x < half) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (errorMessage != null) stringResource(R.string.player_link_dead) else stringResource(R.string.player_stopped),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
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
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
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
                    castPlayer = castPlayer,
                    onSeek = { position ->
                        if (isCasting) {
                            castPlayer?.seekTo(position)
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
                        if (currentGroupChannels.isNotEmpty()) {
                            val idx = currentGroupChannels.indexOfFirst { it.url == currentChannel.url }
                            val prevIdx = if (idx <= 0) currentGroupChannels.size - 1 else idx - 1
                            changeChannel(currentGroupChannels[prevIdx])
                        }
                    },
                    onNext = {
                        if (currentGroupChannels.isNotEmpty()) {
                            val idx = currentGroupChannels.indexOfFirst { it.url == currentChannel.url }
                            val nextIdx = if (idx == -1 || idx >= currentGroupChannels.size - 1) 0 else idx + 1
                            changeChannel(currentGroupChannels[nextIdx])
                        }
                    },
                    onFullscreenToggle = toggleFullscreen,
                    isFullscreen = isFullscreen,
                    onShowAudio = { showAudioDialog = true },
                    onShowResolution = { showResolutionDialog = true },
                    onShowDebug = { if (isDebug) showDebugDialog = true },
                    onShowCast = { 
                        if (isCasting) {
                            handleStopCasting()
                        } else {
                            showCastDiscovery = true 
                        }
                    },
                    exoPlayer = exoPlayer,
                    castPlayer = castPlayer,
                    isCasting = isCasting,
                    isPlayingState = isPlayingState,
                    isDebug = isDebug,
                    isPlaybackStuck = isPlaybackStuck,
                    isBuffering = isBuffering,
                    onReload = reloadVideo,
                    loadingStatus = loadingStatus,
                    onEnterPip = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val builder = android.app.PictureInPictureParams.Builder()
                            exoPlayer?.let { player ->
                                val videoSize = player.videoSize
                                if (videoSize.width > 0 && videoSize.height > 0) {
                                    val rational = android.util.Rational(videoSize.width, videoSize.height)
                                    val finalRatio = if (rational.toFloat() < 0.418f) android.util.Rational(418, 1000)
                                    else if (rational.toFloat() > 2.39f) android.util.Rational(2390, 1000)
                                    else rational
                                    builder.setAspectRatio(finalRatio)
                                } else {
                                    builder.setAspectRatio(android.util.Rational(16, 9))
                                }
                            } ?: builder.setAspectRatio(android.util.Rational(16, 9))
                            activity?.enterPictureInPictureMode(builder.build())
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                @Suppress("DEPRECATION")
                                activity?.enterPictureInPictureMode()
                            }
                        }
                    }
                )
            }

            GestureHUD(gestureType, brightness, volume, seekPosition, seekTarget, seekDuration)

            val menuModifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 80.dp)

            if (showAudioDialog || showResolutionDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                showAudioDialog = false
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
                    trackType = C.TRACK_TYPE_AUDIO,
                    onDismiss = { showAudioDialog = false },
                    showAudioBoost = true,
                    audioBoost = audioBoost,
                    onAudioBoostToggle = { viewModel.setAudioBoost(it) }
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
                    viewModel = viewModel,
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

            if (showCastDiscovery) {
                CastDiscoveryOverlay(
                    routes = availableRoutes,
                    onRouteSelected = { route ->
                        isConnectingToCast = true
                        route.select()
                        showCastDiscovery = false
                    },
                    onRetry = { viewModel.retryDiscovery() },
                    onDismiss = { showCastDiscovery = false }
                )
            }

            if (isConnectingToCast) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.player_preparing),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
    }
    }
}

@Composable
fun CastDiscoveryOverlay(
    routes: List<MediaRouter.RouteInfo>,
    onRouteSelected: (MediaRouter.RouteInfo) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    var isSearching by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LaunchedEffect(isSearching) {
        if (isSearching) {
            delay(8000)
            isSearching = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!isSearching) onDismiss()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(170.dp)
                .padding(6.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = alpha + 0.1f
                                    scaleY = alpha + 0.1f
                                    this.alpha = 0.8f - alpha
                                }
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Cast else Icons.Default.CastConnected,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isSearching) "Mencari TV..." else "Pilih Perangkat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                
                Text(
                    text = if (isSearching) "Pastikan WiFi sama" else "${routes.size} Ditemukan",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 1.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (routes.isEmpty() && !isSearching) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            onClick = { 
                                isSearching = true
                                onRetry() 
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(9.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Cari Ulang", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ketuk di luar untuk menutup",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(routes) { route ->
                            Surface(
                                onClick = { onRouteSelected(route) },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (route.deviceType) {
                                                MediaRouter.RouteInfo.DEVICE_TYPE_TV -> Icons.Default.Tv
                                                MediaRouter.RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER -> Icons.Default.Speaker
                                                else -> Icons.Default.Cast
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = route.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (route.description != null) {
                                            Text(
                                                text = route.description!!,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 6.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                        
                        if (isSearching) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }
            }
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        val isForward = seekTarget >= seekPosition
                        Icon(
                            imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                        )
                    }
                }
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (displayType == "Brightness") Icons.Default.Brightness6 else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val progress = if (displayType == "Brightness") brightness else volume
                    val percentage = (progress * 100).toInt()
                    Text(
                        text = "$percentage%",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
    trackType: Int,
    onDismiss: () -> Unit,
    showAudioBoost: Boolean = false,
    audioBoost: Boolean = false,
    onAudioBoostToggle: ((Boolean) -> Unit)? = null
) {
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

    Surface(
        modifier = Modifier.width(200.dp).padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.15f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                        color = MaterialTheme.colorScheme.onSurface,
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
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                    Text(stringResource(R.string.player_debug_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(0.1f), contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@Composable
fun DebugInfoItem(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(value))
            }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(12.dp))
        }
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
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
    onShowResolution: () -> Unit,
    onShowDebug: () -> Unit,
    onShowCast: () -> Unit = {},
    exoPlayer: ExoPlayer? = null,
    castPlayer: CastPlayer? = null,
    isCasting: Boolean = false,
    isPlayingState: Boolean = false,
    isDebug: Boolean = false,
    isPlaybackStuck: Boolean = false,
    isBuffering: Boolean = false,
    onReload: () -> Unit = {},
    loadingStatus: String = "",
    onEnterPip: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

            PlayerControlAction(
                icon = Icons.Default.PictureInPicture,
                onClick = onEnterPip
            )

            Spacer(modifier = Modifier.width(12.dp))

            PlayerControlAction(
                icon = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                tint = if (isCasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = onShowCast
            )

            Spacer(modifier = Modifier.width(12.dp))
            PlayerControlAction(icon = Icons.AutoMirrored.Filled.FormatListBulleted, onClick = onShowChannels)
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = if (isFullscreen) 400.dp else 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(0.3f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f), CircleShape)
            ) {
                Icon(Icons.Default.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }

            Box(
                modifier = Modifier.height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                val isError = isPlaybackStuck ||
                             loadingStatus.contains("Gagal", true) ||
                             loadingStatus.contains("Error", true) ||
                             loadingStatus == stringResource(R.string.player_ended)

                Box(contentAlignment = Alignment.Center) {
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
                                exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface.copy(0.2f), Color.Transparent))),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Refresh else if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(0.05f)
                        )
                    }
                }

                if (loadingStatus.isNotEmpty() && !isPlaybackStuck) {
                    Text(
                        text = loadingStatus.uppercase(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 0.dp),
                        color = MaterialTheme.colorScheme.primary.copy(0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp
                    )
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(0.3f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f), CircleShape)
            ) {
                Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 20.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlayerControlAction(
                            icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                            onClick = onToggleFavorite
                        )
                        PlayerControlAction(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onClick = onShowAudio
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlayerControlAction(
                            icon = Icons.Default.HighQuality,
                            onClick = onShowResolution
                        )
                        if (isDebug) {
                            PlayerControlAction(
                                icon = Icons.Default.BugReport,
                                onClick = onShowDebug
                            )
                        }
                        PlayerControlAction(
                            icon = Icons.Default.LockOpen,
                            onClick = onLock
                        )
                        PlayerControlAction(
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            onClick = onFullscreenToggle
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerControlAction(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun QuickChannelList(
    viewModel: MainViewModel,
    allChannels: List<IptvChannel>,
    groups: List<String>,
    currentChannel: IptvChannel,
    onChannelSelect: (IptvChannel) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val groupListState = rememberLazyListState()

    LaunchedEffect(currentChannel) {
        val index = allChannels.indexOfFirst { it.url == currentChannel.url }
        if (index >= 0) {
            delay(300)
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(selectedGroup, groups) {
        if (groups.isNotEmpty()) {
            val index = if (selectedGroup == null) 0 else groups.indexOf(selectedGroup)
            if (index >= 0) {
                groupListState.animateScrollToItem(index)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = (selectedGroup ?: stringResource(R.string.player_channels_title)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(180.dp)
                    )
                    if (selectedGroup != null) {
                        Text(
                            text = stringResource(R.string.player_channels_title).lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
            LazyRow(
                state = groupListState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(groups) { group ->
                    QuickGroupChip(
                        text = group,
                        isSelected = selectedGroup == group,
                        onClick = { viewModel.setSelectedGroup(group) }
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = allChannels,
                    key = { it.url }
                ) { channel ->
                    val isSelected = channel.url == currentChannel.url
                    ChannelListItem(
                        channel = channel,
                        isSelected = isSelected,
                        onClick = { onChannelSelect(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickGroupChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        modifier = Modifier.height(24.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChannelListItem(
    channel: IptvChannel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp, 28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.app_icon_android),
                placeholder = painterResource(R.drawable.app_icon_android)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val groupName = channel.group
                if (!groupName.isNullOrEmpty()) {
                    Text(
                        text = groupName.uppercase(),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
