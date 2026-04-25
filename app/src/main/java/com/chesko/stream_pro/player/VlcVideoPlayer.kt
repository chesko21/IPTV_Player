package com.chesko.stream_pro.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chesko.stream_pro.core.data.model.IptvChannel
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun VlcVideoPlayer(
    channel: IptvChannel,
    modifier: Modifier = Modifier,
    hwAcceleration: Boolean = true,
    resizeMode: Int = 0,
    onPlayerInit: ((MediaPlayer?) -> Unit)? = null,
    onBuffering: ((Boolean) -> Unit)? = null,
    onPlayingChanged: ((Boolean) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnError by rememberUpdatedState(onError)
    val currentOnBuffering by rememberUpdatedState(onBuffering)
    val currentOnPlayingChanged by rememberUpdatedState(onPlayingChanged)
    val currentOnPlayerInit by rememberUpdatedState(onPlayerInit)

    val libVLC = remember { 
        val options = arrayListOf<String>().apply {

            add("--no-drop-late-frames")
            add("--no-skip-frames")
            add("--rtsp-tcp")
            add("--network-caching=3000")
            add("--live-caching=3000")
            add("--clock-jitter=500")
            add("--clock-synchro=0")
            
            // Fix glitches and stability
            add("--http-reconnect")
            
            // Suppress noisy logs
            add("--no-stats")
            add("--no-osd")
            add("--no-video-title-show")

            // Stream parsing and H.264 specific fixes
            add("--avcodec-skiploopfilter=1")
            add("--avcodec-hw=any")
            
            if (hwAcceleration) {
                add("--codec=mediacodec_ndk,mediacodec_jni,all")
            } else {
                add("--codec=all")
            }
        }
        
        try {
            LibVLC(context.applicationContext, options)
        } catch (_: Exception) {
            LibVLC(context.applicationContext)
        }
    }
    
    val mediaPlayer = remember { MediaPlayer(libVLC) }

    // Handle resize mode aligned with Media3 constants
    LaunchedEffect(resizeMode, mediaPlayer) {
        mediaPlayer.videoScale = when (resizeMode) {
            0 -> MediaPlayer.ScaleType.SURFACE_BEST_FIT // FIT
            3 -> MediaPlayer.ScaleType.SURFACE_FILL     // FILL
            4 -> MediaPlayer.ScaleType.SURFACE_16_9     // ZOOM -> 16:9 for TV feel
            else -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
        }
    }

    LaunchedEffect(mediaPlayer) {
        currentOnPlayerInit?.invoke(mediaPlayer)
    }

    // Add event listener to handle media events
    val eventListener = remember {
        MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.EncounteredError -> {
                    currentOnError?.invoke("VLC encountered an error (Check if link is valid)")
                }
                MediaPlayer.Event.Buffering -> {
                    currentOnBuffering?.invoke(event.buffering < 100f)
                }
                MediaPlayer.Event.Playing -> {
                    currentOnBuffering?.invoke(false)
                    currentOnPlayingChanged?.invoke(true)
                }
                MediaPlayer.Event.Vout -> {
                    currentOnBuffering?.invoke(false)
                    currentOnPlayingChanged?.invoke(true)
                }
                MediaPlayer.Event.Paused -> {
                    currentOnPlayingChanged?.invoke(false)
                }
                MediaPlayer.Event.Stopped -> {
                    currentOnPlayingChanged?.invoke(false)
                }
                else -> {}
            }
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer.setEventListener(eventListener)
        onDispose {
            mediaPlayer.setEventListener(null)
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVLC.release()
            currentOnPlayerInit?.invoke(null)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!mediaPlayer.isPlaying && mediaPlayer.hasMedia()) {
                        mediaPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(channel.url) {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                delay(200) // Give VLC a moment to clean up resources
            }
            
            // Clean and encode URL to fix HTTP 400 issues with spaces/special characters
            val cleanUrl = channel.url.trim().replace(" ", "%20")
            val media = Media(libVLC, Uri.parse(cleanUrl))
            
            // Add options for stability and malformed streams
            media.addOption(":network-caching=10000") // Increased to 10s for TLS stability
            media.addOption(":clock-jitter=500")
            media.addOption(":clock-synchro=0")
            media.addOption(":http-reconnect=true")
            media.addOption(":http-continuous=true")
            media.addOption(":ipv4") // Force IPv4 to avoid TLS handshake issues on some networks
            
            // H.264 / HLS specific fixes
            media.addOption(":packetizer-avc-insert-sps-pps")
            media.addOption(":m3u8-ext-x-key")
            media.addOption(":codec=all")
            media.addOption(":hls-caching=10000")
            
            // Force higher caching for unstable IPTV links
            media.addOption(":file-caching=10000")
            media.addOption(":live-caching=10000")
            
            // Help with some specific HLS errors and bypass some bot protections
            media.addOption(":http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            
            // Add headers if any (this will override the default if provided)
            channel.userAgent?.let { if (it.isNotEmpty()) media.addOption(":http-user-agent=$it") }
            channel.referrer?.let { media.addOption(":http-referrer=$it") }
            channel.cookie?.let { if (it.isNotEmpty()) media.addOption(":http-cookie=$it") }
            
            mediaPlayer.media = media
            media.release()
            
            mediaPlayer.play()
        } catch (e: Exception) {
            currentOnError?.invoke("VLC Error: ${e.message}")
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Ensure the layout itself doesn't have a weird background
                    setBackgroundColor(android.graphics.Color.BLACK)
                    mediaPlayer.attachViews(this, null, true, false)
                }
            },
            update = { /* No updates needed here */ },
            modifier = Modifier.fillMaxSize()
        )
    }
}
