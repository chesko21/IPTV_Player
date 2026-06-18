package com.chesko.stream_pro.core.data.parser

import android.util.Xml
import com.chesko.stream_pro.core.data.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.*

object EpgParser {


    suspend fun parse(inputStream: InputStream, onBatchParsed: suspend (List<EpgProgram>) -> Unit) {
        val bis = java.io.BufferedInputStream(inputStream, 32 * 1024)
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

        val batch = ArrayList<EpgProgram>(500)
        val channelIdCache = HashMap<String, String>()
        
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "programme") {
                batch.add(readProgramme(parser, channelIdCache))
                if (batch.size >= 500) {
                    onBatchParsed(ArrayList(batch))
                    batch.clear()
                }
            }
            eventType = parser.next()
        }
        if (batch.isNotEmpty()) {
            onBatchParsed(batch)
        }
        channelIdCache.clear()
    }

    private fun readProgramme(parser: XmlPullParser, channelCache: MutableMap<String, String>): EpgProgram {
        val rawChannelId = parser.getAttributeValue(null, "channel") ?: ""
        val channelId = channelCache.getOrPut(rawChannelId) { rawChannelId }
        
        val startStr = parser.getAttributeValue(null, "start")
        val endStr = parser.getAttributeValue(null, "stop")
        
        val startTime = parseEpgDate(startStr)
        val endTime = parseEpgDate(endStr)
        
        var title = ""
        var description = ""

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.END_TAG && parser.name == "programme") break
            
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = readTextContent(parser)
                    "desc" -> description = readTextContent(parser)
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }

        return EpgProgram(
            channelId = channelId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime
        )
    }

    private fun parseEpgDate(dateStr: String?): Long {
        if (dateStr == null || dateStr.length < 14) return 0L
        
        return try {
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(4, 6).toInt() - 1 // 0-based
            val day = dateStr.substring(6, 8).toInt()
            val hour = dateStr.substring(8, 10).toInt()
            val min = dateStr.substring(10, 12).toInt()
            val sec = dateStr.substring(12, 14).toInt()

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.set(year, month, day, hour, min, sec)
            calendar.set(Calendar.MILLISECOND, 0)
            
            var timeMillis = calendar.timeInMillis

            val spaceIndex = dateStr.indexOf(' ')
            if (spaceIndex != -1 && dateStr.length >= spaceIndex + 6) {
                val tzStr = dateStr.substring(spaceIndex + 1).trim()
                if (tzStr.length >= 5) {
                    val sign = if (tzStr[0] == '-') -1 else 1
                    val tzHours = tzStr.substring(1, 3).toInt()
                    val tzMins = tzStr.substring(3, 5).toInt()
                    val offsetMillis = (tzHours * 3600000L + tzMins * 60000L) * sign
                    timeMillis -= offsetMillis
                }
            }
            timeMillis
        } catch (e: Exception) {
            0L
        }
    }

    private fun readTextContent(parser: XmlPullParser): String {
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
            }
        }
    }
}
