package com.example.nkdsify.ui.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.nkdsify.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object SecretRepository {
    private const val SECRET_FOLDER_NAME = ".secret"

    private fun getSecretFolder(context: Context): File {
        return File(context.filesDir, SECRET_FOLDER_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun moveToSecret(context: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val secretFolder = getSecretFolder(context)
        uris.forEach { uri ->
            var isSuccess = false
            try {
                val (originalFileName, _) = getFileInfo(context, uri)
                if (originalFileName != null) {
                    val encryptedFile = File(secretFolder, originalFileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        encryptedFile.outputStream().use { output ->
                            CryptoUtils.encrypt(input, output)
                        }
                    }
                    isSuccess = true
                }
            } catch (e: Exception) {
                Log.e("SecretRepository", "Failed to move to secret storage: ${e.message}", e)
                isSuccess = false
            }

            if (isSuccess) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    Log.e("SecretRepository", "Failed to delete original file after moving to secret: ${e.message}", e)
                }
            }
        }
    }

    suspend fun restoreFromSecret(context: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        uris.forEach { uri ->
            try {
                val encryptedFile = File(uri.path!!)
                val restoredFile = File(picturesFolder, encryptedFile.name)

                encryptedFile.inputStream().use { input ->
                    restoredFile.outputStream().use { output ->
                        CryptoUtils.decrypt(input, output)
                    }
                }

                // Wait for the media scanner to complete before proceeding
                suspendCancellableCoroutine<Unit> { continuation ->
                    MediaScannerConnection.scanFile(context, arrayOf(restoredFile.absolutePath), null) { _, _ ->
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }

                encryptedFile.delete()

            } catch (e: Exception) {
                Log.e("SecretRepository", "Failed to restore from secret storage: ${e.message}", e)
            }
        }
    }

    suspend fun deleteFromSecret(context: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            try {
                val file = File(uri.path!!)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("SecretRepository", "Failed to delete from secret storage: ${e.message}", e)
            }
        }
    }

    suspend fun getSecretMediaItems(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val secretFolder = getSecretFolder(context)
        secretFolder.listFiles()?.mapNotNull { file ->
            try {
                val uri = Uri.fromFile(file)
                val name = file.name
                val isVideo = name.endsWith(".mp4", true) || name.endsWith(".webm", true) // Simple check
                MediaItem(uri, name, isVideo, 0, 0, 0)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }
}