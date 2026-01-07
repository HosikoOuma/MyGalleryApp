package com.example.nkdsify.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class EditorTab {
    DRAW, TRANSFORM, FILTERS
}

enum class CropHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

enum class PhotoFilter {
    NONE, B_AND_W, SEPIA, INVERT, VINTAGE, COOL, WARM
}
