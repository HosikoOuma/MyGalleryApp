package com.example.nkdsify.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Stack

class PhotoEditorViewModel : ViewModel() {
    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap

    private var baseBitmap: Bitmap? = null

    private val _paths = mutableStateListOf<DrawPath>()
    val paths: List<DrawPath> get() = _paths

    private val undoStack = Stack<List<DrawPath>>()
    private val redoStack = Stack<List<DrawPath>>()

    var drawColor by mutableStateOf(Color.Red)
    var strokeWidth by mutableStateOf(10f)
    var isEraser by mutableStateOf(false)
    var activeTab by mutableStateOf(EditorTab.DRAW)
    
    var isCropMode by mutableStateOf(false)
    var cropRect by mutableStateOf(Rect(0f, 0f, 1f, 1f))

    var currentFilter by mutableStateOf(PhotoFilter.NONE)

    var isSaving by mutableStateOf(false)

    fun loadBitmap(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = decodeSampledBitmapFromUri(context, uri, 2048, 2048)
            baseBitmap = bitmap
            _currentBitmap.value = bitmap
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inMutable = true

        return context.contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, options) 
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun addPath(drawPath: DrawPath) {
        undoStack.push(_paths.toList())
        _paths.add(drawPath)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_paths.toList())
            val previous = undoStack.pop()
            _paths.clear()
            _paths.addAll(previous)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_paths.toList())
            val next = redoStack.pop()
            _paths.clear()
            _paths.addAll(next)
        }
    }

    fun rotate(degrees: Float) {
        bakePaths()
        baseBitmap?.let { bitmap ->
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            baseBitmap = rotated
            applyCurrentFilter()
        }
    }

    fun flipHorizontal() {
        bakePaths()
        baseBitmap?.let { bitmap ->
            val matrix = Matrix().apply { postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f) }
            val flipped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            baseBitmap = flipped
            applyCurrentFilter()
        }
    }

    private fun bakePaths() {
        val original = baseBitmap ?: return
        if (_paths.isEmpty()) {
            _currentBitmap.value = original
            return
        }

        val resultBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val resultCanvas = android.graphics.Canvas(resultBitmap)

        val drawingBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val drawingCanvas = android.graphics.Canvas(drawingBitmap)
        
        _paths.forEach { drawPath ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = drawPath.color.toArgb()
                strokeWidth = drawPath.strokeWidth
                style = android.graphics.Paint.Style.STROKE
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeCap = android.graphics.Paint.Cap.ROUND
                if (drawPath.isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            }
            drawingCanvas.drawPath(drawPath.path.asAndroidPath(), paint)
        }
        resultCanvas.drawBitmap(drawingBitmap, 0f, 0f, null)
        drawingBitmap.recycle()
        
        baseBitmap = resultBitmap
        _currentBitmap.value = resultBitmap 
        _paths.clear()
        undoStack.clear()
        redoStack.clear()
    }

    fun onTabChanged(newTab: EditorTab) {
        if (activeTab == EditorTab.DRAW && newTab != EditorTab.DRAW) {
            bakePaths()
            applyCurrentFilter()
        }
        activeTab = newTab
    }

    fun toggleCropMode(enabled: Boolean) {
        if (enabled) {
            bakePaths()
            cropRect = Rect(0f, 0f, 1f, 1f)
        }
        isCropMode = enabled
    }

    fun applyCrop() {
        val currentBase = baseBitmap ?: return
        val rect = cropRect
        
        if (_paths.isNotEmpty()) bakePaths()
        
        val current = baseBitmap ?: currentBase
        
        val left = (rect.left * current.width).toInt().coerceIn(0, current.width - 1)
        val top = (rect.top * current.height).toInt().coerceIn(0, current.height - 1)
        val width = (rect.width * current.width).toInt().coerceIn(1, current.width - left)
        val height = (rect.height * current.height).toInt().coerceIn(1, current.height - top)

        try {
            baseBitmap = Bitmap.createBitmap(current, left, top, width, height)
            applyCurrentFilter()
            isCropMode = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setFilter(filter: PhotoFilter) {
        currentFilter = filter
        applyCurrentFilter()
    }

    private fun getFilterMatrix(filter: PhotoFilter): ColorMatrix {
        return when (filter) {
            PhotoFilter.B_AND_W -> ColorMatrix().apply { setSaturation(0f) }
            PhotoFilter.SEPIA -> ColorMatrix().apply {
                set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            PhotoFilter.INVERT -> ColorMatrix().apply {
                set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            PhotoFilter.VINTAGE -> ColorMatrix().apply {
                set(floatArrayOf(
                    0.9f, 0f, 0f, 0f, 0f,
                    0f, 0.9f, 0f, 0f, 0f,
                    0f, 0f, 0.5f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            PhotoFilter.COOL -> ColorMatrix().apply {
                set(floatArrayOf(
                    1f, 0f, 0f, 0f, 30f,
                    0f, 1f, 0f, 0f, 50f,
                    0f, 0f, 1.2f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            PhotoFilter.WARM -> ColorMatrix().apply {
                set(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 30f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            else -> ColorMatrix()
        }
    }

    private fun applyCurrentFilter() {
        val base = baseBitmap ?: return
        if (currentFilter == PhotoFilter.NONE) {
            _currentBitmap.value = base
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val matrix = getFilterMatrix(currentFilter)
            val result = base.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(result)
            val paint = android.graphics.Paint().apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(base, 0f, 0f, paint)
            
            withContext(Dispatchers.Main) {
                _currentBitmap.value = result
            }
        }
    }

    fun saveResult(context: Context, onComplete: (Boolean) -> Unit) {
        isSaving = true

        viewModelScope.launch(Dispatchers.IO) {
            if (isCropMode) {
                withContext(Dispatchers.Main) { applyCrop() }
            } else {
                withContext(Dispatchers.Main) { bakePaths() }
            }
            
            val base = baseBitmap ?: return@launch
            
            // ГЕНЕРИРУЕМ ФИНАЛЬНЫЙ БИТМАП С ФИЛЬТРОМ СИНХРОННО ДЛЯ СОХРАНЕНИЯ
            val finalToSave = if (currentFilter != PhotoFilter.NONE) {
                val matrix = getFilterMatrix(currentFilter)
                val result = base.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(result)
                val paint = android.graphics.Paint().apply {
                    colorFilter = ColorMatrixColorFilter(matrix)
                }
                canvas.drawBitmap(base, 0f, 0f, paint)
                result
            } else {
                base
            }

            val filename = "Nekolery_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Nekolery")
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val success = uri?.let { targetUri ->
                context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                    finalToSave.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                } != null
            } ?: false

            withContext(Dispatchers.Main) {
                isSaving = false
                onComplete(success)
            }
        }
    }
}
