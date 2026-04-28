package com.chesko.stream_pro_tv.player

import android.media.AudioDeviceInfo
import android.media.audiofx.LoudnessEnhancer
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.utils.PlayerUtils
import com.chesko.stream_pro_tv.ui.screens.UniverseBackground
import androidx.compose.ui.res.stringResource
import com.chesko.stream_pro_tv.R
import kotlinx.coroutines.delay
import org.videolan.libvlc.MediaPlayer

@OptIn(UnstableApi::class)
@Composable
fun TvVideoPlayer(
    channel: IptvChannel,
    modifier: Modifier = Modifier,
    engine: String = "EXO",
    autoPlay: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    hwAcceleration: Boolean = true,
    autoQuality: Boolean = true,
    audioBoost: Boolean = false,
    maxVideoHeight: Int = 0,
    bufferSize: Int = 15,
    onPlayerInit: ((ExoPlayer) -> Unit)? = null,
    onVlcInit: ((MediaPlayer?) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onEngineSwitch: ((String) -> Unit)? = null
) {
    if (engine == "VLC") {
        com.chesko.stream_pro.core.player.VlcVideoPlayer(
            channel = channel,
            modifier = modifier,
            hwAcceleration = hwAcceleration,
            resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) 3 else 0,
            onPlayerInit = onVlcInit,
            onBuffering = { },
            onPlayingChanged = { },
            onError = { errorString: String ->
                onError?.invoke(errorString)
            }
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var retryCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    val exoPlayer = remember(hwAcceleration, autoQuality, bufferSize) {

        val minBufferMs = bufferSize * 1000
        val maxBufferMs = (bufferSize * 3000).coerceAtLeast(30000)
        val bufferForPlaybackMs = (bufferSize * 100).coerceIn(1000, 5000)
        val bufferForPlaybackAfterRebufferMs = (bufferSize * 200).coerceIn(2000, 10000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setMaxVideoSize(
                    if (maxVideoHeight > 0) Int.MAX_VALUE else Int.MAX_VALUE,
                    if (maxVideoHeight > 0) maxVideoHeight else Int.MAX_VALUE
                )
                .setForceLowestBitrate(false)
                .setExceedRendererCapabilitiesIfNecessary(true))
        }

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(
                if (hwAcceleration) 
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                else 
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = autoPlay

                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        try {
                            loudnessEnhancer?.release()
                            if (audioSessionId != AudioDeviceInfo.TYPE_UNKNOWN) {
                                val enhancer = LoudnessEnhancer(audioSessionId)
                                if (audioBoost) {
                                    enhancer.setTargetGain(3000)
                                    enhancer.enabled = true
                                }
                                loudnessEnhancer = enhancer
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                errorMessage = null
                            }
                            Player.STATE_READY -> {
                                retryCount = 0
                                errorMessage = null
                            }
                            Player.STATE_IDLE -> {
                                // Jika idle tanpa error setelah mencoba, anggap stuck
                                if (this@apply.playerError == null && retryCount >= 3) {
                                    errorMessage = context.getString(R.string.error_video_stuck)
                                    onError?.invoke(errorMessage!!)
                                }
                            }
                            Player.STATE_ENDED -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                            seekToDefaultPosition()
                            prepare()
                            return
                        }

                        val cause = error.cause
                        val errorMsg = if (cause is HttpDataSource.InvalidResponseCodeException) {
                            when (cause.responseCode) {
                                404 -> context.getString(R.string.error_404)
                                403 -> context.getString(R.string.error_403)
                                401 -> context.getString(R.string.error_401)
                                504 -> context.getString(R.string.error_504)
                                500, 502, 503 -> context.getString(R.string.error_server_issue, cause.responseCode)
                                else -> context.getString(R.string.error_load_failed, cause.responseCode)
                            }
                        } else {
                            when (error.errorCode) {
                                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> context.getString(R.string.error_http_connection)
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> context.getString(R.string.error_network_lost)
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> context.getString(R.string.error_timeout)
                                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> context.getString(R.string.error_protocol)
                                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> context.getString(R.string.error_file_not_found)
                                PlaybackException.ERROR_CODE_DECODING_FAILED -> context.getString(R.string.error_decoding)
                                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> context.getString(R.string.error_unsupported_format)
                                PlaybackException.ERROR_CODE_REMOTE_ERROR -> context.getString(R.string.error_remote)
                                else -> context.getString(R.string.error_unknown, error.errorCodeName)
                            }
                        }

                        if (retryCount < 3) {
                            retryCount++
                        } else {
                            // Fallback to VLC if Exo fails
                            if (engine == "EXO" && onEngineSwitch != null) {
                                onEngineSwitch("VLC")
                                return
                            }
                            errorMessage = errorMsg
                            onError?.invoke(errorMsg)
                        }
                    }
                })
            }
    }

    LaunchedEffect(audioBoost) {
        try {
            loudnessEnhancer?.let { enhancer ->
                if (audioBoost) {
                    enhancer.setTargetGain(3000) // 30dB boost
                    enhancer.enabled = true
                } else {
                    enhancer.enabled = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(retryCount) {
        if (retryCount in 1..3) {
            delay(5000)
            exoPlayer.stop()
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    LaunchedEffect(exoPlayer) {
        onPlayerInit?.invoke(exoPlayer)
    }

    LaunchedEffect(channel.url) {
        retryCount = 0
        errorMessage = null
        
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val mediaItem = PlayerUtils.buildMediaItem(channel)
        val allHeaders = PlayerUtils.getHeadersFromChannel(channel)
        
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(allHeaders)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(dataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)

        val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view -> 
                if (view.resizeMode != resizeMode) {
                    view.resizeMode = resizeMode
                }
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Error State
            errorMessage?.let { msg ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    UniverseBackground(
                        primaryColor = MaterialTheme.colorScheme.error,
                        glowAlpha = 0.5f
                    )
                    
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                            .padding(horizontal = 40.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            onClick = {},
                            shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.CircleShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.player_system_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg.uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                fontSize = if (isSmallScreen) 16.sp else 18.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.alpha(0.5f)
                        ) {
                            Text(
                                text = stringResource(R.string.player_re_sync),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isSmallScreen) 9.sp else 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.player_signal_lost),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isSmallScreen) 8.sp else 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
