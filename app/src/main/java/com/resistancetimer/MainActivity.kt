package com.resistancetimer

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.resistancetimer.ui.MainViewModel
import com.resistancetimer.ui.screens.HomeScreen
import com.resistancetimer.ui.screens.PermissionsScreen
import com.resistancetimer.ui.screens.StatsScreen
import com.resistancetimer.ui.theme.ResistanceTimerTheme

class MainActivity : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { recompose() }

    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { recompose() }

    private val usagePermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { recompose() }

    private var recomposeTrigger = mutableStateOf(0)

    private fun recompose() { recomposeTrigger.value++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val trigger by recomposeTrigger
            ResistanceTimerTheme {
                val allGranted = remember(trigger) { checkAllPermissions() }
                if (allGranted) {
                    ResistanceTimerApp()
                } else {
                    PermissionsScreen(
                        hasUsageAccess = remember(trigger) { hasUsageStatsPermission() },
                        hasOverlay = remember(trigger) { Settings.canDrawOverlays(this) },
                        hasNotification = remember(trigger) { hasNotificationPermission() },
                        onRequestUsage = {
                            usagePermLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        onRequestOverlay = {
                            overlayPermLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recompose()
    }

    private fun checkAllPermissions(): Boolean =
        hasUsageStatsPermission() && Settings.canDrawOverlays(this)

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}

@Composable
fun ResistanceTimerApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToStats = { navController.navigate("stats") }
            )
        }
        composable("stats") {
            StatsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
