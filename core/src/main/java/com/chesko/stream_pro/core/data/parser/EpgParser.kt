package com.chesko.stream_pro.core.data.parser

import android.util.Xml
import com.chesko.stream_pro.core.data.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object EpgParser {
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val fallbackFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

    /**
     * Parse EPG XML stream and process in batches to save memory.
     */
    suspend fun parse(inputStream: InputStream, onBatchParsed: suspend (List<EpgProgram>) -> Unit) {
        val bis = java.io.BufferedInputStream(inputStream)
        bis.mark(1024)
        val head = ByteArray(2)
        val read = bis.read(head)
        bis.reset()
        
        val actualStream = if (read == 2 && head[0] == 0x1f.toByte() && head[1] == 0x8b.toByte()) {
            java.util.zip.GZIPInputStream(bis)
        } else {
            bis
        }

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(actualStream, "UTF-8")

        val batch = mutableListOf<EpgProgram>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "programme") {
                batch.add(readProgramme(parser))
                if (batch.size >= 500) {
                    onBatchParsed(batch.toList())
                    batch.clear()
                }
            }
            eventType = parser.next()
        }
        if (batch.isNotEmpty()) {
            onBatchParsed(batch)
        }
    }

    private fun readProgramme(parser: XmlPullParser): EpgProgram {
        val channelId = parser.getAttributeValue(null, "channel")
        val startStr = parser.getAttributeValue(null, "start")
        val endStr = parser.getAttributeValue(null, "stop")
        
        val startTime = parseDate(startStr)
        val endTime = parseDate(endStr)
        
        var title = ""
        var description = ""

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.END_TAG && parser.name == "programme") break
            
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = readText(parser)
                    "desc" -> description = readText(parser)
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }

        return EpgProgram(
            channelId = channelId ?: "",
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime
        )
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null || dateStr.isBlank()) return 0L
        val trimmed = dateStr.trim()
        return try {
            synchronized(dateFormat) {
                dateFormat.parse(trimmed)?.time ?: 0L
            }
        } catch (e: Exception) {
            try {
                if (trimmed.length >= 14) {
                    synchronized(fallbackFormat) {
                        fallbackFormat.parse(trimmed.substring(0, 14))?.time ?: 0L
                    }
                } else 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }
}
