package com.resistancetimer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {

    @Insert
    suspend fun insert(session: UsageSession)

    @Query("SELECT * FROM usage_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<UsageSession>>

    @Query("SELECT * FROM usage_sessions WHERE startTimeMillis >= :sinceMillis ORDER BY startTimeMillis DESC")
    fun getSessionsSince(sinceMillis: Long): Flow<List<UsageSession>>

    @Query("SELECT SUM(durationSeconds) FROM usage_sessions WHERE startTimeMillis >= :sinceMillis")
    fun getTotalSecondsSince(sinceMillis: Long): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM usage_sessions WHERE startTimeMillis >= :startMillis AND startTimeMillis < :endMillis")
    suspend fun getTotalSecondsBetween(startMillis: Long, endMillis: Long): Long?

    @Query("SELECT DISTINCT appPackageName FROM usage_sessions")
    fun getTrackedApps(): Flow<List<String>>
}
