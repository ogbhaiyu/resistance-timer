package com.resistancetimer.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Visual Theme Color Palette
val ObsidianBg = Color(0xFF08080C)
val RichCardBg = Color(0xFF14141E)
val DarkCardBgVariant = Color(0xFF1D1D2B)
val PremiumBorder = Color(0xFF2A2A3A)

val ResistanceRed = Color(0xFFFF3B30)
val ResistanceOrange = Color(0xFFFF7F30)
val NeonOrange = Color(0xFFFF9F0A)
val GlowYellow = Color(0xFFFFCC00)
val EmeraldGreen = Color(0xFF34C759)
val DeepGreen = Color(0xFF1E5E2F)

private val DarkColorScheme = darkColorScheme(
    primary = ResistanceRed,
    secondary = NeonOrange,
    tertiary = GlowYellow,
    background = ObsidianBg,
    surface = RichCardBg,
    surfaceVariant = DarkCardBgVariant,
    outline = PremiumBorder,
    outlineVariant = PremiumBorder.copy(alpha = 0.5f),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.85f),
    error = ResistanceRed,
    errorContainer = Color(0xFF5A0000)
)

@Composable
fun ResistanceTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
