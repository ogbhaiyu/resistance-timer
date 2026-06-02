package com.resistancetimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.resistancetimer.service.AppWatcherService

/**
 * Restarts the watcher service when the phone reboots,
 * so the user doesn't have to open the app each day.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AppWatcherService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Unable to restart watcher after boot", e)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
