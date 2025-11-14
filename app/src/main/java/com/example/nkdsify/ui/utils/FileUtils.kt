package com.example.nkdsify.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun copyMediaToFolder(context: Context, uri: Uri, targetRelativePath: String) {
    withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.SIZE
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                val dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))

                val availableName = findAvailableName(context, targetRelativePath, name)

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, availableName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.DATE_ADDED, dateAdded)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, dateModified)
                    put(MediaStore.MediaColumns.DATE_TAKEN, dateTaken)
                    put(MediaStore.MediaColumns.SIZE, size)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val collection = if (mimeType.startsWith("image/")) {
                    MediaStore.Images.Media.getContentUri("external")
                } else {
                    MediaStore.Video.Media.getContentUri("external")
                }

                val newUri = context.contentResolver.insert(collection, contentValues)

                if (newUri != null) {
                    try {
                        context.contentResolver.openOutputStream(newUri)?.use { outputStream ->
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            context.contentResolver.update(newUri, contentValues, null, null)
                        }
                    } catch (e: Exception) {
                        context.contentResolver.delete(newUri, null, null)
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

suspend fun moveMediaToFolder(context: Context, uri: Uri, targetRelativePath: String) {
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+ supports reliable move
            val originalName = getFileName(context, uri) ?: return@withContext
            val finalName = findAvailableName(context, targetRelativePath, originalName)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
            }
            try {
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                // Fallback to copy-then-delete for edge cases
                copyMediaToFolder(context, uri, targetRelativePath)
                context.contentResolver.delete(uri, null, null)
            }
        } else { // Fallback for Android 10
            copyMediaToFolder(context, uri, targetRelativePath)
            context.contentResolver.delete(uri, null, null)
        }
    }
}

private fun findAvailableName(context: Context, relativePath: String, originalName: String): String {
    var newName = originalName
    var counter = 1
    val projection = arrayOf(MediaStore.Files.FileColumns._ID)
    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
    val collection = MediaStore.Files.getContentUri("external")

    while (true) {
        val selectionArgs = arrayOf("$relativePath/", newName)
        val cursor = context.contentResolver.query(collection, projection, selection, selectionArgs, null)
        val fileExists = cursor?.use { it.count > 0 } ?: false
        if (!fileExists) {
            return newName
        }

        val nameWithoutExtension = originalName.substringBeforeLast('.')
        val extension = originalName.substringAfterLast('.', "")
        newName = if (extension.isNotEmpty()) {
            "$nameWithoutExtension($counter).$extension"
        } else {
            "$originalName($counter)"
        }
        counter++
    }
}

internal fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    if (uri.scheme == "content") {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                fileName = cursor.getString(nameIndex)
            }
        }
    } else {
        fileName = uri.path?.substringAfterLast('/')
    }
    return fileName
}

internal fun getFileInfo(context: Context, uri: Uri): Pair<String?, String?> {
    if (uri.scheme == "content") {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA)
        }
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val name = if (nameIndex != -1) cursor.getString(nameIndex) else null

                var path: String? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    if (pathIndex != -1) {
                        path = cursor.getString(pathIndex)
                    }
                } else {
                    val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (dataIndex != -1) {
                        val data = cursor.getString(dataIndex)
                        val file = File(data)
                        val externalStoragePath = Environment.getExternalStorageDirectory().path
                        file.parent?.let { parentPath ->
                            if (parentPath.startsWith(externalStoragePath)) {
                                path = parentPath.substring(externalStoragePath.length).removePrefix("/")
                            }
                        }
                    }
                }
                return Pair(name, path)
            }
        }
    }
    val path = uri.path
    return Pair(path?.substringAfterLast('/'), path?.substringBeforeLast('/'))
}

suspend fun createThumbnail(context: Context, uri: Uri, quality: Int = 50): File? = withContext(Dispatchers.IO) {
    return@withContext try {
        // Используем современный и надежный способ ContentResolver.loadThumbnail (требует API 29+, наша minSdk = 29)
        val bitmap = context.contentResolver.loadThumbnail(uri, Size(256, 256), null)

        bitmap?.let {
            val tempFile = File.createTempFile("thumb", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            tempFile
        }
    } catch (e: Exception) {
        null // Если создание превью не удалось, просто возвращаем null
    }
}


fun getFolderPathFromUri(context: Context, uri: Uri): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return File(uri.path ?: "").parent
    }
    var relativePath: String? = null
    try {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                relativePath = cursor.getString(pathIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return relativePath
}
