package com.resistancetimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shown on first launch to walk the user through the two required permissions:
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚔️", fontSize = 56.sp)

        Spacer(Modifier.height(16.dp))

        Text(
            "Before We Start",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Resistance Timer needs two things to work properly. No data leaves your phone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        PermissionItem(
            emoji = "👁",
            title = "Usage Access",
            description = "So we can detect when you open Instagram, TikTok, etc. and start counting your time.",
            granted = hasUsageAccess,
            onRequest = onRequestUsage
        )

        Spacer(Modifier.height(16.dp))

        PermissionItem(
            emoji = "💬",
            title = "Display Over Other Apps",
            description = "So the Resistance alert slides over the app without closing it. Your scroll position stays.",
            granted = hasOverlay,
            onRequest = onRequestOverlay
        )

        Spacer(Modifier.height(16.dp))

        PermissionItem(
            emoji = "🔔",
            title = "Notifications",
            description = "Optional — backup alerts if the overlay doesn't trigger.",
            granted = hasNotification,
            onRequest = onRequestNotification,
            required = false
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "\"The professional does not wait for inspiration to strike. He acts in the face of fear.\" — Pressfield",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (!required) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "optional",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Black, fontSize = 18.sp)
            } else {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Allow", fontSize = 12.sp)
                }
            }
        }
    }
}
