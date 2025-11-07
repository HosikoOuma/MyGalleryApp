package com.example.nkdsify.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

object TrashRepository {

    private fun getTrashDir(context: Context): File {
        val trashDir = File(context.filesDir, ".trash")
        if (!trashDir.exists()) {
            trashDir.mkdirs()
        }
        return trashDir
    }

    fun getTrashedUris(context: Context): Set<Uri> {
        val trashDir = getTrashDir(context)
        return trashDir.listFiles()?.map { it.toUri() }?.toSet() ?: emptySet()
    }

    fun copyToTrash(context: Context, uris: List<Uri>): List<Uri> {
        val trashDir = getTrashDir(context)
        val successfullyCopiedOriginalUris = mutableListOf<Uri>()

        uris.forEach { uri ->
            try {
                val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
                
                var destinationFile = File(trashDir, fileName)
                var counter = 1
                while (destinationFile.exists()) {
                    val nameWithoutExtension = fileName.substringBeforeLast('.')
                    val extension = fileName.substringAfterLast('.', "")
                    val newName = if (extension.isNotEmpty()) {
                        "$nameWithoutExtension ($counter).$extension"
                    } else {
                        "$nameWithoutExtension ($counter)"
                    }
                    destinationFile = File(trashDir, newName)
                    counter++
                }

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    successfullyCopiedOriginalUris.add(uri)
                }
            } catch (e: Exception) { // Catch broader exceptions
                e.printStackTrace()
            }
        }
        return successfullyCopiedOriginalUris
    }

    fun restoreFromTrash(context: Context, uri: Uri) {
        try {
            val sourceFile = uri.path?.let { File(it) } ?: return
            if (!sourceFile.exists()) return

            val fileName = sourceFile.name
            val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
            val isVideo = extension in listOf("mp4", "mkv", "webm", "3gp")

            val collection = if (isVideo) {
                 MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                 MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            
            val mediaUri = context.contentResolver.insert(collection, contentValues)

            mediaUri?.let { newUri ->
                context.contentResolver.openOutputStream(newUri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(newUri, contentValues, null, null)
                sourceFile.delete()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
    
    fun removeFromTrash(context: Context, uris: List<Uri>) {
        uris.forEach { restoreFromTrash(context, it) }
    }

    fun clearTrash(context: Context) {
        val trashDir = getTrashDir(context)
        trashDir.listFiles()?.forEach { it.delete() }
    }
}