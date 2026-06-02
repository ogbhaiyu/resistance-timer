package com.resistancetimer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    @Query("SELECT * FROM app_limits ORDER BY appLabel ASC")
    fun getAllLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getLimit(packageName: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appLimit: AppLimit)

    @Delete
    suspend fun delete(appLimit: AppLimit)

    @Query("UPDATE app_limits SET usedSecondsToday = usedSecondsToday + :seconds WHERE packageName = :packageName")
    suspend fun addUsedSeconds(packageName: String, seconds: Int)

    @Query("UPDATE app_limits SET usedSecondsToday = 0, extraSecondsEarned = 0, lastResetDate = :date WHERE packageName = :packageName")
    suspend fun resetDailyUsage(packageName: String, date: String)

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    fun observeLimit(packageName: String): Flow<AppLimit?>
}
