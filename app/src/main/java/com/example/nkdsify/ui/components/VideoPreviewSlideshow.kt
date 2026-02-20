package com.example.nkdsify.ui.components
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.utils.VideoFrameExtractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
/**
 * Компонент для отображения видео превью с анимированным слайд-шоу
 * Показывает последовательно: начальный кадр → средний → конечный
 */
@Composable
fun VideoPreviewWithSlideshow(
    modifier: Modifier = Modifier,
    item: MediaItem,
    imageLoader: ImageLoader,
    intervalMs: Long = 800L,
    contentScale: ContentScale = ContentScale.Crop,
    useLowQuality: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var frameIndex by remember { mutableStateOf(0) }
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(item.uri, useLowQuality) {
        scope.launch {
            isLoading = true
            try {
                val extractedFrames = VideoFrameExtractor.getFramesWithCache(context, item.uri, useLowQuality)
                if (extractedFrames != null) {
                    val frameList = listOfNotNull(
                        extractedFrames[VideoFrameExtractor.FrameType.START],
                        extractedFrames[VideoFrameExtractor.FrameType.MIDDLE],
                        extractedFrames[VideoFrameExtractor.FrameType.END]
                    )
                    if (frameList.isNotEmpty()) {
                        frames = frameList
                        currentFrameBitmap = frameList[0]
                        frameIndex = 0
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }
    LaunchedEffect(frames) {
        if (frames.isNotEmpty()) {
            while (true) {
                delay(intervalMs)
                frameIndex = (frameIndex + 1) % frames.size
                currentFrameBitmap = frames[frameIndex]
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        if (currentFrameBitmap != null) {
            Image(
                bitmap = currentFrameBitmap!!.asImageBitmap(),
                contentDescription = "Video frame",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!isLoading) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
