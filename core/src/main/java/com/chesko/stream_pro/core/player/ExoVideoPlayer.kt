package com.chesko.stream_pro.core.player

import android.media.audiofx.LoudnessEnhancer
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Log
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.chesko.stream_pro.core.R
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.utils.PlayerUtils
import kotlinx.coroutines.delay

import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    channel: IptvChannel,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    audioBoost: Boolean = false,
    hwAcceleration: Boolean = true,
    bufferSize: Int = 15,
    maxVideoHeight: Int = 0,
    isInPipMode: Boolean = false,
    onPlayerInit: ((ExoPlayer?) -> Unit)? = null,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnPlayerInit by rememberUpdatedState(onPlayerInit)
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnError by rememberUpdatedState(onError)

    var retryCount by remember { mutableIntStateOf(0) }
    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    val exoPlayer = remember(hwAcceleration, bufferSize) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferSize * 1000,
                (bufferSize * 2).coerceAtLeast(30) * 1000,
                1000,
                2000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .apply {
                        if (maxVideoHeight > 0) {
                            setMaxVideoSize(Int.MAX_VALUE, maxVideoHeight)
                        }
                    }
            )
        }

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
        }

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = autoPlay

                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        try {
                            loudnessEnhancer?.release()
                            if (audioSessionId != android.media.AudioDeviceInfo.TYPE_UNKNOWN) {
                                val enhancer = LoudnessEnhancer(audioSessionId)
                                enhancer.setTargetGain(if (audioBoost) 2000 else 0)
                                enhancer.enabled = audioBoost
                                loudnessEnhancer = enhancer
                            }
                        } catch (e: Exception) {
                            Log.e("ExoVideoPlayer", "LoudnessEnhancer error", e)
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            retryCount = 0
                            currentOnSuccess?.invoke()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("ExoVideoPlayer", "Player Error: ${error.errorCodeName}", error)
                        
                        val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                           error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                                           error.errorCode == PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ||
                                           error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED

                        if (isNetworkError && retryCount < 5) {
                            retryCount++
                            seekToDefaultPosition()
                            prepare()
                            play()
                            return
                        }

                        val detailedMessage = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Manifest Error (Tautan mungkin memerlukan VPN atau Header khusus)"
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> context.getString(R.string.exo_error_link_dead)
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> context.getString(R.string.exo_error_connection_failed)
                            PlaybackException.ERROR_CODE_DECODING_FAILED -> "Decoding Failed (Masalah Codec atau DRM)"
                            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "DRM License Failed (Kunci ClearKey/Widevine tidak valid)"
                            else -> context.getString(R.string.exo_error_generic, error.errorCodeName)
                        }
                        currentOnError?.invoke(detailedMessage)
                    }
                })
            }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val activity = context as? android.app.Activity
            val isFinishing = activity?.isFinishing == true

            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val inPip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else false

                    if (isFinishing) {
                        exoPlayer.playWhenReady = false
                        exoPlayer.stop()
                    } else if (!inPip && !exoPlayer.isCurrentMediaItemLive) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    val inPip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else false

                    if (!inPip || isFinishing) {
                        exoPlayer.playWhenReady = false
                        exoPlayer.stop()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.playWhenReady = false
                    exoPlayer.stop()
                    exoPlayer.release()
                }
                Lifecycle.Event.ON_RESUME -> {
                    val inPip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else false
                    if (!inPip && !isFinishing) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        currentOnPlayerInit?.invoke(exoPlayer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.playWhenReady = false
            exoPlayer.stop()
            exoPlayer.release()
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            currentOnPlayerInit?.invoke(null)
        }
    }

    LaunchedEffect(audioBoost, exoPlayer) {
        try {
            if (loudnessEnhancer == null && exoPlayer.audioSessionId != android.media.AudioDeviceInfo.TYPE_UNKNOWN) {
                val enhancer = LoudnessEnhancer(exoPlayer.audioSessionId)
                loudnessEnhancer = enhancer
            }
            
            loudnessEnhancer?.let { enhancer ->
                enhancer.setTargetGain(if (audioBoost) 2000 else 0)
                enhancer.enabled = audioBoost
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(maxVideoHeight) {
        val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector
        trackSelector?.let { selector ->
            val parametersBuilder = selector.buildUponParameters()
            if (maxVideoHeight > 0) {
                parametersBuilder.setMaxVideoSize(Int.MAX_VALUE, maxVideoHeight)
            } else {
                parametersBuilder.clearVideoSizeConstraints()
            }
            selector.setParameters(parametersBuilder)
        }
    }

    LaunchedEffect(channel.url, exoPlayer) {
        val activity = context as? android.app.Activity
        if (activity?.isFinishing == true) {
            exoPlayer.stop()
            return@LaunchedEffect
        }

        retryCount = 0
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val mediaItem = PlayerUtils.buildMediaItem(channel)
        val allHeaders = PlayerUtils.getHeadersFromChannel(channel)
        val cleanUrl = PlayerUtils.getCleanUrl(channel.url)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(allHeaders["User-Agent"])
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(allHeaders)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(dataSourceFactory)

        val hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
            .setExtractorFactory(DefaultHlsExtractorFactory(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES, true))
            .setAllowChunklessPreparation(true)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)

        val dashMediaSourceFactory = DashMediaSource.Factory(
            DefaultDashChunkSource.Factory(dataSourceFactory),
            dataSourceFactory
        ).setDrmSessionManagerProvider(drmSessionManagerProvider)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)

        val urlLower = cleanUrl.lowercase()
        val mediaSource = when {
            urlLower.contains(".m3u8") || urlLower.contains(".m3u") || urlLower.contains("format=m3u8") -> {
                hlsMediaSourceFactory.createMediaSource(mediaItem)
            }
            urlLower.contains(".mpd") || urlLower.contains("format=mpd") -> {
                dashMediaSourceFactory.createMediaSource(mediaItem)
            }
            else -> {
                mediaSourceFactory.createMediaSource(mediaItem)
            }
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF0A0A0A))) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    subtitleView?.visibility = android.view.View.GONE
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
                if (view.resizeMode != resizeMode) {
                    view.resizeMode = resizeMode
                }
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPipMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 80.dp)
                    .alpha(0.4f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_android),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.brand_name),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
