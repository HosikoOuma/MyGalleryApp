package com.example.nkdsify.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.animate
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch

@Composable
fun ZoomableImage(
    uri: Uri,
    imageLoader: ImageLoader,
    zoomType: ZoomType,
    isVibrationEnabled: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }

    LaunchedEffect(key1 = uri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val gestureModifier = if (zoomType == ZoomType.DOUBLE_TAP) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onToggleControls() },
                onDoubleTap = { tapOffset ->
                    if (isVibrationEnabled) performVibration(context)
                    coroutineScope.launch {
                        val startScale = scale
                        val startOffsetX = offsetX
                        val startOffsetY = offsetY

                        val (targetScale, targetOffsetX, targetOffsetY) = if (startScale > 1f) {
                            Triple(1f, 0f, 0f)
                        } else {
                            val targetS = 3f
                            val center = Offset(viewSize.width / 2f, viewSize.height / 2f)
                            val targetX = (tapOffset.x - center.x) * (1 - targetS)
                            val targetY = (tapOffset.y - center.y) * (1 - targetS)
                            Triple(targetS, targetX, targetY)
                        }

                        animate(0f, 1f) { fraction, _ ->
                            scale = startScale + (targetScale - startScale) * fraction
                            offsetX = startOffsetX + (targetOffsetX - startOffsetX) * fraction
                            offsetY = startOffsetY + (targetOffsetY - startOffsetY) * fraction
                        }
                    }
                }
            )
        }
    } else {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                val now = System.currentTimeMillis()
                if (now - lastTap > 500) { // More than 500ms passed, it's a single tap
                    onToggleControls()
                    tapCount = 1
                } else { // Less than 500ms, it's a multi-tap sequence
                    tapCount++
                }
                lastTap = now

                if (tapCount == 5) {
                    tapCount = 0
                    if (isVibrationEnabled) performVibration(context)
                    val mediaPlayer = MediaPlayer.create(context, R.raw.pii)
                    mediaPlayer.setOnCompletionListener { it.release() }
                    mediaPlayer.start()
                }
            })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(gestureModifier)
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrentPosition = true)

                        val newScale = (scale * zoom).coerceIn(1f, 5f)

                        if (newScale <= 1f) {
                            offsetX = 0f
                            offsetY = 0f
                            scale = 1f
                        } else {
                            val newOffsetX =
                                offsetX + pan.x + (centroid.x - viewSize.width / 2) * (1 - zoom)
                            val newOffsetY =
                                offsetY + pan.y + (centroid.y - viewSize.height / 2) * (1 - zoom)

                            val maxOffsetX = (viewSize.width * (newScale - 1)) / 2f
                            val maxOffsetY = (viewSize.height * (newScale - 1)) / 2f

                            if (zoom != 1f || pan != Offset.Zero) {
                                event.changes.forEach { it.consume() }
                            }

                            scale = newScale
                            offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
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
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
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