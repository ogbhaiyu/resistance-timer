package com.resistancetimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resistancetimer.service.AppWatcherService
import com.resistancetimer.ui.theme.ResistanceTimerTheme
import kotlin.math.roundToInt

class ResistanceAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appPackage  = intent.getStringExtra(EXTRA_PACKAGE)      ?: ""
        val appLabel    = intent.getStringExtra(EXTRA_APP_LABEL)     ?: "this app"
        val limitSeconds = intent.getIntExtra(EXTRA_LIMIT_SECONDS, 0)

        setContent {
            ResistanceTimerTheme {
                ResistanceAlertOverlay(
                    appLabel    = appLabel,
                    limitMinutes = limitSeconds / 60,
                    onDone = {
                        notifyService(AppWatcherService.ACTION_DONE, appPackage)
                        startActivity(
                            Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                        finish()
                    },
                    onExtend = { extraMinutes ->
                        notifyService(AppWatcherService.ACTION_EXTEND, appPackage, extraMinutes * 60)
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }

    private fun notifyService(action: String, pkg: String, seconds: Int = 0) {
        startService(Intent(this, AppWatcherService::class.java).apply {
            this.action = action
            putExtra(AppWatcherService.EXTRA_PACKAGE, pkg)
            if (seconds > 0) putExtra(AppWatcherService.EXTRA_SECONDS, seconds)
        })
    }

    companion object {
        const val EXTRA_PACKAGE       = "extra_package"
        const val EXTRA_APP_LABEL     = "extra_app_label"
        const val EXTRA_LIMIT_SECONDS = "extra_limit_seconds"
    }
}

// ---------------------------------------------------------------------------
// Resistance intensity model
// ---------------------------------------------------------------------------

/**
 * Bucketed intensity based on how much extra time the user is grabbing.
 * The more time → the deeper Resistance has them → darker UI + harsher quote.
 */
enum class ResistanceLevel(
    val minutes: Int,
    val label: String,
    val color: Color,
    val dimAlpha: Float,
    val quote: String,
    val quoteAttrib: String
) {
    MILD(
        minutes    = 5,
        label      = "5 minutes",
        color      = Color(0xFFFF7043),
        dimAlpha   = 0.60f,
        quote      = "\"Resistance is always lying and always full of shit.\"",
        quoteAttrib = "The War of Art"
    ),
    MODERATE(
        minutes    = 10,
        label      = "10 minutes",
        color      = Color(0xFFE53935),
        dimAlpha   = 0.72f,
        quote      = "\"The more Resistance you experience, the more important this unfinished calling is to your soul.\"",
        quoteAttrib = "The War of Art"
    ),
    HIGH(
        minutes    = 20,
        label      = "20 minutes",
        color      = Color(0xFFB71C1C),
        dimAlpha   = 0.82f,
        quote      = "\"Most of us have two lives: the life we live, and the unlived life within us. Between the two stands Resistance.\"",
        quoteAttrib = "The War of Art"
    ),
    DEEP(
        minutes    = 30,
        label      = "30 minutes",
        color      = Color(0xFF7B0000),
        dimAlpha   = 0.90f,
        quote      = "\"Resistance has beaten you today. It knows exactly which buttons to push. It's been doing this your whole life.\"",
        quoteAttrib = "Paraphrasing Pressfield"
    ),
    SURRENDERED(
        minutes    = 60,
        label      = "1 hour",
        color      = Color(0xFF3E0000),
        dimAlpha   = 0.95f,
        quote      = "\"Are you a writer who doesn't write, a painter who doesn't paint, an entrepreneur who never starts a venture? Then you know what Resistance is.\"",
        quoteAttrib = "The War of Art"
    );

    companion object {
        /** Snap to the nearest level given a raw minute value */
        fun fromMinutes(mins: Int): ResistanceLevel = when {
            mins <= 5  -> MILD
            mins <= 10 -> MODERATE
            mins <= 20 -> HIGH
            mins <= 30 -> DEEP
            else       -> SURRENDERED
        }
    }
}

// ---------------------------------------------------------------------------
// Drum-roll / slot-machine style minute picker
// ---------------------------------------------------------------------------

/**
 * A vertical scroll drum that lets the user swipe up/down to pick minutes.
 * The selected value is shown large in the centre; values above and below
 * are visible but faded — like a real clock picker wheel.
 */
@Composable
fun MinuteDrum(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val options = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
    val currentIndex = remember(minutes) {
        options.indexOfFirst { it >= minutes }.takeIf { it >= 0 } ?: (options.size - 1)
    }
    val itemHeight = 72.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .height(220.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd   = { dragAccumulator = 0f },
                    onDragCancel = { dragAccumulator = 0f }
                ) { _, delta ->
                    dragAccumulator -= delta          // swipe up = more time
                    val steps = (dragAccumulator / itemHeightPx).roundToInt()
                    if (steps != 0) {
                        val newIndex = (currentIndex + steps).coerceIn(0, options.lastIndex)
                        if (newIndex != currentIndex) {
                            onMinutesChange(options[newIndex])
                            dragAccumulator = 0f
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Visible window: one item above + current + one below (+ extras faded)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            (-2..2).forEach { offset ->
                val idx = currentIndex + offset
                val value = options.getOrNull(idx)
                val isCurrent = offset == 0
                val alpha = when (kotlin.math.abs(offset)) {
                    0 -> 1f
                    1 -> 0.45f
                    else -> 0.15f
                }
                val scale = if (isCurrent) 1f else 0.75f

                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (value != null) {
                        val label = if (value == 60) "1 hr" else "${value}m"
                        Text(
                            text  = label,
                            fontSize = if (isCurrent) 42.sp else 26.sp,
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                            color = if (isCurrent) accentColor else Color.White.copy(alpha = alpha),
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
        }

        // Selection highlight lines
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.55f)
                .height(2.dp)
                .offset(y = (-36).dp)
                .background(accentColor.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.55f)
                .height(2.dp)
                .offset(y = 36.dp)
                .background(accentColor.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
        )

        // Fade top + bottom edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val fadeH = size.height * 0.28f
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0xFF1A1A1A),
                            1f to Color.Transparent,
                            startY = 0f,
                            endY = fadeH
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color(0xFF1A1A1A),
                            startY = size.height - fadeH,
                            endY = size.height
                        )
                    )
                }
        )
    }
}

// ---------------------------------------------------------------------------
// Main overlay composable
// ---------------------------------------------------------------------------

@Composable
fun ResistanceAlertOverlay(
    appLabel: String,
    limitMinutes: Int,
    onDone: () -> Unit,
    onExtend: (minutes: Int) -> Unit
) {
    var showExtend       by remember { mutableStateOf(false) }
    var pickedMinutes    by remember { mutableIntStateOf(5) }

    val level by remember(pickedMinutes) {
        derivedStateOf { ResistanceLevel.fromMinutes(pickedMinutes) }
    }

    // Animate the dim and accent colour as resistance intensity changes
    val animatedDim by animateFloatAsState(
        targetValue  = if (showExtend) level.dimAlpha else 0.75f,
        animationSpec = tween(500),
        label        = "dim"
    )
    val animatedColor by animateColorAsState(
        targetValue  = if (showExtend) level.color else Color(0xFFE53935),
        animationSpec = tween(500),
        label        = "accentColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = animatedDim)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle pill
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )

            Spacer(Modifier.height(18.dp))

            // Header — changes when extend is open
            AnimatedContent(
                targetState = showExtend,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "header"
            ) { extending ->
                if (!extending) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 44.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "RESISTANCE HAS YOU",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            color = animatedColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "You've used your $limitMinutes min daily limit on $appLabel.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "\"Resistance will tell you anything to keep you from doing your work.\"",
                            fontSize = 13.sp,
                            color = Color(0xFFFF7043),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp
                        )
                        Text(
                            "— Steven Pressfield, The War of Art",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                } else {
                    // Extend mode — show intensity feedback
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (level) {
                                ResistanceLevel.MILD      -> "😬"
                                ResistanceLevel.MODERATE  -> "😔"
                                ResistanceLevel.HIGH      -> "😤"
                                ResistanceLevel.DEEP      -> "🔥"
                                ResistanceLevel.SURRENDERED -> "💀"
                            },
                            fontSize = 40.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (level) {
                                ResistanceLevel.MILD       -> "A little more... okay."
                                ResistanceLevel.MODERATE   -> "Resistance is tightening its grip."
                                ResistanceLevel.HIGH       -> "It's winning. You know that, right?"
                                ResistanceLevel.DEEP       -> "Resistance owns you right now."
                                ResistanceLevel.SURRENDERED -> "You've fully surrendered. For now."
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = animatedColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = level.quote,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Text(
                            "— ${level.quoteAttrib}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Time picker drum — only visible in extend mode
            AnimatedVisibility(
                visible = showExtend,
                enter   = expandVertically(tween(350)) + fadeIn(tween(300)),
                exit    = shrinkVertically(tween(300)) + fadeOut(tween(200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Swipe to choose",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    MinuteDrum(
                        minutes         = pickedMinutes,
                        onMinutesChange = { pickedMinutes = it },
                        accentColor     = animatedColor,
                        modifier        = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Primary: done
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape  = RoundedCornerShape(14.dp)
            ) {
                Text("I'm done. Resistance loses. 💪", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            // Secondary: extend / confirm extend
            AnimatedContent(
                targetState = showExtend,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "extendBtn"
            ) { extending ->
                if (!extending) {
                    OutlinedButton(
                        onClick  = { showExtend = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.55f)
                        )
                    ) {
                        Text("Give me more time… (Resistance wins this round)", fontSize = 13.sp)
                    }
                } else {
                    Column {
                        Button(
                            onClick  = { onExtend(pickedMinutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = animatedColor),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            val label = if (pickedMinutes == 60) "1 hour" else "$pickedMinutes minutes"
                            Text(
                                "Give me $label more",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick  = { showExtend = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "← Actually, I'm done",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
