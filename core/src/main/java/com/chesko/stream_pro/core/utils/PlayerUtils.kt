package com.chesko.stream_pro.core.utils

import android.net.Uri
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.chesko.stream_pro.core.data.model.IptvChannel
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

object PlayerUtils {

    @OptIn(UnstableApi::class)
    fun buildMediaItem(channel: IptvChannel): MediaItem {
        val cleanUrl = getCleanUrl(channel.url)
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(cleanUrl)
            .setMediaId(cleanUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(channel.name)
                    .setSubtitle(channel.group ?: "IPTV Stream")
                    .setArtist("StreamPro")
                    .setAlbumTitle("Live TV")
                    .setArtworkUri(if (!channel.logo.isNullOrBlank() && channel.logo.startsWith("http")) Uri.parse(channel.logo) else null)
                    .build()
            )
            .setTag(channel)

        val urlLower = cleanUrl.lowercase()
        when {
            urlLower.contains(".mpd") || urlLower.contains("format=mpd") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                mediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(30000)
                        .build()
                )
            }
            urlLower.contains(".m3u8") || urlLower.contains(".m3u") || urlLower.contains("format=m3u8") || urlLower.contains("/hls/") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                mediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(10000)
                        .build()
                )
            }
            urlLower.contains(".ism") || urlLower.contains("/manifest") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_SS)
            }
            urlLower.contains(".ts") || urlLower.contains("mpegts") || urlLower.contains("protocol=ts") -> {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
            urlLower.contains(".mp4") -> {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
            }
            urlLower.contains(".mkv") -> {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MATROSKA)
            }
            urlLower.contains("drive.google.com") || urlLower.contains("googleusercontent.com") -> {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_UNKNOWN) // Let ExoPlayer sniff the stream
            }
            urlLower.startsWith("rtsp://") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_RTSP)
            }
            else -> {

            }
        }

        val allHeaders = getHeadersFromChannel(channel)

        channel.drmConfig?.let { config ->
            val properties = config.split("|")
            var licenseUri: String? = null
            var drmType = C.WIDEVINE_UUID

            properties.forEach { prop ->
                val trimmedProp = prop.trim()
                val lowerProp = trimmedProp.lowercase()
                
                when {
                    lowerProp.contains("license_key=") || lowerProp.contains("license_url=") -> {
                        licenseUri = trimmedProp.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                    }
                    lowerProp.contains("license_type=") -> {
                        val type = lowerProp.substringAfter("=").trim().lowercase()
                        if (type.contains("clearkey")) {
                            drmType = C.CLEARKEY_UUID
                        } else if (type.contains("widevine")) {
                            drmType = C.WIDEVINE_UUID
                        } else if (type.contains("playready")) {
                            drmType = C.PLAYREADY_UUID
                        }
                    }
                }
            }

            if (licenseUri != null && !licenseUri!!.startsWith("http")) {
                licenseUri = formatClearKeyLicense(licenseUri!!)
                drmType = C.CLEARKEY_UUID
            }
            if (licenseUri == null && !channel.drmKey.isNullOrBlank() && !channel.drmKeyId.isNullOrBlank()) {
                licenseUri = formatClearKeyLicense("${channel.drmKeyId}:${channel.drmKey}")
                drmType = C.CLEARKEY_UUID
            } else if (licenseUri == null && !channel.drmLicenseUrl.isNullOrBlank()) {
                licenseUri = channel.drmLicenseUrl
                if (channel.drmType?.lowercase()?.contains("clearkey") == true) {
                    drmType = C.CLEARKEY_UUID
                }
            }

            if (licenseUri != null) {
                val drmBuilder = MediaItem.DrmConfiguration.Builder(drmType)
                    .setLicenseUri(licenseUri)
                    .setMultiSession(true)
                    .setForceDefaultLicenseUri(true)
                    .setPlayClearContentWithoutKey(true)
                    .setForceSessionsForAudioAndVideoTracks(true)

                val drmHeaders = allHeaders.toMutableMap()

                if (drmType == C.WIDEVINE_UUID) {
                    drmHeaders["Content-Type"] = "application/octet-stream"
                }
                drmHeaders["Accept"] = "*/*"

                if (drmHeaders.isNotEmpty()) {
                    drmBuilder.setLicenseRequestHeaders(drmHeaders)
                }
                
                mediaItemBuilder.setDrmConfiguration(drmBuilder.build())
            }
        }

        return mediaItemBuilder.build()
    }

    fun getCleanUrl(url: String): String {
        return if (url.contains("|")) {
            url.substringBefore("|").trim()
        } else {
            url.trim()
        }
    }

    fun getHeadersFromChannel(channel: IptvChannel): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        headers["User-Agent"] = if (!channel.userAgent.isNullOrBlank() && !channel.userAgent.lowercase().contains("default")) {
            channel.userAgent
        } else {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
        channel.referrer?.let { headers["Referer"] = it }
        channel.cookie?.let { headers["Cookie"] = it }

        if (channel.url.contains("|")) {
            val parts = channel.url.split("|")
            if (parts.size > 1) {
                val params = parts[1].split("&")
                params.forEach { param ->
                    val pair = param.split("=")
                    if (pair.size == 2) {
                        val key = pair[0].trim()
                        val value = pair[1].trim()
                        when (key.lowercase()) {
                            "user-agent", "ua" -> headers["User-Agent"] = value
                            "referer", "ref" -> headers["Referer"] = value
                            "origin" -> headers["Origin"] = value
                            "cookie" -> headers["Cookie"] = value
                        }
                    }
                }
            }
        }

        val url = channel.url.lowercase()
        if (url.contains("indihome") || url.contains("telkom") || url.contains("jtedge")) {
            headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
            headers["X-Requested-With"] = "com.indihome.itv"
            headers["Origin"] = "https://www.indihometv.com"
            headers["Referer"] = "https://www.indihometv.com/"
            headers["Accept"] = "*/*"
        }

        if (url.contains("dens.tv")) {
            if (!headers.containsKey("Origin")) headers["Origin"] = "https://www.dens.tv"
            if (!headers.containsKey("Referer")) headers["Referer"] = "https://www.dens.tv/"
            headers["X-Requested-With"] = "com.dens.tv"
        }

        if (url.contains("visionplus.id") || url.contains("cloudfront.net")) {
            if (!headers.containsKey("Origin")) headers["Origin"] = "https://www.visionplus.id"
            if (!headers.containsKey("Referer")) headers["Referer"] = "https://www.visionplus.id/"
            headers["X-Requested-With"] = "com.visionplus"
        }

        if (url.contains("izzigo.tv")) {
            if (!headers.containsKey("Referer")) headers["Referer"] = "https://www.izzigo.tv/"
            if (!headers.containsKey("Origin")) headers["Origin"] = "https://www.izzigo.tv"
        }

        if (url.contains("beeline.tv")) {
            headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"
        }

        if (url.contains("100ycdn.com")) {
            headers["Accept"] = "*/*"
            if (!headers.containsKey("Referer")) headers["Referer"] = "https://100ycdn.com/"
        }

        if (url.contains("rtm.gov.my") || url.contains("rtmklik")) {
            headers["Referer"] = "https://rtmklik.rtm.gov.my/"
            headers["Origin"] = "https://rtmklik.rtm.gov.my"
        }

        if (url.contains("antik.sk")) {
            if (!headers.containsKey("Referer")) headers["Referer"] = "https://webtv.sk/"
        }

        if (url.contains("sedotcw3.workers.dev") || url.contains("r-plus")) {
            headers["Origin"] = "https://r-plus.top"
            headers["Referer"] = if (headers.containsKey("Referer")) headers["Referer"]!! else "https://www.rctiplus.com/"
            headers["User-Agent"] = if (headers.containsKey("User-Agent") && headers["User-Agent"] != "android-R+") headers["User-Agent"]!! else "android-R+"
            headers["Accept"] = "*/*"
        } else if (url.contains("bintangstreaming.my.id") || url.contains("dailymotion.php")) {
            headers["Referer"] = "https://bintangstreaming.my.id/"
            headers["Origin"] = "https://bintangstreaming.my.id"
            headers["Accept"] = "*/*"
        } else if (url.contains("turboviplay.com")) {
            headers["Referer"] = "https://turboviplay.com/"
            headers["Origin"] = "https://turboviplay.com"
            headers["Accept"] = "*/*"
        } else if (url.contains("workers.dev")) {
            try {
                val cleanUrl = getCleanUrl(channel.url)
                val uri = URI(cleanUrl)
                val domain = uri.host
                if (domain != null) {
                    headers["Origin"] = "https://$domain"
                    headers["Referer"] = "https://$domain/"
                }
            } catch (e: Exception) {}
            headers["Accept"] = "*/*"
        }

        channel.drmConfig?.let { config ->
            val properties = config.split("|")
            properties.forEach { prop ->
                val trimmedProp = prop.trim()
                if (trimmedProp.startsWith("HEADERS=")) {
                    val jsonHeaders = trimmedProp.substringAfter("HEADERS=").trim()
                    try {
                        if (jsonHeaders.startsWith("{")) {
                            val content = jsonHeaders.substring(1, jsonHeaders.length - 1)
                            val pairs = content.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                            pairs.forEach { pair ->
                                val parts = pair.split(Regex(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"), 2)
                                if (parts.size == 2) {
                                    val key = parts[0].trim().replace("\"", "")
                                    val value = parts[1].trim().replace("\"", "")
                                    headers[key] = value
                                }
                            }
                        }
                    } catch (e: Exception) {}
                } else if (trimmedProp.contains("=")) {
                    val key = trimmedProp.substringBefore("=").trim()
                    val value = trimmedProp.substringAfter("=").trim()
                    val lowerKey = key.lowercase()
                    
                    val headerMap = mapOf(
                        "origin" to "Origin",
                        "referer" to "Referer",
                        "user-agent" to "User-Agent",
                        "x-requested-with" to "X-Requested-With",
                        "http-user-agent" to "User-Agent",
                        "http-referrer" to "Referer",
                        "http-origin" to "Origin",
                        "referrer" to "Referer",
                        "ua" to "User-Agent",
                        "ref" to "Referer"
                    )
                    headerMap[lowerKey]?.let { headers[it] = value }
                }
            }
        }
        return headers
    }

    private fun formatClearKeyLicense(licenseKey: String): String {
        return try {
            val pairs = if (licenseKey.contains(",")) licenseKey.split(",") else listOf(licenseKey)
            val keysArray = JSONArray()
            
            pairs.forEach { pair ->
                val parts = if (pair.contains(":")) pair.split(":") else listOf("", pair)
                if (parts.size == 2) {
                    val kidHex = parts[0].trim()
                    val keyHex = parts[1].trim()
                    
                    val keyObj = JSONObject().apply {
                        put("kty", "oct")
                        put("kid", base64UrlEncode(kidHex))
                        put("k", base64UrlEncode(keyHex))
                    }
                    keysArray.put(keyObj)
                }
            }
            
            val jwk = JSONObject().apply {
                put("keys", keysArray)
                put("type", "temporary")
            }
            
            "data:application/json;base64," + Base64.encodeToString(
                jwk.toString().toByteArray(),
                Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
            )
        } catch (e: Exception) {
            licenseKey
        }
    }

    private fun base64UrlEncode(hex: String): String {
        return try {
            val bytes = hexToBytes(hex)
            Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
        } catch (e: Exception) {
            hex
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val s = hex.replace(" ", "").replace("0x", "")
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
