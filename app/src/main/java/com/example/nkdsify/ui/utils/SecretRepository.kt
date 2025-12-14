package com.example.nkdsify.ui.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.nkdsify.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object SecretRepository {
    private const val SECRET_FOLDER_NAME = ".secret"
    private const val THUMBNAIL_SUFFIX = ".thumb" // Keep for cleanup of old files, but no longer create new ones

    internal fun getSecretFolder(context: Context): File {
        return File(context.filesDir, SECRET_FOLDER_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun moveToSecret(context: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val secretFolder = getSecretFolder(context)
        uris.map { uri ->
            async {
                var isSuccess = false
                try {
                    val (originalFileName, _) = getFileInfo(context, uri)
                    if (originalFileName != null) {
                        val encryptedFile = File(secretFolder, originalFileName)

                        // Encrypt original file
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            encryptedFile.outputStream().use { output ->
                                CryptoUtils.encrypt(input, output)
                            }
                        }

                        // Thumbnail creation is now removed. Coil will handle this automatically.

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
        }.awaitAll()
    }

    suspend fun restoreFromSecret(context: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        uris.map { uri ->
            async {
                try {
                    val encryptedFile = File(uri.path!!)
                    // Legacy thumbnail file, to be deleted if it exists
                    val encryptedThumbFile = File(uri.path!! + THUMBNAIL_SUFFIX)
                    val restoredFile = File(picturesFolder, encryptedFile.name)

                    encryptedFile.inputStream().use { input ->
                        restoredFile.outputStream().use { output ->
                            CryptoUtils.decrypt(input, output)
                        }
                    }

                    suspendCancellableCoroutine<Unit> { continuation ->
                        MediaScannerConnection.scanFile(context, arrayOf(restoredFile.absolutePath), null) { _, _ ->
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    encryptedFile.delete()
                    // Delete the old thumbnail file if it exists
                    if (encryptedThumbFile.exists()) encryptedThumbFile.delete()

                } catch (e: Exception) {
                    Log.e("SecretRepository", "Failed to restore from secret storage: ${e.message}", e)
                }
            }
        }.awaitAll()
    }

    suspend fun deleteFromSecret(context: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        uris.map { uri ->
            async {
                try {
                    val file = File(uri.path!!)
                    // Legacy thumbnail file, to be deleted if it exists
                    val thumbFile = File(uri.path!! + THUMBNAIL_SUFFIX)
                    if (file.exists()) file.delete()
                    if (thumbFile.exists()) thumbFile.delete()
                } catch (e: Exception) {
                    Log.e("SecretRepository", "Failed to delete from secret storage: ${e.message}", e)
                }
            }
        }.awaitAll()
    }

    suspend fun getSecretMediaItems(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val secretFolder = getSecretFolder(context)
        // This filter will now correctly ignore any old .thumb files that might still exist.
        secretFolder.listFiles { _, name -> !name.endsWith(THUMBNAIL_SUFFIX) }?.mapNotNull { file ->
            try {
                val uri = Uri.fromFile(file)
                val name = file.name
                val isVideo = name.endsWith(".mp4", true) || name.endsWith(".webm", true) 
                // The URI for MediaItem should point to the full file, not the thumbnail
                MediaItem(uri, name, file.absolutePath, isVideo, 0, 0, 0)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }
}
