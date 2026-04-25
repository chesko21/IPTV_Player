package com.chesko.stream_pro.player

import android.media.audiofx.LoudnessEnhancer
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.utils.PlayerUtils

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
    onPlayerInit: ((ExoPlayer) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    val exoPlayer = remember(hwAcceleration, bufferSize) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferSize * 1000, 
                (bufferSize * 2).coerceAtLeast(30) * 1000, 
                2500, 
                5000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            val parametersBuilder = buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setForceLowestBitrate(false)
                .setExceedRendererCapabilitiesIfNecessary(true)
            
            if (maxVideoHeight > 0) {
                // Set max video size based on height, width can be large to not limit aspect ratio
                parametersBuilder.setMaxVideoSize(Int.MAX_VALUE, maxVideoHeight)
            }
            
            setParameters(parametersBuilder)
        }

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (hwAcceleration) 
                    androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON 
                else 
                    androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            )
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

                    override fun onPlayerError(error: PlaybackException) {
                        val errorCode = error.errorCode
                        val errorString = error.errorCodeName
                        
                        if (errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                            seekToDefaultPosition()
                            prepare()
                            return
                        }

                        val detailedMessage = when (errorCode) {
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                                "LINK MATI (404)"
                            }
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                                "KONEKSI GAGAL"
                            }
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                                "TIMEOUT"
                            }
                            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> {
                                "SSL ERROR (Gunakan HTTPS)"
                            }
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                                "FILE TIDAK DITEMUKAN"
                            }
                            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> {
                                "DRM CONTENT ERROR"
                            }
                            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> {
                                "DRM PROVISIONING ERROR"
                            }
                            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                                "DRM ERROR"
                            }
                            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> {
                                "DRM SYSTEM ERROR"
                            }
                            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                                "DECODE ERROR"
                            }
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
                                "FORMAT TIDAK DIDUKUNG"
                            }
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> {
                                "PARSING ERROR"
                            }
                            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                                "LIVE STREAM ERROR"
                            }
                            PlaybackException.ERROR_CODE_REMOTE_ERROR -> {
                                "SERVER ERROR"
                            }
                            else -> "ERROR ($errorString)"
                        }
                        onError?.invoke(detailedMessage)
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

    LaunchedEffect(maxVideoHeight) {
        val player = exoPlayer
        val trackSelector = player.trackSelector as? DefaultTrackSelector
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

    LaunchedEffect(exoPlayer) {
        onPlayerInit?.invoke(exoPlayer)
    }

    LaunchedEffect(channel.url) {
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

    DisposableEffect(lifecycleOwner) {
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

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
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
    }
}
