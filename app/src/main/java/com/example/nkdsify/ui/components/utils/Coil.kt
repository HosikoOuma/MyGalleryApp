package com.example.nkdsify.ui.components.utils

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.intercept.Interceptor
import coil.request.ImageResult
import com.example.nkdsify.ui.utils.EncryptedImageDecoder

@Composable
fun rememberCoilImageLoader(
    context: Context,
    gridState: LazyGridState
): ImageLoader {
    return remember(context, gridState) {
        ImageLoader.Builder(context)
            .components {
                add(EncryptedImageDecoder.Factory(context))
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                // ПРАВИЛЬНЫЙ СПОСОБ: Добавляем Interceptor здесь
                add(ScrollOptimizingInterceptor(gridState))
            }
            .build()
    }
}

private class ScrollOptimizingInterceptor(
    private val gridState: LazyGridState
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        // Логика остаётся той же: просто передаем запрос дальше.
        // Основной эффект достигается за счет того, что при быстрой прокрутке
        // запросы на изображения, которые уже не видны, будут отменены самим Coil.
        // Этот Interceptor - задел на будущее для более сложных оптимизаций.
        return chain.proceed(chain.request)
    }
}
