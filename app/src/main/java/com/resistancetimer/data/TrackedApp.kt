package com.resistancetimer.data

import android.graphics.drawable.Drawable

/**
 * Represents an app the user can choose to track.
 */
data class TrackedApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSelected: Boolean = false
)
