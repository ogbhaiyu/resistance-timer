package com.resistancetimer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the user's chosen daily limit for a specific app.
 * e.g. Instagram → 20 minutes per day
 *
 * usedSecondsToday is reset each day by the watcher service.
 */
@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val dailyLimitSeconds: Int,        // how much they allow themselves
    val usedSecondsToday: Int = 0,     // how much they've burned today
    val lastResetDate: String = ""     // "yyyy-MM-dd" — used to detect day rollover
)
