package com.example.nkdsify.ui.components.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent

internal fun PointerEvent.calculateCentroid(useCurrentPosition: Boolean = false): Offset {
    var sum = Offset.Zero
    var count = 0
    changes.forEach {
        if (it.pressed) {
            sum += if (useCurrentPosition) it.position else it.previousPosition
            count++
        }
    }
    return if (count == 0) Offset.Zero else sum / count.toFloat()
}
