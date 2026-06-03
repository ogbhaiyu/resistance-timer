@file:OptIn(ExperimentalMaterial3Api::class)

package com.resistancetimer.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resistancetimer.data.AppLimit
import com.resistancetimer.data.TrackedApp
import com.resistancetimer.ui.MainViewModel
import com.resistancetimer.ui.theme.EmeraldGreen
import com.resistancetimer.ui.theme.ObsidianBg
import kotlin.math.roundToInt

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
                title = {
                    Text(
                        text = "Resistance Timer",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToStats,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Stats",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg
                )
            )
        },
        containerColor = ObsidianBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    placeholder = {
                        Text(
                            "Search apps",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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

    val target = showLimitDialog
    if (target != null) {
        val currentLimit = limitsByPackage[target.packageName]
        val app = installedApps.find { it.packageName == target.packageName }
        SetLimitDialog(
            appLabel = target.label,
            appIcon = app?.icon,
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (appLimits.isEmpty()) "No Limits Set Yet" else "Today at a Glance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            if (appLimits.isEmpty()) {
                Text(
                    text = "Your daily watchlist is empty. Set limits on distracting apps below to begin fighting Resistance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 22.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Circular Dial Progress Chart
                    CircularDialProgress(
                        progress = progress,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    // Summary statistics
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
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

                        // Status message
                        val statusText = when {
                            progress >= 1f -> "Resistance wins. Lockout active."
                            progress >= 0.8f -> "Danger zone. Close the apps."
                            progress >= 0.5f -> "Halfway gone. Keep focused."
                            else -> "Doing great. Stay mindful!"
                        }
                        val statusColor = when {
                            progress >= 1f -> MaterialTheme.colorScheme.error
                            progress >= 0.8f -> MaterialTheme.colorScheme.secondary
                            else -> EmeraldGreen
                        }

                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularDialProgress(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val progressColor = if (progress >= 1f) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val trackColor = Color.White.copy(alpha = 0.08f)

    Box(
        modifier = modifier.size(108.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val innerSize = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - innerSize) / 2,
                (size.height - innerSize) / 2
            )
            val innerBoxSize = Size(innerSize, innerSize)

            // Background Track Arc (270 degrees clockwise starting at -225)
            drawArc(
                color = trackColor,
                startAngle = -225f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = innerBoxSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = progressColor,
                startAngle = -225f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = innerBoxSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(progress * 100).roundToInt()}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "of limit",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp,
        color = Color.White.copy(alpha = 0.4f),
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    icon = icon,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDuration(limit.usedSecondsToday)} used • ${formatDuration(remaining)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (limit.extraSecondsEarned > 0) {
                        Text(
                            text = "+${formatDuration(limit.extraSecondsEarned)} added",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit limit",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove limit",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val progressAnimated by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(500),
                label = "rowProgress"
            )

            LinearProgressIndicator(
                progress = progressAnimated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) {
                    MaterialTheme.colorScheme.error
                } else if (progress >= 0.8f) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

@Composable
private fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon == null) {
        Box(
            modifier = modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        return
    }

    val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier.size(44.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (limit != null) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
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
            AppIcon(
                icon = app.icon,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (limit != null) {
                    Text(
                        text = "${formatDuration(limit.usedSecondsToday)} used today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = limit?.let { "${it.dailyLimitSeconds / 60}m/day" } ?: "Set Limit",
                fontSize = 13.sp,
                color = if (limit != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.5f)
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (limit != null) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            Color.White.copy(alpha = 0.04f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun EmptySearchState(searchQuery: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "No apps found for \"$searchQuery\".",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
    }
}

@Composable
fun SetLimitDialog(
    appLabel: String,
    appIcon: Drawable?,
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(currentMinutes.coerceIn(1, 120)) }
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF161622),
                            Color(0xFF0C0C12)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with App Icon and glowing background ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Faint radial glowing background under the icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    AppIcon(
                        icon = appIcon,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Daily limit for $appLabel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Set your daily scrolling allowance",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Presets Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        "QUICK PRESETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                }
                Spacer(Modifier.height(10.dp))

                // Preset Grid with sleek card buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(presets.take(4), presets.drop(4)).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { mins ->
                                val isSelected = selectedMinutes == mins
                                val presetScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.05f else 1f,
                                    animationSpec = tween(150),
                                    label = "presetScale"
                                )
                                val presetGlowColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f),
                                    animationSpec = tween(200),
                                    label = "presetGlow"
                                )
                                val presetBorderColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                                    animationSpec = tween(200),
                                    label = "presetBorder"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .graphicsLayer {
                                            scaleX = presetScale
                                            scaleY = presetScale
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(presetGlowColor)
                                        .border(
                                            width = 1.dp,
                                            color = presetBorderColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedMinutes = mins
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Custom Allowance Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Custom allowance",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (selectedMinutes == 60) "1 hour" else if (selectedMinutes == 120) "2 hours" else "$selectedMinutes minutes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                
                // Slider
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.toInt().coerceIn(1, 120) },
                    valueRange = 1f..120f,
                    steps = 118,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.6f),
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Save Limit (Vibrant Primary Gradient Button)
                    Button(
                        onClick = { onConfirm(selectedMinutes) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues()
                    ) {
                        Text(
                            "Save Limit",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

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

private data class LimitDialogTarget(
    val packageName: String,
    val label: String
)
