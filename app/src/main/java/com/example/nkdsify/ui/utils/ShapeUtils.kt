package com.example.nkdsify.ui.utils

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon

class GenericPolygonShape(private val polygon: RoundedPolygon) : Shape {
    private val matrix = Matrix()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = polygon.toComposePath()
        matrix.reset()
        matrix.translate(size.width / 2f, size.height / 2f)
        matrix.scale(size.width / 2f, size.height / 2f)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

fun RoundedPolygon.toComposePath(path: Path = Path()): Path {
    path.reset()
    val cubics = this.cubics
    var first = true
    cubics.forEach { cubic ->
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y
        )
    }
    path.close()
    return path
}
