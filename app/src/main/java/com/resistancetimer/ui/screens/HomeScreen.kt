@file:OptIn(ExperimentalMaterial3Api::class)

package com.resistancetimer.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resistancetimer.data.AppLimit
import com.resistancetimer.data.TrackedApp
import com.resistancetimer.ui.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToStats: () -> Unit
) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val appLimits by viewModel.appLimits.collectAsStateWithLifecycle()

    val limitsByPackage = remember(appLimits) { appLimits.associateBy { it.packageName } }
    val trackedPackageNames = remember(appLimits) { appLimits.map { it.packageName }.toSet() }

    var searchQuery by remember { mutableStateOf("") }
    var showLimitDialog by remember { mutableStateOf<LimitDialogTarget?>(null) }

    val filteredApps = remember(installedApps, searchQuery) {
        installedApps.filter {
            searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true)
        }
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "summary") {
                DailyLimitSummary(appLimits = appLimits)
            }

            if (appLimits.isNotEmpty()) {
                item(key = "limits-title") {
                    SectionLabel("Today's limits")
                }

                items(
                    items = appLimits,
                    key = { "limit-${it.packageName}" }
                ) { limit ->
                    val app = installedApps.find { it.packageName == limit.packageName }
                    ActiveLimitRow(
                        limit = limit,
                        icon = app?.icon,
                        onEdit = {
                            showLimitDialog = LimitDialogTarget(
                                packageName = limit.packageName,
                                label = limit.appLabel
                            )
                        },
                        onRemove = { viewModel.removeAppLimit(limit.packageName) }
                    )
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "apps-title") {
                val label = if (trackedPackageNames.isEmpty()) {
                    "Available apps"
                } else {
                    "All apps"
                }
                SectionLabel(label)
            }

            if (filteredApps.isEmpty()) {
                item(key = "empty-search") {
                    EmptySearchState(searchQuery = searchQuery)
                }
            } else {
                items(
                    items = filteredApps,
                    key = { "app-${it.packageName}" }
                ) { app ->
                    val limit = limitsByPackage[app.packageName]
                    AppListRow(
                        app = app,
                        limit = limit,
                        onClick = {
                            showLimitDialog = LimitDialogTarget(
                                packageName = app.packageName,
                                label = app.label
                            )
                        }
                    )
                }
            }
        }
    }

    showLimitDialog?.let { target ->
        val currentLimit = limitsByPackage[target.packageName]
        SetLimitDialog(
            appLabel = target.label,
            currentMinutes = currentLimit?.let { it.dailyLimitSeconds / 60 } ?: 20,
            onConfirm = { minutes ->
                viewModel.setAppLimit(target.packageName, target.label, minutes)
                showLimitDialog = null
            },
            onDismiss = { showLimitDialog = null }
        )
    }
}

@Composable
private fun DailyLimitSummary(appLimits: List<AppLimit>) {
    val totalUsed = appLimits.sumOf { it.usedSecondsToday }
    val totalAllowance = appLimits.sumOf { it.dailyLimitSeconds + it.extraSecondsEarned }
    val progress = if (totalAllowance > 0) {
        (totalUsed.toFloat() / totalAllowance).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remaining = (totalAllowance - totalUsed).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (appLimits.isEmpty()) "No limits set yet" else "Today at a glance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(8.dp))

            if (appLimits.isEmpty()) {
                Text(
                    text = "Your daily watchlist is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    SummaryMetric(
                        label = "Used",
                        value = formatDuration(totalUsed)
                    )
                    SummaryMetric(
                        label = "Left",
                        value = formatDuration(remaining)
                    )
                    SummaryMetric(
                        label = "Apps",
                        value = appLimits.size.toString()
                    )
                }

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (progress >= 1f) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ActiveLimitRow(
    limit: AppLimit,
    icon: Drawable?,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val totalAllowance = limit.dailyLimitSeconds + limit.extraSecondsEarned
    val progress = if (totalAllowance > 0) {
        (limit.usedSecondsToday.toFloat() / totalAllowance).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remaining = (totalAllowance - limit.usedSecondsToday).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(icon = icon)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appLabel,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDuration(limit.usedSecondsToday)} used, ${formatDuration(remaining)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (limit.extraSecondsEarned > 0) {
                        Text(
                            text = "${formatDuration(limit.extraSecondsEarned)} extra added today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit limit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove limit",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (progress >= 1f) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon == null) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
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

@Composable
private fun AppListRow(
    app: TrackedApp,
    limit: AppLimit?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (limit != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon = app.icon)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (limit != null) {
                    Text(
                        text = "${formatDuration(limit.usedSecondsToday)} used today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = limit?.let { "${it.dailyLimitSeconds / 60}m/day" } ?: "Set",
                style = MaterialTheme.typography.labelLarge,
                color = if (limit != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptySearchState(searchQuery: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "No apps found for \"$searchQuery\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        )
    }
}

@Composable
fun SetLimitDialog(
    appLabel: String,
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(currentMinutes.coerceIn(1, 120)) }
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily limit for $appLabel",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text(
                    "Daily allowance",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    "Quick pick",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))

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

                Text(
                    "$selectedMinutes minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.toInt().coerceIn(1, 120) },
                    valueRange = 1f..120f,
                    steps = 118
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMinutes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private data class LimitDialogTarget(
    val packageName: String,
    val label: String
)

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
