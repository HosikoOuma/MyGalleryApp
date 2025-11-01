package com.example.nkdsify.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.ExternalMediaErrorDialog
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    favorites: List<Uri>,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader,
    onDelete: (List<Uri>) -> Unit,
    onShowTagDialog: (Uri) -> Unit,
    onToggleFavorite: (Uri) -> Unit,
    onShowDetails: (Uri) -> Unit,
    isExternal: Boolean = false,
    isMuteVideoByDefault: Boolean,
    zoomType: ZoomType
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    val context = LocalContext.current
    val isVibrationEnabled = remember { SettingsRepository.isVibrationEnabled(context) }
    var showExternalMediaError by remember { mutableStateOf(false) }
    var isMuted by remember(pagerState.currentPage) { mutableStateOf(isMuteVideoByDefault) }

    if (showExternalMediaError) {
        ExternalMediaErrorDialog(onDismiss = { showExternalMediaError = false })
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { items[it].uri }) { page ->
            val item = items[page]
            val isVisible by remember { derivedStateOf { pagerState.currentPage == page } }
            if (item.isVideo) {
                VideoPlayerPage(uri = item.uri, isVisible = isVisible, isMuted = isMuted)
            } else {
                ZoomableImage(uri = item.uri, imageLoader = imageLoader, zoomType = zoomType, isVibrationEnabled = isVibrationEnabled)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                onDismiss()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))

            val currentPage = pagerState.currentPage
            if (currentPage >= 0 && currentPage < items.size) {
                val currentItem = items[currentPage]

                if (currentItem.isVideo) {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        isMuted = !isMuted
                    }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute/Unmute",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    if (isExternal) {
                        showExternalMediaError = true
                    } else {
                        onShowTagDialog(currentItem.uri)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags", tint = Color.White)
                }

                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                        type = if (currentItem.isVideo) "video/*" else "image/*"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                }
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    if (isExternal) {
                        showExternalMediaError = true
                    } else {
                        onDelete(listOf(currentItem.uri))
                    }
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                }
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    if (isExternal) {
                        showExternalMediaError = true
                    } else {
                        onToggleFavorite(currentItem.uri)
                    }
                }) {
                    Icon(
                        imageVector = if (favorites.contains(currentItem.uri)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (favorites.contains(currentItem.uri)) Color.Red else Color.White
                    )
                }

                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onShowDetails(currentItem.uri)
                }) {
                    Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(uri: Uri, imageLoader: ImageLoader, zoomType: ZoomType, isVibrationEnabled: Boolean) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }

    LaunchedEffect(key1 = uri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val doubleTapModifier = if (zoomType == ZoomType.DOUBLE_TAP) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
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
                            val center = Offset(size.width / 2f, size.height / 2f)
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
                if (now - lastTap < 500) { // 500ms between taps
                    tapCount++
                } else {
                    tapCount = 1
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
            .then(doubleTapModifier)
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        val newScale = (scale * zoom).coerceIn(1f, 5f)

                        if (newScale <= 1f) {
                            offsetX = 0f
                            offsetY = 0f
                            scale = 1f
                        } else {
                            val maxOffsetX = (size.width * (newScale - 1)) / 2f
                            val maxOffsetY = (size.height * (newScale - 1)) / 2f

                            val newOffsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newOffsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)

                            if (zoom != 1f || pan != Offset.Zero) {
                                event.changes.forEach { it.consume() }
                            }

                            scale = newScale
                            offsetX = newOffsetX
                            offsetY = newOffsetY
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
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
                .onSizeChanged { size = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

@Composable
fun VideoPlayerPage(uri: Uri, isVisible: Boolean, isMuted: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(uri, isVisible) {
        if (isVisible) {
            exoPlayer.setMediaItem(Media3Item.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = true } },
            update = { playerView -> playerView.player = exoPlayer },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {}
        )
    }
}
