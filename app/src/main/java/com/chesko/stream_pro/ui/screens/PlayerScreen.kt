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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.chesko.stream_pro.core.ui.MainViewModel
import com.chesko.stream_pro.core.utils.PlayerUtils
import com.chesko.stream_pro.core.player.VlcVideoPlayer
import org.videolan.libvlc.MediaPlayer
import com.chesko.stream_pro.ui.components.shimmerEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.pm.ApplicationInfo
import androidx.compose.ui.draw.scale

import java.text.SimpleDateFormat
import java.util.*

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
    exoPlayer: ExoPlayer?
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            playbackPosition = player.currentPosition
            playbackDuration = player.duration
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (showFullControls) 80.dp else 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
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
                                "ON AIR",
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
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NEXT: ${nextProgram.title}",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar (VOD or EPG)
            if (playbackDuration > 0 && playbackDuration != C.TIME_UNSET) {
                Column {
                    LinearProgressIndicator(
                        progress = { (playbackPosition.toFloat() / playbackDuration.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            PlayerUtils.formatTime(playbackPosition),
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
fun PlayerTheme(content: @Composable () -> Unit) {
    val playerColorScheme = darkColorScheme(
        primary = Color(0xFF2979FF),
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
    onBack: () -> Unit
) {
    PlayerTheme {
        PlayerScreenContent(viewModel, channel, onBack)
    }
}

@AndroidOptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreenContent(
    viewModel: MainViewModel,
    channel: IptvChannel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var showControls by remember { mutableStateOf(true) }

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
    var currentChannel by remember { mutableStateOf(channel) }

    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val audioBoost by viewModel.audioBoost.collectAsState()
    val playerEngineSetting by viewModel.playerEngine.collectAsState()
    var activeEngine by remember(playerEngineSetting, currentChannel.url) { 
        val url = currentChannel.url.lowercase()
        val initialEngine = when {
            url.startsWith("rtsp://") || url.startsWith("udp://") || url.startsWith("rtp://") -> "VLC"

            url.contains(".ts") || url.contains("mpegts") -> "VLC"

            url.contains(".mpd") || !currentChannel.drmType.isNullOrBlank() -> "EXO"

            url.contains(".m3u8") -> "EXO"

            else -> playerEngineSetting
        }
        mutableStateOf(initialEngine) 
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
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var vlcPlayer by remember { mutableStateOf<org.videolan.libvlc.MediaPlayer?>(null) }

    var brightness by remember { mutableStateOf(activity?.window?.attributes?.screenBrightness ?: 0.5f) }
    var volume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }
    var gestureType by remember { mutableStateOf<String?>(null) }

    var isBuffering by remember { mutableStateOf(false) }
    var isPlaybackStuck by remember { mutableStateOf(false) }
    var isPlayingState by remember { mutableStateOf(false) }

    // Explicitly stop unused player when engine switches to prevent concurrent execution
    LaunchedEffect(activeEngine) {
        if (activeEngine == "VLC") {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } else {
            try {
                vlcPlayer?.stop()
            } catch (_: Exception) {}
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
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showChannelInfo by remember { mutableStateOf(false) }

    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val reloadVideo: () -> Unit = {
        if (activeEngine == "VLC") {
            try {
                vlcPlayer?.stop()
                vlcPlayer?.play()
            } catch (_: Exception) {
                // If player is released, it will be recreated by AnimatedContent recomposition
            }
        } else {
            exoPlayer?.stop()
            exoPlayer?.prepare()
            exoPlayer?.play()
        }
        isPlaybackStuck = false
        isBuffering = true
        scope.launch {
            delay(3000)
            isBuffering = false
        }
    }

    var loadingStatus by remember { mutableStateOf("Menghubungkan...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentChannel.url, activeEngine) {
        errorMessage = null
        isPlaybackStuck = false
        isBuffering = true
        loadingStatus = "Menghubungkan..."
        
        showChannelInfo = true
        delay(5000)
        showChannelInfo = false
    }

    // Force clear loading state when video starts playing
    LaunchedEffect(isPlayingState) {
        if (isPlayingState) {
            loadingStatus = ""
            isBuffering = false
        }
    }

    DisposableEffect(exoPlayer, currentChannel) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    if (loadingStatus == "Memuat Video...") loadingStatus = ""
                } else if (player.playbackState == Player.STATE_READY) {
                    loadingStatus = "Memuat Video..."
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlayingState = player.isPlaying
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        if (loadingStatus.isEmpty()) loadingStatus = "Loading Stream..."
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
                        if (loadingStatus.isEmpty()) loadingStatus = "Menyiapkan..."
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        loadingStatus = "Siaran Berakhir"
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

                // Automatic Fallback to VLC
                if (activeEngine == "EXO") {
                    loadingStatus = "Fallback ke VLC..."
                    activeEngine = "VLC"
                    return
                }

                loadingStatus = "Stream Error: Gagal Memuat"
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
                    // First try to reload within Exo
                    if (bufferCount == 8) {
                        reloadVideo()
                    }
                    // If still buffering after 15 units (~30s total), try fallback to VLC
                    if (bufferCount >= 15 && activeEngine == "EXO") {
                        activeEngine = "VLC"
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
                onBack()
            }
        }
    }

    val handleResize = {
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

    // Use a fixed deep black background for the player to ensure best video contrast and eye comfort
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        AnimatedContent(
            targetState = currentChannel to activeEngine,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            label = "VideoTransition"
        ) { (targetChannel, engine) ->
            if (engine == "VLC") {
                VlcVideoPlayer(
                    channel = targetChannel,
                    modifier = Modifier.fillMaxSize(),
                    hwAcceleration = hwAcceleration,
                    resizeMode = resizeMode,
                    onPlayerInit = { player ->
                        if (player != null) vlcPlayer = player
                    },
                    onSuccess = {
                        android.util.Log.d("PlayerAnalytics", "VLC Success: ${targetChannel.name} (${targetChannel.url})")
                    },
                    onBuffering = { buffering -> 
                        isBuffering = buffering
                        if (!buffering) {
                            loadingStatus = ""
                        } else if (loadingStatus == "Menghubungkan...") {
                            loadingStatus = "Memuat Video..."
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
                        // If VLC fails after Exo fallback, or is the primary and fails
                        errorMessage = it
                        isPlaybackStuck = true 
                        loadingStatus = "Error: $it"
                    }
                )
            } else {
                VideoPlayer(
                    channel = targetChannel,
                    modifier = Modifier.fillMaxSize(),
                    resizeMode = resizeMode,
                    audioBoost = audioBoost,
                    hwAcceleration = hwAcceleration,
                    bufferSize = bufferSize,
                    maxVideoHeight = maxVideoHeight,
                    onPlayerInit = { player ->
                        if (player != null) exoPlayer = player
                    },
                    onSuccess = {
                        android.util.Log.d("PlayerAnalytics", "Exo Success: ${targetChannel.name} (${targetChannel.url})")
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


        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, showChannelList) {
                    detectTapGestures(
                        onTap = {
                            if (!showChannelList) showControls = !showControls
                        }
                    )
                }
        ) {
            if (!isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { gestureType = "Brightness" },
                                    onDragEnd = { gestureType = null },
                                    onDragCancel = { gestureType = null },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val delta = -dragAmount / size.height
                                        brightness = (brightness + delta).coerceIn(0.01f, 1f)
                                        activity?.let {
                                            val lp = it.window.attributes
                                            lp.screenBrightness = brightness
                                            it.window.attributes = lp
                                        }
                                    }
                                )
                            }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { gestureType = "Volume" },
                                    onDragEnd = { gestureType = null },
                                    onDragCancel = { gestureType = null },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val delta = -dragAmount / size.height
                                        volume = (volume + delta).coerceIn(0f, 1f)
                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVol).toInt(), 0)
                                    }
                                )
                            }
                    )
                }
            }
        }

        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
            }
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
                        text = errorMessage ?: "Koneksi Terputus",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (errorMessage != null) "LINK DEAD!." else "Video berhenti diputar.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                exoPlayer = exoPlayer
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
                        val nextChan = currentGroupChannels[idx - 1]
                        currentChannel = nextChan
                        viewModel.markAsPlayed(nextChan)
                    }
                },
                onNext = {
                    val idx = currentGroupChannels.indexOfFirst { it.url == currentChannel.url }
                    if (idx < currentGroupChannels.size - 1) {
                        val nextChan = currentGroupChannels[idx + 1]
                        currentChannel = nextChan
                        viewModel.markAsPlayed(nextChan)
                    }
                },
                onResizeToggle = handleResize,
                isFullscreen = isFullscreen,
                onShowAudio = { showAudioDialog = true },
                onShowResolution = { showResolutionDialog = true },
                onSwitchEngine = {
                    activeEngine = if (activeEngine == "VLC") "EXO" else "VLC"
                },
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

        GestureHUD(gestureType, brightness, volume)

        if (showDebugDialog && isDebug) {
            DebugInfoDialog(
                channel = currentChannel,
                onDismiss = { showDebugDialog = false }
            )
        }

        if (showAudioDialog) {
            TrackSelectionDialog(
                title = "Opsi Audio",
                exoPlayer = exoPlayer,
                trackType = C.TRACK_TYPE_AUDIO,
                onDismiss = { showAudioDialog = false },
                showAudioBoost = true,
                audioBoost = audioBoost,
                onAudioBoostToggle = { viewModel.setAudioBoost(it) }
            )
        }

        if (showResolutionDialog) {
            TrackSelectionDialog(
                title = "Kualitas Video",
                exoPlayer = exoPlayer,
                trackType = C.TRACK_TYPE_VIDEO,
                onDismiss = { showResolutionDialog = false }
            )
        }

        AnimatedVisibility(
            visible = showChannelList,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            QuickChannelList(
                channels = currentGroupChannels,
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
fun GestureHUD(type: String?, brightness: Float, volume: Float) {
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

@AndroidOptIn(UnstableApi::class)
@Composable
fun TrackSelectionDialog(
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

    val sortedTrackItems = remember(tracks, trackType) {
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(240.dp).padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (showAudioBoost && onAudioBoostToggle != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable { onAudioBoostToggle(!audioBoost) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Boost",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = audioBoost,
                            onCheckedChange = onAudioBoostToggle,
                            modifier = Modifier.scale(0.6f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    item {
                        val isAuto = exoPlayer?.trackSelectionParameters?.overrides?.values?.none {
                            it.type == trackType
                        } ?: true

                        TrackItem(
                            label = if (trackType == C.TRACK_TYPE_VIDEO) "Kualitas Otomatis" else "Default",
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

                    items(sortedTrackItems) { (group, trackIndex) ->
                        val format = group.getTrackFormat(trackIndex)
                        val isExplicitlySelected = exoPlayer?.trackSelectionParameters?.overrides?.get(group.mediaTrackGroup)?.trackIndices?.contains(trackIndex) ?: false

                        val label = if (trackType == C.TRACK_TYPE_VIDEO) {
                            "${format.height}p"
                        } else {
                            format.language?.uppercase() ?: "Audio Track ${trackIndex + 1}"
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            label, 
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.bodySmall,
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
                    Text("Debug Info", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DebugInfoItem(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { clipboardManager.setText(AnnotatedString(value)) }
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
    onResizeToggle: () -> Unit,
    onShowAudio: () -> Unit,
    onShowResolution: () -> Unit,
    onSwitchEngine: () -> Unit,
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
                onClick = onSwitchEngine,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(19.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SettingsInputComponent, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playerEngine,
                        color = MaterialTheme.colorScheme.primary,
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
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    val isPlaying = isPlayingState
                    val isError = isPlaybackStuck
                    
                    // Reduced glow size
                    if (isPlaying && !isBuffering) {
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
                                imageVector = if (isError) Icons.Default.Refresh else if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
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
                    PlayerControlAction(icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, tint = if (isFavorite) Color.Red else Color.White, onClick = onToggleFavorite)
                    PlayerControlAction(icon = Icons.Default.Settings, onClick = onShowAudio)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerControlAction(icon = Icons.Default.HighQuality, onClick = onShowResolution)
                    if (isDebug) PlayerControlAction(icon = Icons.Default.BugReport, onClick = onShowDebug)
                    PlayerControlAction(icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, onClick = onResizeToggle)
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
    channels: List<IptvChannel>,
    currentChannel: IptvChannel,
    onChannelSelect: (IptvChannel) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(240.dp),
        color = Color(0xFF1A1A1A).copy(alpha = 0.85f),
        tonalElevation = 8.dp
    ) {

        Column(modifier = Modifier.statusBarsPadding()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp), modifier = Modifier.padding(end = 8.dp)) {
                    Box(modifier = Modifier.width(4.dp).height(20.dp))
                }
                Text(
                    text = currentChannel.group?.uppercase() ?: "SALURAN",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels) { channel ->
                    val selected = channel.url == currentChannel.url
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChannelSelect(channel) }
                            // Softer selection background
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(0.15f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp, 38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(0.05f)),
                            contentScale = ContentScale.Fit,
                            error = painterResource(R.drawable.app_icon_android),
                            placeholder = painterResource(R.drawable.app_icon_android)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = channel.name,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!channel.drmConfig.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "DRM",
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
