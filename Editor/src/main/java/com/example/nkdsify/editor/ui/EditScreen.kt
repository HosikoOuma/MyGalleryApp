package com.example.nkdsify.editor.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

data class PathData(
    val path: Path,
    val strokeWidth: Float,
    val color: Color,
    val blendMode: BlendMode = BlendMode.SrcOver
)

@Composable
fun EditScreen(
    uri: Uri,
    onCropImage: (CropImageContractOptions) -> Unit,
    onSaveDrawing: (Uri, List<PathData>, IntSize, IntSize) -> Unit
) {
    var isMarkupMode by remember { mutableStateOf(false) }
    var finishedPaths by remember { mutableStateOf(listOf<PathData>()) }
    var currentPath by remember { mutableStateOf<PathData?>(null) }
    var isErasing by remember { mutableStateOf(false) }
    var currentStrokeWidth by remember { mutableStateOf(8f) }
    var currentColor by remember { mutableStateOf(Color.Red) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showThicknessPicker by remember { mutableStateOf(false) }

    var canvasSize by remember { mutableStateOf<IntSize?>(null) }
    var imageSize by remember { mutableStateOf<IntSize?>(null) }

    val painter = rememberAsyncImagePainter(
        model = uri,
        onSuccess = { state ->
            val drawable = state.result.drawable
            imageSize = IntSize(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
    )

    val imageAspectRatio = imageSize?.let {
        if (it.height == 0) 1f else it.width.toFloat() / it.height.toFloat()
    } ?: 1f

    val fittedImageSize = canvasSize?.let {
        val canvasAspectRatio = it.width.toFloat() / it.height.toFloat()
        if (canvasAspectRatio > imageAspectRatio) {
            IntSize( (it.height * imageAspectRatio).toInt(), it.height)
        } else {
            IntSize(it.width, (it.width / imageAspectRatio).toInt())
        }
    } ?: IntSize.Zero

    val offsetX = canvasSize?.let { (it.width - fittedImageSize.width) / 2f } ?: 0f
    val offsetY = canvasSize?.let { (it.height - fittedImageSize.height) / 2f } ?: 0f

    fun transformOffset(offset: Offset): Offset? {
        if (fittedImageSize == IntSize.Zero) return null

        val newX = (offset.x - offsetX) / fittedImageSize.width
        val newY = (offset.y - offsetY) / fittedImageSize.height

        return if (newX in 0f..1f && newY in 0f..1f) {
            Offset(newX, newY)
        } else {
            null
        }
    }

    Scaffold(
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .onSizeChanged { canvasSize = it },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Image for editing",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isMarkupMode, fittedImageSize) {
                        if (isMarkupMode && fittedImageSize != IntSize.Zero) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    transformOffset(startOffset)?.let { transformedStart ->
                                        currentPath = PathData(
                                            path = Path().apply { moveTo(transformedStart.x, transformedStart.y) },
                                            strokeWidth = if (isErasing) currentStrokeWidth else currentStrokeWidth,
                                            color = if (isErasing) Color.Transparent else currentColor,
                                            blendMode = if (isErasing) BlendMode.Clear else BlendMode.SrcOver
                                        )
                                    }
                                },
                                onDrag = { change, _ ->
                                    transformOffset(change.position)?.let { transformedPos ->
                                        currentPath?.let {
                                            val newPath = Path().apply {
                                                addPath(it.path)
                                                lineTo(transformedPos.x, transformedPos.y)
                                            }
                                            currentPath = it.copy(path = newPath)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    currentPath?.let {
                                        finishedPaths = finishedPaths + it
                                    }
                                    currentPath = null
                                },
                                onDragCancel = {
                                    currentPath = null
                                }
                            )
                        }
                    }
                ) {
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)

                        val allPaths = finishedPaths + listOfNotNull(currentPath)

                        allPaths.forEach { (path, strokeWidth, color, blendMode) ->
                            val rescaledPath = Path().apply {
                                addPath(path)
                                transform(Matrix().apply {
                                    translate(offsetX, offsetY)
                                    scale(fittedImageSize.width.toFloat(), fittedImageSize.height.toFloat())
                                })
                            }

                            drawPath(
                                path = rescaledPath,
                                color = color,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = blendMode
                            )
                        }
                        restoreToCount(checkPoint)
                    }
                }
            }
        },
        bottomBar = {
            if (isMarkupMode) {
                MarkupToolbar(
                    onDone = {
                        if (imageSize != null) {
                            onSaveDrawing(uri, finishedPaths, imageSize!!, fittedImageSize)
                        }
                        isMarkupMode = false
                    },
                    onCancel = {
                        finishedPaths = emptyList()
                        currentPath = null
                        isMarkupMode = false
                    },
                    onColorClick = { showColorPicker = true },
                    onThicknessClick = { showThicknessPicker = true },
                    onToggleErase = { isErasing = !isErasing },
                    isErasing = isErasing
                )
            } else {
                EditToolbar(
                    onCropClick = { onCropImage(CropImageContractOptions(uri, CropImageOptions())) },
                    onMarkupClick = { isMarkupMode = true }
                )
            }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                currentColor = color
                showColorPicker = false
            }
        )
    }

    if (showThicknessPicker) {
        ThicknessPickerDialog(
            onDismiss = { showThicknessPicker = false },
            onThicknessSelected = {
                currentStrokeWidth = it
                showThicknessPicker = false
            },
            currentThickness = currentStrokeWidth
        )
    }
}

@Composable
private fun EditToolbar(
    onCropClick: () -> Unit,
    onMarkupClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Crop, contentDescription = "Crop") },
            label = { Text("Crop") },
            selected = false,
            onClick = onCropClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.FormatPaint, contentDescription = "Markup") },
            label = { Text("Markup") },
            selected = false,
            onClick = onMarkupClick
        )
    }
}

@Composable
private fun MarkupToolbar(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onColorClick: () -> Unit,
    onThicknessClick: () -> Unit,
    onToggleErase: () -> Unit,
    isErasing: Boolean
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Done, contentDescription = "Done") },
            label = { Text("Done") },
            selected = false,
            onClick = onDone
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Circle, contentDescription = "Color") },
            label = { Text("Color") },
            selected = false,
            onClick = onColorClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.LineWeight, contentDescription = "Thickness") },
            label = { Text("Thickness") },
            selected = false,
            onClick = onThicknessClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Draw, contentDescription = "Pen/Eraser") },
            label = { Text(if (isErasing) "Eraser" else "Pen") },
            selected = isErasing,
            onClick = onToggleErase
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Clear, contentDescription = "Cancel") },
            label = { Text("Cancel") },
            selected = false,
            onClick = onCancel
        )
    }
}

@Composable
private fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Black, Color.White)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colors.forEach { color ->
                    IconButton(onClick = { onColorSelected(color) }) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = color,
                            border = null
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ThicknessPickerDialog(
    onDismiss: () -> Unit,
    onThicknessSelected: (Float) -> Unit,
    currentThickness: Float
) {
    var thickness by remember { mutableStateOf(currentThickness) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Thickness") },
        text = {
            Column {
                Slider(
                    value = thickness,
                    onValueChange = { thickness = it },
                    valueRange = 1f..50f
                )
                Text(text = "${thickness.toInt()}")
            }
        },
        confirmButton = {
            TextButton(onClick = { onThicknessSelected(thickness) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

val DrawScope.nativeCanvas: android.graphics.Canvas
    get() = drawContext.canvas.nativeCanvas

fun Path.transform(matrix: Matrix) {
    asAndroidPath().transform(matrix.toAndroidMatrix())
}

fun Matrix.toAndroidMatrix(): android.graphics.Matrix {
    val matrix = android.graphics.Matrix()
    val values = FloatArray(9)
    this.values.copyInto(values)
    matrix.setValues(values)
    return matrix
}
