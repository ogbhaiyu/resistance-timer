package com.resistancetimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.resistancetimer.service.AppWatcherService

/**
 * Restarts the watcher service when the phone reboots,
 * so the user doesn't have to open the app each day.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(
                Intent(context, AppWatcherService::class.java)
            )
        }
    }
}
