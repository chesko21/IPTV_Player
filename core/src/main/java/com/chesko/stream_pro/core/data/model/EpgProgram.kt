package com.chesko.stream_pro.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epg_programs")
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String, // Menghubungkan ke tvg-id di IptvChannel
    val title: String,
    val description: String?,
    val startTime: Long, // Epoch milliseconds
    val endTime: Long,
    val category: String? = null
)
