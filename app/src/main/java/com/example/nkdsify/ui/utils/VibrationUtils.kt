package com.example.nkdsify.ui.utils

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class VibrationStrength {
    NONE,
    LIGHT,
    MEDIUM,
    STRONG
}

fun performVibration(hapticFeedback: HapticFeedback, strength: VibrationStrength) {
    if (strength == VibrationStrength.NONE) return

    val hapticFeedbackType = when (strength) {
        VibrationStrength.LIGHT -> HapticFeedbackType.TextHandleMove
        VibrationStrength.MEDIUM -> HapticFeedbackType.LongPress
        VibrationStrength.STRONG -> HapticFeedbackType.LongPress
        else -> HapticFeedbackType.TextHandleMove // Default for other cases
    }
    hapticFeedback.performHapticFeedback(hapticFeedbackType)
}
