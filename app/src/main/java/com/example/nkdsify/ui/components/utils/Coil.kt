package com.example.nkdsify.ui.components.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.nkdsify.ui.utils.EncryptedImageDecoder
import kotlinx.coroutines.Dispatchers


// Создаем синглтон для ImageLoader, чтобы обеспечить общий кэш для всего приложения
object AppImageLoader {
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: buildImageLoader(context).also { instance = it }
        }
    }

    private fun buildImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(EncryptedImageDecoder.Factory(context))
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            // Явно настраиваем кэширование для надежности
            .memoryCache {
                MemoryCache.Builder(context)
                    // Используем 25% доступной памяти для кэша в памяти
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    // Указываем папку для кэша на диске
                    .directory(context.cacheDir.resolve("image_cache"))
                    // Устанавливаем максимальный размер в 256MB
                    .maxSizeBytes(256 * 1024 * 1024)
                    .build()
            }
            .dispatcher(Dispatchers.IO)
            .build()
    }
}

@Composable
fun rememberCoilImageLoader(
    context: Context,
): ImageLoader {
    // Теперь мы просто помним единственный экземпляр ImageLoader
    return remember(context) {
        AppImageLoader.get(context)
    }
}
