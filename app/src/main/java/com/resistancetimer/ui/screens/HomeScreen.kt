package com.resistancetimer.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.resistancetimer.data.AppLimit
import com.resistancetimer.data.TrackedApp
import com.resistancetimer.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToStats: () -> Unit
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val appLimits by viewModel.appLimits.collectAsState()

    val limitsByPackage = remember(appLimits) { appLimits.associateBy { it.packageName } }

    var searchQuery by remember { mutableStateOf("") }
    var showLimitDialog by remember { mutableStateOf<TrackedApp?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resistance Timer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header quote
            Text(
                text = "\"Resistance will tell you anything to keep you from doing your work.\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Active limits section
            if (appLimits.isNotEmpty()) {
                Text(
                    text = "YOUR LIMITS TODAY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                appLimits.forEach { limit ->
                    val app = installedApps.find { it.packageName == limit.packageName }
                    ActiveLimitRow(
                        limit = limit,
                        icon = app?.icon,
                        onEdit = {
                            if (app != null) showLimitDialog = app
                        },
                        onRemove = { viewModel.removeAppLimit(limit.packageName) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Search
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Text(
                text = "TAP AN APP TO SET A DAILY LIMIT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val filteredApps = remember(installedApps, searchQuery) {
                installedApps.filter {
                    searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val limit = limitsByPackage[app.packageName]
                    AppListRow(
                        app = app,
                        currentLimitMinutes = limit?.let { it.dailyLimitSeconds / 60 },
                        usedSeconds = limit?.usedSecondsToday ?: 0,
                        onClick = { showLimitDialog = app }
                    )
                }
            }
        }
    }

    // Limit-setting dialog
    showLimitDialog?.let { app ->
        val currentLimit = limitsByPackage[app.packageName]
        SetLimitDialog(
            appLabel = app.label,
            currentMinutes = currentLimit?.let { it.dailyLimitSeconds / 60 } ?: 20,
            onConfirm = { minutes ->
                viewModel.setAppLimit(app.packageName, app.label, minutes)
                showLimitDialog = null
            },
            onDismiss = { showLimitDialog = null }
        )
    }
}

@Composable
private fun ActiveLimitRow(
    limit: AppLimit,
    icon: Drawable?,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val limitMins = limit.dailyLimitSeconds / 60
    val usedMins = limit.usedSecondsToday / 60
    val progress = (limit.usedSecondsToday.toFloat() / limit.dailyLimitSeconds).coerceIn(0f, 1f)
    val remaining = ((limit.dailyLimitSeconds - limit.usedSecondsToday) / 60).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(icon = icon, modifier = Modifier.padding(end = 12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(limit.appLabel, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${usedMins}m used · ${remaining}m remaining · ${limitMins}m limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (progress >= 1f) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon == null) {
        Spacer(modifier = modifier.size(40.dp))
        return
    }
    val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListRow(
    app: TrackedApp,
    currentLimitMinutes: Int?,
    usedSeconds: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentLimitMinutes != null)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon = app.icon, modifier = Modifier.padding(end = 12.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (currentLimitMinutes != null) {
                Text(
                    text = "${currentLimitMinutes}m/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "No limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
fun SetLimitDialog(
    appLabel: String,
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(currentMinutes) }
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily limit for $appLabel") },
        text = {
            Column {
                Text(
                    "How many minutes per day?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Preset chips
                Text(
                    "Quick pick:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                // Two rows of preset chips
                listOf(presets.take(4), presets.drop(4)).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        row.forEach { mins ->
                            FilterChip(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                label = { Text("${mins}m", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Slider for fine control
                Text(
                    "Or drag: $selectedMinutes minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.toInt() },
                    valueRange = 1f..120f,
                    steps = 118
                )

                Text(
                    "When you hit this limit, Resistance will remind you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMinutes) }) {
                Text("Set Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
