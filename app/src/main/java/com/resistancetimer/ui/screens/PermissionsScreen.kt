package com.resistancetimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resistancetimer.ui.theme.EmeraldGreen
import com.resistancetimer.ui.theme.ObsidianBg
import com.resistancetimer.ui.theme.ResistanceRed

/**
 * Shown on first launch to walk the user through required permissions:
 * 1. Usage Access — so we can detect which app is in foreground
 * 2. Draw Over Apps — so the alert overlay works on top of Instagram etc.
 * 3. Notifications — optional but recommended
 */
@Composable
fun PermissionsScreen(
    hasUsageAccess: Boolean,
    hasOverlay: Boolean,
    hasNotification: Boolean,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High fidelity visual shield / crest
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(ResistanceRed.copy(alpha = 0.1f))
                .border(2.dp, ResistanceRed.copy(alpha = 0.2f), CircleShape)
        ) {
            Text("⚔️", fontSize = 44.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Setup Resistance",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Resistance Timer needs a few local permissions to watch your app limits and block doomscrolling. All processing happens entirely offline.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.6f),
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Checklist of items
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PermissionItem(
                emoji = "👁",
                title = "Usage Access",
                description = "Required to count your scrolling hours in background.",
                granted = hasUsageAccess,
                onRequest = onRequestUsage
            )

            PermissionItem(
                emoji = "💬",
                title = "Display Over Other Apps",
                description = "Required to slide the alert overlay over distracting apps.",
                granted = hasOverlay,
                onRequest = onRequestOverlay
            )

            PermissionItem(
                emoji = "🔔",
                title = "Notifications",
                description = "Optional backing system to keep service running reliably.",
                granted = hasNotification,
                onRequest = onRequestNotification,
                required = false
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "\"The professional does not wait for inspiration to strike. He acts in the face of fear.\" — The War of Art",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.35f),
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun PermissionItem(
    emoji: String,
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
    required: Boolean = true
) {
    val cardBorderColor = if (granted) {
        EmeraldGreen.copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                EmeraldGreen.copy(alpha = 0.03f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Emoji Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) {
                            EmeraldGreen.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.04f)
                        }
                    )
            ) {
                Text(emoji, fontSize = 20.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    if (!required) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "optional",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            if (granted) {
                // Glow Checkmark Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen)
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            } else {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (required) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        }
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Allow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
