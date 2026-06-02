@file:OptIn(ExperimentalMaterial3Api::class)

package com.resistancetimer.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resistancetimer.data.UsageSession
import com.resistancetimer.ui.MainViewModel
import com.resistancetimer.ui.theme.EmeraldGreen
import com.resistancetimer.ui.theme.ObsidianBg
import com.resistancetimer.ui.theme.ResistanceRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessionsThisWeek.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.totalSecondsThisWeek.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var improvementPercent by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        improvementPercent = viewModel.getImprovementPercent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Your Progress",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                shareStats(context, totalSeconds, improvementPercent, sessions.size)
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
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
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Summary card
            item {
                ShareableStatsCard(
                    totalSeconds = totalSeconds,
                    improvementPercent = improvementPercent,
                    sessionCount = sessions.size
                )
            }

            // Custom Canvas Bar Chart for weekly scrolling times
            item {
                WeeklyUsageChart(sessions = sessions)
            }

            // Per-app breakdown title
            item {
                Text(
                    text = "Session History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No sessions recorded this week. Your scrolling habits are under control! Keep it up.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                items(
                    items = sessions,
                    key = { "session-${it.startTimeMillis}" }
                ) { session ->
                    SessionRow(session)
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ResistanceRed, Color(0xFFFF5252))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⚔️ RESISTANCE REPORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Total Time Value
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            Text(
                text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text(
                text = "spent doomscrolling this week",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(16.dp))

            // Trend Indicator Badge
            if (improvementPercent != 0) {
                val isImproved = improvementPercent > 0
                val badgeColor = if (isImproved) EmeraldGreen else ResistanceRed
                val trendText = if (isImproved) {
                    "📉 $improvementPercent% LESS time scrolling"
                } else {
                    "📈 ${-improvementPercent}% MORE time scrolling"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.1f))
                        .border(1.dp, badgeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = trendText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            } else {
                Text(
                    text = "Starting block — building your weekly baseline",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "$sessionCount total sessions tracked",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WeeklyUsageChart(sessions: List<UsageSession>) {
    val dailyMinutes = remember(sessions) {
        val seconds = DoubleArray(7)
        val calendar = Calendar.getInstance()
        sessions.forEach { session ->
            calendar.timeInMillis = session.startTimeMillis
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val index = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            seconds[index] += session.durationSeconds.toDouble()
        }
        seconds.map { (it / 60.0) }
    }

    val maxMinutes = remember(dailyMinutes) {
        dailyMinutes.maxOrNull()?.coerceAtLeast(15.0) ?: 15.0
    }

    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))

            // Canvas drawing the bar chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barCount = 7
                    val spacing = 18.dp.toPx()
                    val totalWidth = size.width
                    val barWidth = (totalWidth - (spacing * (barCount - 1))) / barCount
                    val canvasHeight = size.height

                    for (i in 0 until barCount) {
                        val minutes = dailyMinutes[i]
                        val rawRatio = (minutes / maxMinutes).toFloat()
                        val animRatio = rawRatio.coerceIn(0f, 1f)

                        val x = i * (barWidth + spacing)
                        val barHeight = canvasHeight * animRatio

                        // Draw background track bar
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.04f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, canvasHeight),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )

                        // Draw active progress bar
                        if (barHeight > 0f) {
                            val activeBrush = Brush.verticalGradient(
                                colors = listOf(ResistanceRed, ResistanceRed.copy(alpha = 0.5f))
                            )
                            drawRoundRect(
                                brush = activeBrush,
                                topLeft = Offset(x, canvasHeight - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Day labels and values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 7) {
                    val mins = dailyMinutes[i].roundToInt()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (mins > 0) "${mins}m" else "-",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mins > 0) Color.White else Color.White.copy(alpha = 0.25f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dayLabels[i],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: UsageSession) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
    val minutes = session.durationSeconds / 60
    val seconds = session.durationSeconds % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeline Bullet indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.extensions > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            EmeraldGreen
                        }
                    )
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.appLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = dateFormat.format(Date(session.startTimeMillis)),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${minutes}m ${seconds}s",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                if (session.extensions > 0) {
                    Text(
                        text = "+${session.extensions} extends",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
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
#ResistanceTimer #WarOfArt
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share your Resistance Report"))
}
