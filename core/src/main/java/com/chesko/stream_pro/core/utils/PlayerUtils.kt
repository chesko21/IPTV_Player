package com.chesko.stream_pro.core.utils

import android.net.Uri
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.chesko.stream_pro.core.data.model.IptvChannel
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

object PlayerUtils {

    fun buildMediaItem(channel: IptvChannel): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(channel.url)
            .setMediaId(channel.url)
            .setTag(channel)

        when {
            channel.url.contains(".mpd") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                mediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(30000)
                        .build()
                )
            }
            channel.url.contains(".m3u8") -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            channel.url.contains(".ism") -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_SS)
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

    fun getHeadersFromChannel(channel: IptvChannel): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        headers["User-Agent"] = channel.userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        channel.referrer?.let { 
            headers["Referer"] = it 
            try {
                val uri = URI(it)
                headers["Origin"] = "${uri.scheme}://${uri.host}"
            } catch (e: Exception) {}
        }
        channel.cookie?.let { headers["Cookie"] = it }

        val url = channel.url.lowercase()
        if (url.contains("indihome") || url.contains("telkom") || url.contains("jtedge")) {
            headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
            headers["X-Requested-With"] = "com.indihome.itv"
            headers["Origin"] = "https://www.indihometv.com"
            headers["Referer"] = "https://www.indihometv.com/"
            headers["Accept"] = "*/*"
            headers["Connection"] = "keep-alive"
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
                        "http-referrer" to "Referer"
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
