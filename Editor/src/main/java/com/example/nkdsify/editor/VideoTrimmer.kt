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
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.TransformationException
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.TransformationResult
import androidx.media3.transformer.Transformer
import java.io.File

@OptIn(UnstableApi::class)
class VideoTrimmer(private val context: Context) {

    fun trimVideo(
        uri: Uri,
        startPositionMs: Long,
        endPositionMs: Long,
        onFinished: (Boolean) -> Unit
    ) {
        val outputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startPositionMs)
                    .setEndPositionMs(endPositionMs)
                    .build()
            )
            .build()

        val listener = object : Transformer.Listener {
            override fun onTransformationCompleted(mediaItem: MediaItem, result: TransformationResult) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outputFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, MimeTypes.VIDEO_MP4)
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
                             Toast.makeText(context, "Video trimmed successfully", Toast.LENGTH_SHORT).show()
                        }
                        onFinished(true)

                    } catch (e: Exception) {
                         Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Error saving video to MediaStore: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        onFinished(false)
                    } finally {
                        outputFile.delete()
                    }
                } else {
                     Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Could not create new video file in MediaStore.", Toast.LENGTH_SHORT).show()
                    }
                    outputFile.delete()
                    onFinished(false)
                }
            }

            override fun onTransformationError(mediaItem: MediaItem, exception: TransformationException) {
                outputFile.delete()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to trim video: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                onFinished(false)
            }
        }
        
        val transformationRequest = TransformationRequest.Builder().build()

        val transformer = Transformer.Builder(context)
            .setTransformationRequest(transformationRequest)
            .setEnableVideoEncoderFallback(false)
            .addListener(listener)
            .build()

        transformer.start(mediaItem, outputFile.absolutePath)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Processing video...", Toast.LENGTH_SHORT).show()
        }
    }
}