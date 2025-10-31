package com.example.nkdsify.editor.utils

import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

fun StrokeCap.toPaintCap(): Paint.Cap {
    return when (this) {
        StrokeCap.Butt -> Paint.Cap.BUTT
        StrokeCap.Round -> Paint.Cap.ROUND
        StrokeCap.Square -> Paint.Cap.SQUARE
        else -> throw IllegalArgumentException("Invalid stroke cap")
    }
}

fun StrokeJoin.toPaintJoin(): Paint.Join {
    return when (this) {
        StrokeJoin.Miter -> Paint.Join.MITER
        StrokeJoin.Round -> Paint.Join.ROUND
        StrokeJoin.Bevel -> Paint.Join.BEVEL
        else -> throw IllegalArgumentException("Invalid stroke join")
    }
}

fun BlendMode.toPorterDuffMode(): PorterDuff.Mode {
    return when (this) {
        BlendMode.Clear -> PorterDuff.Mode.CLEAR
        BlendMode.SrcOver -> PorterDuff.Mode.SRC_OVER
        else -> PorterDuff.Mode.SRC_OVER
    }
}
