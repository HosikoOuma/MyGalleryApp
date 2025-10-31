package com.example.nkdsify.editor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoSaver(private val context: Context) {

    @OptIn(UnstableApi::class)
    suspend fun saveVideo(uri: Uri, isMuted: Boolean) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Processing video...", Toast.LENGTH_SHORT).show()
        }

        withContext(Dispatchers.IO) {
            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(uri))
                .setRemoveAudio(isMuted)
                .build()

            val composition = Composition.Builder(EditedMediaItemSequence(editedMediaItem)).build()

            val outputFile = File(context.cacheDir, "edited_${System.currentTimeMillis()}.mp4")

            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        // Now copy the file to MediaStore
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, outputFile.name)
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NkdsifyEdits")
                                put(MediaStore.Video.Media.IS_PENDING, 1)
                            }
                        }

                        val newVideoFileUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

                        if (newVideoFileUri != null) {
                            try {
                                context.contentResolver.openOutputStream(newVideoFileUri)?.use { outputStream ->
                                    outputFile.inputStream().use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    values.clear()
                                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                                    context.contentResolver.update(newVideoFileUri, values, null, null)
                                }
                                
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Video saved successfully.", Toast.LENGTH_SHORT).show()
                                }

                            } catch (e: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Error saving video to MediaStore: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                outputFile.delete()
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Could not create new video file in MediaStore.", Toast.LENGTH_SHORT).show()
                            }
                            outputFile.delete()
                        }
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        outputFile.delete()
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Error saving video: ${exportException.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                })
                .build()

            transformer.start(composition, outputFile.absolutePath)
        }
    }
}
