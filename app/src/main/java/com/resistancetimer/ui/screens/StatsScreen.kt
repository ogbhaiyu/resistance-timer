package com.resistancetimer.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.resistancetimer.data.UsageSession
import com.resistancetimer.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessionsThisWeek.collectAsState()
    val totalSeconds by viewModel.totalSecondsThisWeek.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var improvementPercent by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        improvementPercent = viewModel.getImprovementPercent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            shareStats(context, totalSeconds, improvementPercent, sessions.size)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary card
            item {
                ShareableStatsCard(
                    totalSeconds = totalSeconds,
                    improvementPercent = improvementPercent,
                    sessionCount = sessions.size
                )
            }

            // Per-app breakdown
            item {
                Text(
                    "This Week's Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Text(
                        "No sessions yet. Start a timer to track your scrolling!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            items(sessions) { session ->
                SessionRow(session)
            }
        }
    }
}

@Composable
fun ShareableStatsCard(
    totalSeconds: Long,
    improvementPercent: Int,
    sessionCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚔️ RESISTANCE REPORT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(16.dp))

            // Total time
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            Text(
                text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "total scrolling this week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            // Improvement
            if (improvementPercent != 0) {
                val isImproved = improvementPercent > 0
                Text(
                    text = if (isImproved)
                        "📉 ${improvementPercent}% LESS than last week"
                    else
                        "📈 ${-improvementPercent}% MORE than last week",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isImproved) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            } else {
                Text(
                    text = "First week tracking — keep going!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "$sessionCount sessions tracked",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "\"Put. The. Phone. Down.\" — Pressfield (probably)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SessionRow(session: UsageSession) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
    val minutes = session.durationSeconds / 60
    val seconds = session.durationSeconds % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.appLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(session.startTimeMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${minutes}m ${seconds}s",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (session.extensions > 0) {
                    Text(
                        text = "+${session.extensions} extends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun shareStats(context: Context, totalSeconds: Long, improvementPercent: Int, sessionCount: Int) {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    val timeText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    val improvementText = when {
        improvementPercent > 0 -> "📉 ${improvementPercent}% less doomscrolling than last week!"
        improvementPercent < 0 -> "Working on it... ${-improvementPercent}% more than last week"
        else -> "Just started tracking!"
    }

    val shareText = """
⚔️ RESISTANCE REPORT ⚔️

This week I spent $timeText scrolling across $sessionCount sessions.
$improvementText

"The more Resistance you experience, the more important your unfinished work is to you."

Fighting Resistance with @ResistanceTimer 💪
#ResistanceTimer #WarOfArt #Pressfield
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share your Resistance Report"))
}
