package com.chesko.stream_pro.core.data.local

import androidx.room.*
import com.chesko.stream_pro.core.data.model.EpgProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgram>)

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :tvgId 
        AND endTime > :currentTime 
        ORDER BY startTime ASC
    """)
    fun getProgramsForChannelById(tvgId: String, currentTime: Long): Flow<List<EpgProgram>>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE (channelId LIKE :namePattern OR REPLACE(REPLACE(LOWER(channelId), ' ', ''), '.', '') = :normalizedName) 
        AND endTime > :currentTime 
        ORDER BY startTime ASC
    """)
    fun getProgramsForChannelByName(namePattern: String, normalizedName: String, currentTime: Long): Flow<List<EpgProgram>>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :tvgId 
        AND :currentTime BETWEEN startTime AND endTime 
        LIMIT 1
    """)
    fun getCurrentProgramById(tvgId: String, currentTime: Long): Flow<EpgProgram?>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE (channelId LIKE :namePattern OR REPLACE(REPLACE(LOWER(channelId), ' ', ''), '.', '') = :normalizedName) 
        AND :currentTime BETWEEN startTime AND endTime 
        LIMIT 1
    """)
    fun getCurrentProgramByName(namePattern: String, normalizedName: String, currentTime: Long): Flow<EpgProgram?>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :tvgId 
        AND startTime > :currentTime 
        ORDER BY startTime ASC 
        LIMIT 1
    """)
    fun getNextProgramById(tvgId: String, currentTime: Long): Flow<EpgProgram?>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE (channelId LIKE :namePattern OR REPLACE(REPLACE(LOWER(channelId), ' ', ''), '.', '') = :normalizedName) 
        AND startTime > :currentTime 
        ORDER BY startTime ASC 
        LIMIT 1
    """)
    fun getNextProgramByName(namePattern: String, normalizedName: String, currentTime: Long): Flow<EpgProgram?>

    @Query("DELETE FROM epg_programs WHERE endTime < :currentTime")
    suspend fun deleteOldPrograms(currentTime: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAll()
}
