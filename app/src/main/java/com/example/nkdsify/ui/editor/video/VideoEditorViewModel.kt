package com.example.nkdsify.ui.editor.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

@OptIn(UnstableApi::class)
class VideoEditorViewModel : ViewModel() {
    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri

    var trimStart by mutableLongStateOf(0L)
    var trimEnd by mutableLongStateOf(0L)
    var videoDuration by mutableLongStateOf(0L)
    
    var currentPosition by mutableLongStateOf(0L)

    var videoWidth by mutableIntStateOf(0)
    var videoHeight by mutableIntStateOf(0)

    var isMuted by mutableStateOf(false)
    var isCropMode by mutableStateOf(false)
    var cropRect by mutableStateOf(Rect(0f, 0f, 1f, 1f))

    var player: ExoPlayer? = null
    private val _isPlaying = mutableStateOf(false)
    val isPlaying: Boolean get() = _isPlaying.value

    val thumbnails = mutableStateListOf<Bitmap>()
    var isExporting by mutableStateOf(false)

    private var playbackJob: Job? = null

    fun loadVideo(context: Context, uri: Uri) {
        if (player != null) return
        _videoUri.value = uri
        
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val rWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val rHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            trimEnd = videoDuration

            if (rotation == 90 || rotation == 270) {
                videoWidth = rHeight
                videoHeight = rWidth
            } else {
                videoWidth = rWidth
                videoHeight = rHeight
            }
        } catch (e: Exception) { e.printStackTrace() }
        finally { retriever.release() }

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }
            })
        }
        generateThumbnails(context, uri)
        startPlaybackBoundaryWatcher()
    }

    private fun startPlaybackBoundaryWatcher() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive) {
                player?.let { p ->
                    currentPosition = p.currentPosition
                    if (p.isPlaying && p.currentPosition >= trimEnd) {
                        p.pause()
                        p.seekTo(trimStart)
                    }
                }
                delay(100)
            }
        }
    }

    fun togglePlay() {
        player?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
        currentPosition = position
    }

    fun onTrimChanged(start: Long, end: Long) {
        trimStart = start
        trimEnd = end
        seekTo(start)
    }

    private fun generateThumbnails(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val count = 8
                val interval = videoDuration / count
                if (interval <= 0) return@launch
                for (i in 0 until count) {
                    val timeUs = i * interval * 1000
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bitmap?.let {
                        val scaled = Bitmap.createScaledBitmap(it, 120, 120, false)
                        withContext(Dispatchers.Main) { thumbnails.add(scaled) }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { retriever.release() }
        }
    }

    fun export(context: Context, onComplete: (Boolean) -> Unit) {
        val uri = _videoUri.value ?: return
        player?.pause()
        isExporting = true

        viewModelScope.launch(Dispatchers.IO) {
            val outputPath = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")
            val transformer = Transformer.Builder(context).setVideoMimeType(MimeTypes.VIDEO_H264).build()

            val videoEffects = mutableListOf<Effect>()
            if (cropRect != Rect(0f, 0f, 1f, 1f)) {
                // ПРАВИЛЬНЫЙ МАППИНГ: GL coords [-1, 1], Y axis is UP
                val glLeft = (cropRect.left * 2f) - 1f
                val glRight = (cropRect.right * 2f) - 1f
                // Инвертируем Y: 0 в Android (верх) -> 1 в GL, 1 в Android (низ) -> -1 в GL
                val glTop = 1f - (cropRect.top * 2f)
                val glBottom = 1f - (cropRect.bottom * 2f)
                
                // Crop эффект в Media3 принимает (left, right, bottom, top)
                videoEffects.add(Crop(glLeft, glRight, glBottom, glTop))
            }

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStart).setEndPositionMs(trimEnd).build())
                .build()

            val editedItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(isMuted)
                .setEffects(androidx.media3.transformer.Effects(emptyList<AudioProcessor>(), videoEffects))
                .build()

            withContext(Dispatchers.Main) {
                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        saveToGallery(context, outputPath, onComplete)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, e: ExportException) {
                        isExporting = false
                        onComplete(false)
                    }
                })
                transformer.start(editedItem, outputPath.absolutePath)
            }
        }
    }

    private fun saveToGallery(context: Context, file: File, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "Nekolery_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/Nekolery")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            val success = uri?.let { target ->
                context.contentResolver.openOutputStream(target)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } != null
            } ?: false
            file.delete()
            withContext(Dispatchers.Main) {
                isExporting = false
                onComplete(success)
            }
        }
    }

    override fun onCleared() {
        player?.release()
        player = null
        playbackJob?.cancel()
    }
}
