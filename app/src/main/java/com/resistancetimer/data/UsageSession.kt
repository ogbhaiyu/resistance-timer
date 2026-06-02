package com.resistancetimer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records each scrolling session.
 * - appPackageName: which app was being used (e.g. "com.instagram.android")
 * - appLabel: human-readable name (e.g. "Instagram")
 * - startTimeMillis: when the session started
 * - durationSeconds: total seconds spent before choosing to stop
 * - initialTimerSeconds: the timer they originally set
 * - extensions: how many times they hit "extend"
 */
@Entity(tableName = "usage_sessions")
data class UsageSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackageName: String,
    val appLabel: String,
    val startTimeMillis: Long,
    val durationSeconds: Long,
    val initialTimerSeconds: Int,
    val extensions: Int
)
