package com.resistancetimer.service

import android.app.Service
import android.app.usage.UsageEvents
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
    private var lastAlertedPkg: String? = null
    private var lastKnownForegroundPkg: String? = null
    private var lastUsageEventTime: Long = 0L

    private val sessionExtensions: MutableMap<String, Int> = mutableMapOf()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
        if (startForegroundWithNotification()) {
            startWatchLoop()
        } else {
            stopSelf()
        }
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
                val foregroundPkg = getForegroundApp(usageStatsManager, now)
                if (foregroundPkg == null) {
                    flushCurrentSession(now)
                    continue
                }

                val today = dateFormat.format(Date(now))

                if (foregroundPkg == packageName || foregroundPkg == "com.android.systemui") {
                    flushCurrentSession(now)
                    continue
                }

                var limit = db.appLimitDao().getLimit(foregroundPkg)
                if (limit == null) {
                    flushCurrentSession(now)
                    alertShownForPkg = null
                    continue
                }

                if (limit.lastResetDate != today) {
                    if (currentForegroundPkg == foregroundPkg) {
                        flushCurrentSession(now)
                    }
                    db.appLimitDao().resetDailyUsage(foregroundPkg, today)
                    sessionExtensions.remove(foregroundPkg)
                    alertShownForPkg = null
                    limit = db.appLimitDao().getLimit(foregroundPkg) ?: continue
                }

                if (currentForegroundPkg != foregroundPkg) {
                    flushCurrentSession(now)
                    currentForegroundPkg = foregroundPkg
                    sessionStartTime = now
                    alertShownForPkg = null
                    lastAlertedPkg = null
                    Log.d(TAG, "Now watching: $foregroundPkg")
                }

                db.appLimitDao().addUsedSeconds(foregroundPkg, 1)

                val updated = db.appLimitDao().getLimit(foregroundPkg) ?: continue
                val remaining = updated.dailyLimitSeconds +
                        updated.extraSecondsEarned -
                        updated.usedSecondsToday

                if (remaining <= 0 && alertShownForPkg != foregroundPkg) {
                    alertShownForPkg = foregroundPkg
                    showResistanceAlert(foregroundPkg, updated)
                }
            }
        }
    }

    /**
     * When a tracked app leaves foreground, save the session to history.
     */
    private suspend fun flushCurrentSession(now: Long) {
        val pkg = currentForegroundPkg ?: return
        val startedAt = sessionStartTime

        currentForegroundPkg = null
        sessionStartTime = 0L

        if (startedAt == 0L) {
            sessionExtensions.remove(pkg)
            return
        }

        val duration = (now - startedAt) / 1000
        val extensions = sessionExtensions.remove(pkg) ?: 0
        if (duration < MIN_SESSION_SECONDS) return

        val limit = db.appLimitDao().getLimit(pkg)
        db.usageSessionDao().insert(
            UsageSession(
                appPackageName = pkg,
                appLabel = limit?.appLabel ?: pkg,
                startTimeMillis = startedAt,
                durationSeconds = duration,
                initialTimerSeconds = limit?.dailyLimitSeconds ?: 0,
                extensions = extensions
            )
        )
    }

    // -------------------------------------------------------------------------
    // Overlay alert — slides over the running app without killing it
    // -------------------------------------------------------------------------

    private fun showResistanceAlert(packageName: String, limit: AppLimit) {
        lastAlertedPkg = packageName
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
            if (pkg == currentForegroundPkg || pkg == lastAlertedPkg) {
                sessionExtensions[pkg] = (sessionExtensions[pkg] ?: 0) + 1
            }
            alertShownForPkg = null
            lastAlertedPkg = null
        }
    }

    private fun handleDone(pkg: String) {
        scope.launch {
            if (currentForegroundPkg == pkg) {
                flushCurrentSession(System.currentTimeMillis())
            }
            sessionExtensions.remove(pkg)
            alertShownForPkg = null
            lastAlertedPkg = null
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getForegroundApp(manager: UsageStatsManager, now: Long): String? {
        val queryStart = if (lastUsageEventTime == 0L) {
            now - INITIAL_EVENT_LOOKBACK_MS
        } else {
            lastUsageEventTime
        }

        val usageEvents = manager.queryEvents(queryStart, now)
        val event = UsageEvents.Event()
        var foregroundPkg = lastKnownForegroundPkg
        var newestEventTime = lastUsageEventTime

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val eventPackage = event.packageName ?: continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> foregroundPkg = eventPackage

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (foregroundPkg == eventPackage) {
                        foregroundPkg = null
                    }
                }
            }

            if (event.timeStamp > newestEventTime) {
                newestEventTime = event.timeStamp
            }
        }

        if (newestEventTime > 0L) {
            lastUsageEventTime = newestEventTime
        } else {
            lastUsageEventTime = now
            foregroundPkg = getRecentUsageFallback(manager, now)
        }

        lastKnownForegroundPkg = foregroundPkg
        return foregroundPkg
    }

    private fun getRecentUsageFallback(manager: UsageStatsManager, now: Long): String? {
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - RECENT_USAGE_FALLBACK_MS,
            now
        )

        return stats
            ?.maxByOrNull { it.lastTimeUsed }
            ?.takeIf { now - it.lastTimeUsed <= RECENT_USAGE_FALLBACK_MS }
            ?.packageName
    }

    private fun startForegroundWithNotification(): Boolean {
        val notification = NotificationCompat.Builder(this, ResistanceApp.TIMER_CHANNEL_ID)
            .setContentTitle("Resistance Timer Active")
            .setContentText("Watching your app usage in the background")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        return try {
            startForeground(NOTIFICATION_ID, notification)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to start foreground notification", e)
            false
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AppWatcherService"
        private const val NOTIFICATION_ID = 1001
        private const val INITIAL_EVENT_LOOKBACK_MS = 10_000L
        private const val RECENT_USAGE_FALLBACK_MS = 15_000L
        private const val MIN_SESSION_SECONDS = 2L

        const val ACTION_EXTEND = "com.resistancetimer.EXTEND"
        const val ACTION_DONE = "com.resistancetimer.DONE"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_SECONDS = "extra_seconds"
    }
}
