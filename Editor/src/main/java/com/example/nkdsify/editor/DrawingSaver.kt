package com.example.nkdsify.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.nkdsify.editor.ui.PathData
import com.example.nkdsify.editor.utils.toPorterDuffMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DrawingSaver(private val context: Context, private val imageLoader: ImageLoader) {

    suspend fun saveDrawing(
        uri: Uri,
        paths: List<PathData>,
        imageSize: IntSize,
        fittedImageSize: IntSize
    ) {
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()
            val originalBitmap = imageLoader.execute(request).drawable?.toBitmap(imageSize.width, imageSize.height)

            if (originalBitmap != null) {
                val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(bitmap)

                val scale = imageSize.width.toFloat() / fittedImageSize.width.toFloat()

                paths.forEach { (path, strokeWidth, color, blendMode) ->
                    val rescaledPath = android.graphics.Path()
                    path.asAndroidPath().transform(Matrix().apply {
                        postScale(imageSize.width.toFloat(), imageSize.height.toFloat())
                    }, rescaledPath)

                    val paint = Paint().apply {
                        this.color = color.toArgb()
                        this.strokeWidth = strokeWidth * scale
                        this.style = Paint.Style.STROKE
                        this.strokeCap = Paint.Cap.ROUND
                        this.strokeJoin = Paint.Join.ROUND
                        this.xfermode = PorterDuffXfermode(blendMode.toPorterDuffMode())
                    }
                    canvas.drawPath(rescaledPath, paint)
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "edited_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NkdsifyEdits")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val newImageFileUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (newImageFileUri != null) {
                    context.contentResolver.openOutputStream(newImageFileUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(newImageFileUri, values, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Edited image saved as new file.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Could not create new image file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image for saving.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
