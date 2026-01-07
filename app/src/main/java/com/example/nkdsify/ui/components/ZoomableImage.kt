package com.example.nkdsify.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.nkdsify.R
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ZoomableImage(
    uri: Uri,
    imageLoader: ImageLoader,
    zoomType: ZoomType,
    isVibrationEnabled: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uri) {
        scale.snapTo(1f)
        offset.snapTo(Offset.Zero)
    }

    // Вспомогательная функция для инкремента счетчика тапов (для пасхалки)
    val updateTapCount: (Int) -> Unit = { count ->
        val now = System.currentTimeMillis()
        if (now - lastTap > 500) tapCount = count else tapCount += count
        lastTap = now
        if (tapCount >= 5) {
            tapCount = 0
            if (isVibrationEnabled) performVibration(context)
            val mediaPlayer = MediaPlayer.create(context, R.raw.pii)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // БЛОК 1: ТАПЫ (Одиночный и Двойной)
            .pointerInput(uri, zoomType) {
                detectTapGestures(
                    onTap = {
                        onToggleControls()
                        updateTapCount(1)
                    },
                    onDoubleTap = { tapOffset ->
                        updateTapCount(2)
                        if (zoomType == ZoomType.DOUBLE_TAP) {
                            if (isVibrationEnabled) performVibration(context)
                            
                            val isZoomed = scale.value > 1.05f
                            val targetScale = if (isZoomed) 1f else 3f
                            val targetOffset = if (targetScale == 1f) {
                                Offset.Zero
                            } else {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                (center - tapOffset) * (targetScale - 1f)
                            }

                            scope.launch {
                                coroutineScope {
                                    launch { scale.animateTo(targetScale) }
                                    launch { offset.animateTo(targetOffset) }
                                }
                            }
                        }
                    }
                )
            }
            // БЛОК 2: ТРАНСФОРМАЦИИ (Зум щипком и Панорамирование)
            .pointerInput(uri) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrentPosition = true)

                        if (zoom != 1f || pan != Offset.Zero) {
                            val newScale = (scale.value * zoom).coerceIn(1f, 5f)
                            val newOffset = if (newScale > 1f) {
                                val oldOffset = offset.value
                                val extraOffset = pan + (centroid - Offset(size.width / 2f, size.height / 2f)) * (1 - zoom)
                                val maxOffsetX = (size.width * (newScale - 1)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1)) / 2f
                                
                                // Поглощаем события только если реально зумируем
                                event.changes.forEach { it.consume() }

                                Offset(
                                    (oldOffset.x + extraOffset.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    (oldOffset.y + extraOffset.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                Offset.Zero
                            }

                            scope.launch {
                                scale.snapTo(newScale)
                                offset.snapTo(newOffset)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current).data(uri).size(Size.ORIGINAL).build(),
                imageLoader = imageLoader
            ),
            contentDescription = "Full screen image",
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.value.x
                    translationY = offset.value.y
                }
        )
    }
}

private fun PointerEvent.calculateCentroid(useCurrentPosition: Boolean = false): Offset {
    var sum = Offset.Zero
    var count = 0
    changes.forEach {
        if (it.pressed) {
            sum += if (useCurrentPosition) it.position else it.previousPosition
            count++
        }
    }
    return if (count == 0) Offset.Zero else sum / count.toFloat()
}