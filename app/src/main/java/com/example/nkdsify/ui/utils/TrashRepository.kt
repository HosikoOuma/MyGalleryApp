package com.example.nkdsify.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object TrashRepository {

    private fun getTrashDir(context: Context): File {
        val trashDir = File(Environment.getExternalStorageDirectory(), ".trash")
        if (!trashDir.exists()) {
            trashDir.mkdirs()
        }
        // Ensure .nomedia file exists to hide media from scanners
        val noMedia = File(trashDir, ".nomedia")
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile()
            } catch (e: Exception) {
                // Log or handle the exception if needed
                e.printStackTrace()
            }
        }
        return trashDir
    }

    fun getTrashedUris(context: Context): Set<Uri> {
        val trashDir = getTrashDir(context)
        return trashDir.listFiles { _, name -> !name.endsWith(".path") && !name.endsWith(".nomedia") } 
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

                    val pathFile = File(trashDir, destinationFile.name + ".path")
                    pathFile.writeText(relativePath ?: "")

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    if (relativePath != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        } else {
                            val targetDir = File(Environment.getExternalStorageDirectory(), relativePath)
                            if (!targetDir.exists()) {
                                targetDir.mkdirs()
                            }
                            put(MediaStore.MediaColumns.DATA, File(targetDir, fileName).absolutePath)
                        }
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
        trashDir.listFiles()?.forEach { 
            if (it.name != ".nomedia") {
                it.delete()
            }
        }
    }

    fun deleteExpired(context: Context, days: Int) {
        if (days <= 0) return

        val expirationTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        val trashDir = getTrashDir(context)

        trashDir.listFiles { _, name -> name.endsWith(".path") }?.forEach { pathFile ->
            if (pathFile.lastModified() < expirationTime) {
                val mediaFile = File(trashDir, pathFile.name.removeSuffix(".path"))
                if (mediaFile.exists()) {
                    mediaFile.delete()
                }
                pathFile.delete()
            }
        }
    }
}