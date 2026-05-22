package com.chesko.stream_pro.core.data.parser

import com.chesko.stream_pro.core.data.model.IptvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.regex.Pattern

object M3uParser {

    private val LOGO_PATTERN = Pattern.compile("""tvg-logo=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val GROUP_PATTERN = Pattern.compile("""group-title=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val ID_PATTERN = Pattern.compile("""tvg-id=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val NAME_PATTERN = Pattern.compile("""tvg-name=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val CHNO_PATTERN = Pattern.compile("""(?:tvg-chno|ch-number)=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val EPG_URL_PATTERN = Pattern.compile("""(?:x-tvg-url|url-tvg)=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)

    fun parse(content: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentChannelNumber: String? = null
        var currentUserAgent: String? = null
        var currentReferrer: String? = null
        var currentCookie: String? = null
        var currentDrmConfig: String? = null

        content.lineSequence().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach

            when {
                trimmedLine.startsWith("#EXTM3U") -> {
                    // Check for global attributes if needed
                }

                trimmedLine.startsWith("#EXTINF:") -> {
                    // 1. Tangani jika ada tag lain yang menempel di baris yang sama (inline tags)
                    val infPart = trimmedLine

                    // 2. Ekstraksi atribut standard
                    currentLogo = findValue(infPart, LOGO_PATTERN) ?: currentLogo
                    currentGroup = findValue(infPart, GROUP_PATTERN) ?: currentGroup
                    currentTvgId = findValue(infPart, ID_PATTERN) ?: currentTvgId
                    currentTvgName = findValue(infPart, NAME_PATTERN) ?: currentTvgName
                    currentChannelNumber = findValue(infPart, CHNO_PATTERN) ?: currentChannelNumber
                    
                    // 3. Ekstraksi nama (setelah koma terakhir)
                    val lastCommaIndex = infPart.lastIndexOf(",")
                    if (lastCommaIndex != -1) {
                        var name = infPart.substring(lastCommaIndex + 1).trim()
                        // Bersihkan nama jika masih mengandung sisa-sisa tag atau separator
                        if (name.contains("#EXT")) name = name.substringBefore("#EXT").trim()
                        currentName = name
                    }
                }

                trimmedLine.startsWith("#EXTGRP:") -> {
                    currentGroup = trimmedLine.substringAfter(":").trim()
                }

                trimmedLine.startsWith("#KODIPROP:") -> {
                    val prop = trimmedLine.substringAfter(":").trim()
                    currentDrmConfig = appendConfig(currentDrmConfig, prop)
                }

                !trimmedLine.startsWith("#") -> {
                    val name = currentName ?: ""
                    val isBlocked = name.contains("001 TRAKTIR KOPI", ignoreCase = true) ||
                                    name.contains("! 01 TV GEULIS PISAN", ignoreCase = true)||
                                    name.contains("! 01 JOIN GROUP" , ignoreCase = true)||
                                    name.contains("! 02 JOIN GROUP" , ignoreCase = true)||
                                    name.contains("! 03 JANGAN DIBUKA" , ignoreCase = true)||
                                    name.contains("! 02 TRAKTIR NGOPI" , ignoreCase = true)||
                                    name.contains("KBTRTV", ignoreCase = true)

                    if (!name.isBlank() && !isBlocked && !isSeparator(name, trimmedLine)) {
                        var finalUrl = trimmedLine.trim()
                        var finalUserAgent = currentUserAgent
                        var finalReferrer = currentReferrer
                        var finalCookie = currentCookie
                        var finalDrmConfig = currentDrmConfig

                        if (finalUrl.contains("|")) {
                            val parts = finalUrl.split("|", limit = 2)
                            finalUrl = parts[0].trim()
                            
                            parts[1].split("&").forEach { param ->
                                parseKeyValuePair(param) { key, value ->
                                    when (key) {
                                        "user-agent" -> finalUserAgent = value
                                        "referer", "referrer" -> finalReferrer = value
                                        "cookie" -> finalCookie = value
                                        else -> finalDrmConfig = appendConfig(finalDrmConfig, "$key=$value")
                                    }
                                }
                            }
                        }

                        val rawGroup = currentGroup?.replace("kbtrtv", "", ignoreCase = true)?.trim()
                        val decodedGroup = try {
                            if (rawGroup?.contains("+") == true || rawGroup?.contains("%") == true) {
                                java.net.URLDecoder.decode(rawGroup, "UTF-8")
                            } else rawGroup
                        } catch (_: Exception) { rawGroup }

                        channels.add(
                            IptvChannel(
                                name = currentName!!,
                                url = finalUrl,
                                logo = currentLogo,
                                group = decodedGroup?.takeIf { it.isNotEmpty() },
                                tvgId = currentTvgId,
                                tvgName = currentTvgName,
                                channelNumber = currentChannelNumber,
                                userAgent = finalUserAgent,
                                referrer = finalReferrer,
                                cookie = finalCookie,
                                drmConfig = finalDrmConfig
                            )
                        )
                    }

                    currentName = null
                    currentLogo = null
                    currentDrmConfig = null
                    currentUserAgent = null
                    currentReferrer = null
                    currentCookie = null
                    currentTvgId = null
                    currentTvgName = null
                    currentChannelNumber = null
                }
            }
        }
        return channels
    }

    private fun parseKeyValuePair(input: String, onParsed: (String, String) -> Unit) {
        if (input.contains("=")) {
            val key = input.substringBefore("=").trim().lowercase()
            val value = input.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
            onParsed(key, value)
        }
    }

    private fun appendConfig(current: String?, new: String): String {
        return if (current == null) new else "$current|$new"
    }

    private fun findValue(line: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(line)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun isSeparator(name: String, url: String): Boolean {
        val isDummyUrl = url.contains("=====") || url.contains("-----") || url.endsWith("/")
        val isHeaderName = name.contains("=====") || name.contains("-----")
        return isDummyUrl || (isHeaderName && url.contains("githubusercontent"))
    }

    fun extractEpgUrls(content: String): List<String> {
        val urls = mutableListOf<String>()
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() }
        if (firstLine != null && firstLine.startsWith("#EXTM3U")) {
            val matcher = EPG_URL_PATTERN.matcher(firstLine)
            while (matcher.find()) {
                matcher.group(1)?.let { urls.add(it) }
            }

            if (firstLine.contains("url-tvg=")) {
                val valPart = firstLine.substringAfter("url-tvg=").substringAfter("\"").substringBefore("\"")
                if (valPart.contains(",")) {
                    urls.addAll(valPart.split(",").map { it.trim() })
                }
            }
        }
        return urls.distinct()
    }

    suspend fun parseFromUrl(url: String): List<IptvChannel> {
        return withContext(Dispatchers.IO) {
            try {
                val content = URL(url).readText()
                parse(content)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
