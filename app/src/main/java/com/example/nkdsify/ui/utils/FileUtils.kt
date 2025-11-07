package com.example.nkdsify.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun copyMediaToFolder(context: Context, uri: Uri, targetRelativePath: String) {
    withContext(Dispatchers.IO) {
        val fileName = getFileName(context, uri) ?: return@withContext

        // Construct the full, absolute path
        val destinationDir = File(Environment.getExternalStorageDirectory(), targetRelativePath)
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        val destinationFile = File(destinationDir, fileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Notify MediaStore about the new file
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, context.contentResolver.getType(uri))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                }
                put(MediaStore.MediaColumns.DATA, destinationFile.absolutePath)
            }

            val collection = if (isImage(context, uri)) {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            context.contentResolver.insert(collection, contentValues)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

suspend fun moveMediaToFolder(context: Context, uri: Uri, targetRelativePath: String) {
    withContext(Dispatchers.IO) {
        // In the context of MANAGE_EXTERNAL_STORAGE, move is copy + delete
        copyMediaToFolder(context, uri, targetRelativePath)
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            // If deletion fails, the original file might remain, but the copy is complete.
        }
    }
}

private fun isImage(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    return mimeType?.startsWith("image/") ?: false
}

private fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}

fun getFolderPathFromUri(context: Context, uri: Uri): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // Fallback for older Android versions if needed
        return File(uri.path ?: "").parent
    }
    var relativePath: String? = null
    try {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                if (pathIndex != -1) {
                    relativePath = cursor.getString(pathIndex)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return relativePath
}
