package com.chesko.stream_pro.core.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chesko.stream_pro.core.R
import com.chesko.stream_pro.core.data.model.IptvChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Stable VLC IPTV Player
 *
 * Optimized for resilience against jitter, timestamp errors, and TS discontinuities.
 */
@Composable
fun VlcVideoPlayer(
    channel: IptvChannel,
    modifier: Modifier = Modifier,
    hwAcceleration: Boolean = true,
    resizeMode: Int = 0,
    onPlayerInit: ((MediaPlayer?) -> Unit)? = null,
    onBuffering: ((Boolean) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnError by rememberUpdatedState(onError)
    val currentOnBuffering by rememberUpdatedState(onBuffering)
    val currentOnPlayingChanged by rememberUpdatedState(onPlayingChanged)
    val currentOnPlayerInit by rememberUpdatedState(onPlayerInit)
    val currentOnSuccess by rememberUpdatedState(onSuccess)

    var retryCount by remember { mutableIntStateOf(0) }

    val libVLC = remember(hwAcceleration) { 
        val options = arrayListOf<String>().apply {
            add("--drop-late-frames")
            add("--skip-frames")
            add("--rtsp-tcp")
            add("--network-caching=5000")
            add("--file-caching=5000")
            add("--live-caching=5000")
            add("--clock-jitter=1500") 
            add("--clock-synchro=0")
            add("--http-reconnect")
            

            add("--no-avcodec-dr")
            add("--adaptive-logic=bandwidth")
            add("--hls-use-access")
            add("--no-stats")
            add("--no-osd")
            add("--no-video-title-show")
            
            if (hwAcceleration) {
                add("--avcodec-hw=any")
                add("--codec=mediacodec_jni,mediacodec_ndk,all")
            } else {
                add("--avcodec-hw=none")
                add("--codec=all")
            }
        }
        try {
            LibVLC(context.applicationContext, options)
        } catch (_: Exception) {
            LibVLC(context.applicationContext)
        }
    }
    
    val mediaPlayer = remember(libVLC) { MediaPlayer(libVLC) }

    val eventListener = remember {
        MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.EncounteredError -> {
                    currentOnBuffering?.invoke(false)
                    currentOnPlayingChanged?.invoke(false)
                    if (retryCount < 3) {
                        retryCount++
                    } else {
                        currentOnError?.invoke("Playback Error (Discontinuity/Server issue)")
                    }
                }
                MediaPlayer.Event.Buffering -> {
                    currentOnBuffering?.invoke(event.buffering < 100f)
                }
                MediaPlayer.Event.Playing -> {
                    currentOnBuffering?.invoke(false)
                    currentOnPlayingChanged?.invoke(true)
                    currentOnSuccess?.invoke()
                    retryCount = 0
                }
                MediaPlayer.Event.Vout -> {
                    currentOnBuffering?.invoke(false)
                    currentOnPlayingChanged?.invoke(true)
                }
                MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped, MediaPlayer.Event.EndReached -> {
                    currentOnPlayingChanged?.invoke(false)
                }
                else -> {}
            }
        }
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.setEventListener(eventListener)
        currentOnPlayerInit?.invoke(mediaPlayer)
        onDispose {
            currentOnPlayerInit?.invoke(null)
            mediaPlayer.setEventListener(null)
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (_: Exception) {}
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVLC.release()
        }
    }

    LaunchedEffect(resizeMode, mediaPlayer) {
        mediaPlayer.videoScale = when (resizeMode) {
            0 -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
            3 -> MediaPlayer.ScaleType.SURFACE_FILL
            4 -> MediaPlayer.ScaleType.SURFACE_16_9
            else -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
        }
    }

    DisposableEffect(lifecycleOwner, mediaPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        if (mediaPlayer.isPlaying) mediaPlayer.pause()
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        if (!mediaPlayer.isPlaying && mediaPlayer.hasMedia()) mediaPlayer.play()
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(channel.url, channel.userAgent, channel.referrer, channel.cookie, mediaPlayer, retryCount) {
        var media: Media? = null
        try {
            val isPlaying = try { mediaPlayer.isPlaying } catch (_: Exception) { false }
            if (isPlaying) {
                mediaPlayer.stop()
                delay(300)
            }

            val cleanUrl = channel.url.trim().replace(" ", "%20")
            media = Media(libVLC, cleanUrl.toUri())

            // Media-specific resilience options
            media.addOption(":network-caching=5000")
            media.addOption(":http-reconnect=true")
            media.addOption(":no-ssl-verify")
            media.addOption(":clock-jitter=1500")
            media.addOption(":clock-synchro=0")
            media.addOption(":hls-use-access=1")

            val effectiveUserAgent = if (!channel.userAgent.isNullOrBlank()) {
                channel.userAgent
            } else {
                "Mozilla/5.0"
            }
            media.addOption(":http-user-agent=$effectiveUserAgent")

            channel.referrer?.let { media.addOption(":http-referrer=$it") }
            channel.cookie?.let { if (it.isNotEmpty()) media.addOption(":http-cookie=$it") }
            
            mediaPlayer.media = media
            mediaPlayer.play()
            
        } catch (e: Exception) {
            if (e !is CancellationException) {
                if (retryCount < 3) {
                    retryCount++
                } else {
                    currentOnError?.invoke("Error: ${e.message}")
                }
            }
        } finally {
            media?.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        key(mediaPlayer) {
            AndroidView(
                factory = { ctx ->
                    VLCVideoLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        mediaPlayer.attachViews(this, null, true, false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Watermark
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 48.dp)
                .alpha(0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon_android),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "StreamPro",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
