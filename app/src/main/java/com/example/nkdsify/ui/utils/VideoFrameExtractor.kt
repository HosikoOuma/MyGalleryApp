package com.example.nkdsify.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Утилита для извлечения кадров из видеофайлов с кэшированием
 */
object VideoFrameExtractor {
    private const val TAG = "VideoFrameExtractor"
    private const val CACHE_DIR = "video_frames_cache"

    // In-memory кэш для быстрого доступа
    private val memoryCache = mutableMapOf<String, Map<FrameType, Bitmap?>>()

    /**
     * Масштабирует битмап в зависимости от выбранного качества
     */
    private fun scaleBitmap(bitmap: Bitmap?, useLowQuality: Boolean): Bitmap? {
        if (bitmap == null || !useLowQuality) return bitmap

        val newWidth = (bitmap.width * 0.25).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * 0.25).roundToInt().coerceAtLeast(1)

        return if (newWidth == bitmap.width && newHeight == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }

    /**
     * Типы кадров для извлечения
     */
    enum class FrameType {
        START,      // Первый кадр
        MIDDLE,     // Средний кадр
        END         // Последний кадр
    }

    /**
     * Извлекает кадры видео (начало, середина, конец)
     * @param context контекст приложения
     * @param videoUri URI видеофайла
     * @return Map с кадрами типов START, MIDDLE, END (или null если невозможно извлечь)
     */
    suspend fun extractFrames(
        context: Context,
        videoUri: Uri,
        useLowQuality: Boolean
    ): Map<FrameType, Bitmap?>? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return@withContext null

            if (duration <= 0) {
                retriever.release()
                return@withContext null
            }

            // Извлекаем три кадра: начало, середина, конец
            val frames = mutableMapOf<FrameType, Bitmap?>()

            // Начало видео (1% от продолжительности)
            val startFrame = retriever.getFrameAtTime(
                (duration * 0.01).toLong() * 1000, // Микросекунды
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            frames[FrameType.START] = scaleBitmap(startFrame, useLowQuality)

            // Середина видео (50%)
            val middleFrame = retriever.getFrameAtTime(
                (duration * 0.5).toLong() * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            frames[FrameType.MIDDLE] = scaleBitmap(middleFrame, useLowQuality)

            // Конец видео (99%)
            val endFrame = retriever.getFrameAtTime(
                (duration * 0.99).toLong() * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            frames[FrameType.END] = scaleBitmap(endFrame, useLowQuality)

            retriever.release()

            // Проверяем что хотя бы один кадр извлечен
            if (frames.values.any { it != null }) {
                frames
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract frames from video: ${e.message}", e)
            null
        }
    }

    /**
     * Загружает кадры из файла кэша
     */
    private suspend fun loadCachedFrames(
        context: Context,
        cacheKey: String
    ): Map<FrameType, Bitmap?>? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) return@withContext null

            val frames = mutableMapOf<FrameType, Bitmap?>()
            var foundAny = false

            for (frameType in FrameType.entries) {
                val fileName = "${cacheKey}_${frameType.name.lowercase()}.bmp"
                val file = File(cacheDir, fileName)

                if (file.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        frames[frameType] = bitmap
                        foundAny = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load cached frame $frameType: ${e.message}")
                        frames[frameType] = null
                    }
                } else {
                    frames[frameType] = null
                }
            }

            if (foundAny) frames else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached frames: ${e.message}")
            null
        }
    }

    /**
     * Получает кадры с полным кэшированием (memory + disk)
     * @param context контекст приложения
     * @param videoUri URI видеофайла
     * @return Map с кадрами или null
     */
    suspend fun getFramesWithCache(
        context: Context,
        videoUri: Uri,
        useLowQuality: Boolean
    ): Map<FrameType, Bitmap?>? = withContext(Dispatchers.IO) {
        val qualitySuffix = if (useLowQuality) "_low" else ""
        val cacheKey = videoUri.toString().hashCode().toString() + qualitySuffix

        // Проверяем in-memory кэш
        memoryCache[cacheKey]?.let {
            Log.d(TAG, "Loading frames from memory cache for: $cacheKey")
            return@withContext it
        }

        // Проверяем disk кэш
        val cachedFrames = loadCachedFrames(context, cacheKey)
        if (cachedFrames != null) {
            Log.d(TAG, "Loading frames from disk cache for: $cacheKey")
            memoryCache[cacheKey] = cachedFrames
            return@withContext cachedFrames
        }

        // Извлекаем заново если нет в кэше
        Log.d(TAG, "Extracting frames for: $cacheKey")
        val frames = extractFrames(context, videoUri, useLowQuality)

        if (frames != null) {
            // Кэшируем в памяти
            memoryCache[cacheKey] = frames
            // Кэшируем на диск
            cacheFrames(context, cacheKey, frames)
        }

        frames
    }

    /**
     * Кэширует извлеченные кадры на диск
     */
    private suspend fun cacheFrames(
        context: Context,
        cacheKey: String,
        frames: Map<FrameType, Bitmap?>
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR).apply {
                if (!exists()) mkdirs()
            }

            frames.forEach { (frameType, bitmap) ->
                if (bitmap != null) {
                    val fileName = "${cacheKey}_${frameType.name.lowercase()}.bmp"
                    val file = File(cacheDir, fileName)

                    try {
                        file.outputStream().use { output ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 85, output)
                            output.flush()
                        }
                        Log.d(TAG, "Cached frame $frameType to: ${file.absolutePath}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to cache frame $frameType: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache frames: ${e.message}")
        }
    }

    /**
     * Очищает старый кэш видео кадров (старше 7 дней)
     */
    suspend fun cleanOldCache(context: Context, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, CACHE_DIR)
                if (!cacheDir.exists()) return@withContext

                val currentTime = System.currentTimeMillis()
                cacheDir.listFiles()?.forEach { file ->
                    if (currentTime - file.lastModified() > maxAgeMs) {
                        file.delete()
                        Log.d(TAG, "Deleted old cache file: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean cache: ${e.message}")
            }
        }
    }

    /**
     * Полностью очищает кэш видео кадров и память
     */
    suspend fun clearCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // Очищаем in-memory кэш
                memoryCache.clear()

                // Очищаем disk кэш
                val cacheDir = File(context.cacheDir, CACHE_DIR)
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                    Log.d(TAG, "Cleared all cache")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear cache: ${e.message}")
            }
        }
    }
}




