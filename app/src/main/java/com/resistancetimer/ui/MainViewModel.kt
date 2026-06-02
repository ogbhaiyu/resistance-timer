package com.resistancetimer.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resistancetimer.ResistanceApp
import com.resistancetimer.data.AppLimit
import com.resistancetimer.data.TrackedApp
import com.resistancetimer.data.UsageSession
import com.resistancetimer.service.AppWatcherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ResistanceApp).database
    private val limitDao = db.appLimitDao()
    private val sessionDao = db.usageSessionDao()

    // All installed launchable apps
    private val _installedApps = MutableStateFlow<List<TrackedApp>>(emptyList())
    val installedApps: StateFlow<List<TrackedApp>> = _installedApps

    // Apps the user has set limits for
    val appLimits: StateFlow<List<AppLimit>> = limitDao.getAllLimits()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Sessions this week for stats
    val sessionsThisWeek: StateFlow<List<UsageSession>> = run {
        val weekStart = startOfWeekMillis()
        sessionDao.getSessionsSince(weekStart)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    val totalSecondsThisWeek: StateFlow<Long> = run {
        val weekStart = startOfWeekMillis()
        sessionDao.getTotalSecondsSince(weekStart)
            .map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    }

    init {
        resetStaleLimits()
        loadInstalledApps()
        ensureServiceRunning()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                    .map { info ->
                        TrackedApp(
                            packageName = info.activityInfo.packageName,
                            label = info.loadLabel(pm).toString(),
                            icon = info.loadIcon(pm)
                        )
                    }
                    .distinctBy { it.packageName }
                    .filter { it.packageName != getApplication<Application>().packageName }
                    .sortedBy { it.label.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    /**
     * Set or update a daily time limit for an app.
     * limitMinutes = 0 means "remove the limit".
     */
    fun setAppLimit(packageName: String, appLabel: String, limitMinutes: Int) {
        viewModelScope.launch {
            if (limitMinutes <= 0) {
                val existing = limitDao.getLimit(packageName) ?: return@launch
                limitDao.delete(existing)
            } else {
                val today = todayKey()
                val existing = limitDao.getLimit(packageName)
                limitDao.upsert(
                    AppLimit(
                        packageName = packageName,
                        appLabel = appLabel,
                        dailyLimitSeconds = limitMinutes * 60,
                        usedSecondsToday = existing?.usedSecondsToday ?: 0,
                        lastResetDate = existing?.lastResetDate ?: today
                    )
                )
            }
        }
    }

    fun removeAppLimit(packageName: String) {
        viewModelScope.launch {
            val existing = limitDao.getLimit(packageName) ?: return@launch
            limitDao.delete(existing)
        }
    }

    suspend fun getImprovementPercent(): Int {
        val now = System.currentTimeMillis()
        val thisWeekStart = startOfWeekMillis()
        val lastWeekStart = thisWeekStart - 7 * 24 * 60 * 60 * 1000L

        val thisWeek = sessionDao.getTotalSecondsBetween(thisWeekStart, now) ?: 0L
        val lastWeek = sessionDao.getTotalSecondsBetween(lastWeekStart, thisWeekStart) ?: 0L
        if (lastWeek == 0L) return 0
        return ((lastWeek - thisWeek).toDouble() / lastWeek * 100).toInt()
    }

    private fun ensureServiceRunning() {
        val context = getApplication<Application>()
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AppWatcherService::class.java)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to start watcher service", e)
        }
    }

    private fun resetStaleLimits() {
        viewModelScope.launch {
            limitDao.resetStaleDailyUsage(todayKey())
        }
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun startOfWeekMillis(): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
