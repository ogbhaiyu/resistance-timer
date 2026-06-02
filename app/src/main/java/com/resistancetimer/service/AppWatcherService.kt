package com.resistancetimer.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.resistancetimer.R
import com.resistancetimer.ResistanceAlertActivity
import com.resistancetimer.ResistanceApp
import com.resistancetimer.data.AppDatabase
import com.resistancetimer.data.AppLimit
import com.resistancetimer.data.UsageSession
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Foreground service that polls UsageStatsManager every second to detect
 * which app is in the foreground. When a tracked app is open it counts down
 * the user's daily allowance. When time runs out it shows the soft overlay
 * (ResistanceAlertActivity) on top of whatever app is running — the app behind
 * is NOT killed or paused.
 */
class AppWatcherService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase

    private var currentForegroundPkg: String? = null
    private var sessionStartTime: Long = 0L
    private var alertShownForPkg: String? = null

    private val sessionExtensions: MutableMap<String, Int> = mutableMapOf()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
        startForegroundWithNotification()
        startWatchLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXTEND -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_STICKY
                val extraSeconds = intent.getIntExtra(EXTRA_SECONDS, 300)
                handleExtend(pkg, extraSeconds)
            }
            ACTION_DONE -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_STICKY
                handleDone(pkg)
            }
        }
        return START_STICKY
    }

    // -------------------------------------------------------------------------
    // Core watch loop — polls every second
    // -------------------------------------------------------------------------

    private fun startWatchLoop() {
        scope.launch {
            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            while (isActive) {
                delay(1000L)

                val now = System.currentTimeMillis()
                val foregroundPkg = getForegroundApp(usageStatsManager, now) ?: continue
                val today = dateFormat.format(Date(now))

                if (foregroundPkg == packageName || foregroundPkg == "com.android.systemui") {
                    flushCurrentSession(now)
                    continue
                }

                val limit = db.appLimitDao().getLimit(foregroundPkg)
                if (limit == null) {
                    flushCurrentSession(now)
                    continue
                }

                if (limit.lastResetDate != today) {
                    db.appLimitDao().resetDailyUsage(foregroundPkg, today)
                    sessionExtensions.remove(foregroundPkg)
                }

                if (currentForegroundPkg != foregroundPkg) {
                    flushCurrentSession(now)
                    currentForegroundPkg = foregroundPkg
                    sessionStartTime = now
                    alertShownForPkg = null
                    Log.d(TAG, "Now watching: $foregroundPkg")
                }

                db.appLimitDao().addUsedSeconds(foregroundPkg, 1)

                val updated = db.appLimitDao().getLimit(foregroundPkg) ?: continue
                val remaining = updated.dailyLimitSeconds +
                        updated.extraSecondsEarned -
                        updated.usedSecondsToday

                if (remaining <= 0 && alertShownForPkg != foregroundPkg) {
                    alertShownForPkg = foregroundPkg
                    showResistanceAlert(foregroundPkg, limit)
                }
            }
        }
    }

    /**
     * When a tracked app leaves foreground, save the session to history.
     */
    private suspend fun flushCurrentSession(now: Long) {
        val pkg = currentForegroundPkg ?: return
        if (sessionStartTime == 0L) return

        val duration = (now - sessionStartTime) / 1000
        if (duration < 2) return

        val limit = db.appLimitDao().getLimit(pkg)
        val extensions = sessionExtensions.remove(pkg) ?: 0
        db.usageSessionDao().insert(
            UsageSession(
                appPackageName = pkg,
                appLabel = limit?.appLabel ?: pkg,
                startTimeMillis = sessionStartTime,
                durationSeconds = duration,
                initialTimerSeconds = limit?.dailyLimitSeconds ?: 0,
                extensions = extensions
            )
        )

        currentForegroundPkg = null
        sessionStartTime = 0L
    }

    // -------------------------------------------------------------------------
    // Overlay alert — slides over the running app without killing it
    // -------------------------------------------------------------------------

    private fun showResistanceAlert(packageName: String, limit: AppLimit) {
        val intent = Intent(this, ResistanceAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra(ResistanceAlertActivity.EXTRA_PACKAGE, packageName)
            putExtra(ResistanceAlertActivity.EXTRA_APP_LABEL, limit.appLabel)
            putExtra(ResistanceAlertActivity.EXTRA_LIMIT_SECONDS, limit.dailyLimitSeconds)
        }
        startActivity(intent)
    }

    // -------------------------------------------------------------------------
    // Handle user choice from the alert
    // -------------------------------------------------------------------------

    private fun handleExtend(pkg: String, extraSeconds: Int) {
        scope.launch {
            val limit = db.appLimitDao().getLimit(pkg) ?: return@launch
            db.appLimitDao().upsert(
                limit.copy(extraSecondsEarned = limit.extraSecondsEarned + extraSeconds)
            )
            if (pkg == currentForegroundPkg) {
                sessionExtensions[pkg] = (sessionExtensions[pkg] ?: 0) + 1
            }
            alertShownForPkg = null
        }
    }

    private fun handleDone(pkg: String) {
        alertShownForPkg = null
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getForegroundApp(manager: UsageStatsManager, now: Long): String? {
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - 5000L,
            now
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun startForegroundWithNotification() {
        val notification = NotificationCompat.Builder(this, ResistanceApp.TIMER_CHANNEL_ID)
            .setContentTitle("Resistance Timer Active")
            .setContentText("Watching your app usage in the background")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AppWatcherService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_EXTEND = "com.resistancetimer.EXTEND"
        const val ACTION_DONE = "com.resistancetimer.DONE"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_SECONDS = "extra_seconds"
    }
}
