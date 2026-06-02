package com.resistancetimer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.resistancetimer.data.AppDatabase

class ResistanceApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Silent persistent channel for the watcher service notification
            val watcherChannel = NotificationChannel(
                WATCHER_CHANNEL_ID,
                "Background Watcher",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps Resistance Timer running in the background"
                setShowBadge(false)
            }

            // High-priority channel for the Resistance alert
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Resistance Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires when you've hit your daily limit"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(watcherChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val WATCHER_CHANNEL_ID = "resistance_watcher_channel"
        const val ALERT_CHANNEL_ID   = "resistance_alert_channel"

        // Keep this for legacy references
        const val TIMER_CHANNEL_ID = WATCHER_CHANNEL_ID
    }
}
