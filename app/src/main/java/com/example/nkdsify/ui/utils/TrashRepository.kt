package com.example.nkdsify.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
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
        return trashDir.listFiles { _, name -> !name.endsWith(".path") }
            ?.map { it.toUri() }
            ?.toSet() ?: emptySet()
    }

    fun copyToTrash(context: Context, uris: List<Uri>): List<Uri> {
        val trashDir = getTrashDir(context)
        val successfullyCopiedOriginalUris = mutableListOf<Uri>()

        uris.forEach { uri ->
            try {
                val (fileName, relativePath) = getFileInfo(context, uri)
                if (fileName == null) {
                    return@forEach
                }

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

                    if (relativePath != null) {
                        val pathFile = File(trashDir, destinationFile.name + ".path")
                        pathFile.writeText(relativePath)
                    }

                    successfullyCopiedOriginalUris.add(uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return successfullyCopiedOriginalUris
    }

    fun restoreFromTrash(context: Context, itemsToRestore: List<Uri>) {
        itemsToRestore.forEach { uri ->
            try {
                val sourceFile = uri.path?.let { File(it) } ?: return@forEach
                if (!sourceFile.exists()) return@forEach

                val fileName = sourceFile.name
                val pathFile = File(sourceFile.parentFile, sourceFile.name + ".path")
                val relativePath = if (pathFile.exists()) pathFile.readText() else null

                val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
                val isVideo = extension in listOf("mp4", "mkv", "webm", "3gp")

                val collection = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    if (relativePath != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val topDir = relativePath.substringBefore('/', missingDelimiterValue = "")
                        val finalPath = if (isVideo) {
                            if (topDir.equals("DCIM", true) || topDir.equals("Movies", true) || topDir.equals("Videos", true)) relativePath else "Movies/$relativePath"
                        } else {
                            if (topDir.equals("DCIM", true) || topDir.equals("Pictures", true)) relativePath else "Pictures/$relativePath"
                        }
                        put(MediaStore.MediaColumns.RELATIVE_PATH, finalPath)
                    }
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
                    if (pathFile.exists()) pathFile.delete()
                }
            } catch (e: Exception) { 
                e.printStackTrace()
            }
        }
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String?, String?> {
        if (uri.scheme == "content") {
            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
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
                    }
                    return Pair(name, path)
                }
            }
        }
        val path = uri.path
        return Pair(path?.substringAfterLast('/'), path?.substringBeforeLast('/'))
    }

    fun removeFromTrash(context: Context, uris: List<Uri>) {
        uris.forEach { uri ->
            try {
                val sourceFile = uri.path?.let { File(it) } ?: return@forEach
                if (sourceFile.exists()) {
                    val pathFile = File(sourceFile.parentFile, sourceFile.name + ".path")
                    sourceFile.delete()
                    if (pathFile.exists()) {
                        pathFile.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearTrash(context: Context) {
        val trashDir = getTrashDir(context)
        trashDir.listFiles()?.forEach { it.delete() }
    }
}