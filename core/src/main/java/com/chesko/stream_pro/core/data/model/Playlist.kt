package com.chesko.stream_pro.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val url: String,
    val epgUrl: String? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
