package com.chesko.stream_pro.core.data.local

import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.data.model.EpgProgram
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao
) {

    // Channel Logic
    val allChannels: Flow<List<IptvChannel>> = channelDao.getAllChannels()
    val favoriteChannels: Flow<List<IptvChannel>> = channelDao.getFavoriteChannels()
    val recentlyPlayed: Flow<List<IptvChannel>> = channelDao.getRecentlyPlayed()
    val favoriteCount: Flow<Int> = channelDao.getFavoriteCount()
    val totalChannelCount: Flow<Int> = channelDao.getTotalChannelCount()

    fun getChannelsByGroup(groupName: String): Flow<List<IptvChannel>> {
        return if (groupName == "Other") {
            channelDao.getUngroupedChannels()
        } else {
            channelDao.getChannelsByGroup(groupName)
        }
    }

    fun getAllGroups(): Flow<List<String>> {
        return combine(
            channelDao.getAllGroups(),
            channelDao.hasUngroupedChannels()
        ) { groups, hasOther ->
            if (hasOther) {
                groups + "Other"
            } else {
                groups
            }
        }
    }

    fun searchChannels(query: String): Flow<List<IptvChannel>> {
        return if (query.isBlank()) {
            allChannels
        } else {
            channelDao.searchChannels(query)
        }
    }

    suspend fun getChannelByUrl(url: String): IptvChannel? {
        return channelDao.getChannelByUrl(url)
    }

    suspend fun clearAllChannels() {
        channelDao.deleteAllChannels()
    }

    suspend fun syncChannels(newChannels: List<IptvChannel>) {
        val currentFavorites = favoriteChannels.first().associateBy { it.url }
        
        // Clear all channels to ensure we don't have leftovers from previous playlists
        channelDao.deleteAllChannels()

        val channelsToInsert = newChannels.map { newChannel ->
            val existing = currentFavorites[newChannel.url]
            if (existing != null) {
                newChannel.copy(
                    isFavorite = true,
                    lastPlayed = existing.lastPlayed
                )
            } else {
                newChannel
            }
        }

        channelDao.insertChannels(channelsToInsert)
    }

    suspend fun toggleFavorite(channel: IptvChannel) {
        channelDao.updateFavoriteStatusByUrl(channel.url, !channel.isFavorite)
    }

    suspend fun updateFavoriteStatus(channelId: Int, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(channelId, isFavorite)
    }

    suspend fun updateFavoriteStatusByUrl(url: String, isFavorite: Boolean) {
        channelDao.updateFavoriteStatusByUrl(url, isFavorite)
    }

    suspend fun markAsPlayed(channel: IptvChannel) {
        channelDao.updateLastPlayed(channel.url, System.currentTimeMillis())
    }

    suspend fun clearRecentlyPlayed() {
        channelDao.clearRecentlyPlayed()
    }

    // EPG Logic
    fun getProgramsForChannel(tvgId: String): Flow<List<EpgProgram>> {
        return epgDao.getProgramsForChannel(tvgId, System.currentTimeMillis())
    }

    fun getCurrentProgram(tvgId: String): Flow<EpgProgram?> {
        return epgDao.getCurrentProgram(tvgId, System.currentTimeMillis())
    }

    fun getNextProgram(tvgId: String): Flow<EpgProgram?> {
        return epgDao.getNextProgram(tvgId, System.currentTimeMillis())
    }

    suspend fun syncEpg(programs: List<EpgProgram>) {
        epgDao.insertPrograms(programs)
        // Cleanup data lama yang sudah lewat lebih dari 24 jam
        epgDao.deleteOldPrograms(System.currentTimeMillis() - (24 * 60 * 60 * 1000))
    }

    suspend fun insertEpgBatch(programs: List<EpgProgram>) {
        epgDao.insertPrograms(programs)
    }

    suspend fun cleanupOldEpg() {
        epgDao.deleteOldPrograms(System.currentTimeMillis() - (24 * 60 * 60 * 1000))
    }

    suspend fun clearEpg() {
        epgDao.clearAll()
    }
}
