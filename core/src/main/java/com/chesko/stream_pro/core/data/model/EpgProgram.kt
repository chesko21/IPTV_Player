package com.chesko.stream_pro.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ]
)
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val category: String? = null
)
