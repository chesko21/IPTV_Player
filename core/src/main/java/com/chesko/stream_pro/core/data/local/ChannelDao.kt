package com.chesko.stream_pro.core.data.local

import androidx.room.*
import com.chesko.stream_pro.core.data.model.IptvChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY name")
    fun getAllChannels(): Flow<List<IptvChannel>>

    @Query("SELECT * FROM channels WHERE is_favorite = 1 ORDER BY name")
    fun getFavoriteChannels(): Flow<List<IptvChannel>>

    @Query("SELECT * FROM channels WHERE last_played IS NOT NULL ORDER BY last_played DESC LIMIT 20")
    fun getRecentlyPlayed(): Flow<List<IptvChannel>>

    @Query("SELECT * FROM channels WHERE group_name = :groupName ORDER BY name")
    fun getChannelsByGroup(groupName: String): Flow<List<IptvChannel>>

    @Query("SELECT * FROM channels WHERE group_name IS NULL OR group_name = '' ORDER BY name")
    fun getUngroupedChannels(): Flow<List<IptvChannel>>

    @Query("SELECT DISTINCT group_name FROM channels WHERE group_name IS NOT NULL AND group_name != '' ORDER BY group_name")
    fun getAllGroups(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM channels WHERE group_name IS NULL OR group_name = '')")
    fun hasUngroupedChannels(): Flow<Boolean>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR group_name LIKE '%' || :query || '%' ORDER BY name")
    fun searchChannels(query: String): Flow<List<IptvChannel>>

    @Query("SELECT * FROM channels WHERE TRIM(url) = TRIM(:url) LIMIT 1")
    suspend fun getChannelByUrl(url: String): IptvChannel?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<IptvChannel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: IptvChannel)

    @Update
    suspend fun updateChannel(channel: IptvChannel)

    @Query("UPDATE channels SET is_favorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavoriteStatus(channelId: Int, isFavorite: Boolean)

    @Query("UPDATE channels SET is_favorite = :isFavorite WHERE TRIM(url) = TRIM(:url)")
    suspend fun updateFavoriteStatusByUrl(url: String, isFavorite: Boolean)

    @Query("UPDATE channels SET last_played = :timestamp WHERE TRIM(url) = TRIM(:url)")
    suspend fun updateLastPlayed(url: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM channels")
    fun getTotalChannelCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE is_favorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("DELETE FROM channels WHERE is_favorite = 0 AND (last_played IS NULL OR last_played < :olderThan)")
    suspend fun cleanupOldNonFavorites(olderThan: Long)

    @Query("DELETE FROM channels WHERE playlist_id = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Int)

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()

    @Query("UPDATE channels SET last_played = NULL")
    suspend fun clearRecentlyPlayed()

    @Query("UPDATE channels SET is_favorite = 0")
    suspend fun clearFavorites()
}
