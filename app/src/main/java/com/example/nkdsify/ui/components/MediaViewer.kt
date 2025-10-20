package com.example.nkdsify.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.nkdsify.data.MediaDetails
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.utils.ExternalMediaErrorDialog
import com.example.nkdsify.ui.utils.MediaDetailsDialog
import com.example.nkdsify.ui.utils.getMediaDetails

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    favorites: List<Uri>,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader,
    onDeletePermanently: (List<Uri>) -> Unit,
    onShowTagDialog: (Uri) -> Unit,
    onToggleFavorite: (Uri) -> Unit,
    isExternal: Boolean = false,
    isMuteVideoByDefault: Boolean
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showExternalMediaError by remember { mutableStateOf(false) }
    var mediaDetails by remember { mutableStateOf<MediaDetails?>(null) }
    var isMuted by remember(pagerState.currentPage) { mutableStateOf(isMuteVideoByDefault) }

    if (showDetailsDialog && mediaDetails != null) {
        MediaDetailsDialog(details = mediaDetails!!, onDismiss = { showDetailsDialog = false })
    }

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
                ZoomableImage(uri = item.uri, imageLoader = imageLoader)
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
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))

            val currentPage = pagerState.currentPage
            if (currentPage >= 0 && currentPage < items.size) {
                val currentItem = items[currentPage]

                if (currentItem.isVideo) {
                    IconButton(onClick = { isMuted = !isMuted }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute/Unmute",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = {
                    if (isExternal) {
                        showExternalMediaError = true
                    } else {
                        onShowTagDialog(currentItem.uri)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags", tint = Color.White)
                }

                IconButton(onClick = {
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
                    if (isExternal) {
                        showExternalMediaError = true
                    } else {
                        onDeletePermanently(listOf(currentItem.uri))
                    }
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                }
                IconButton(onClick = {
                    if (isExternal) {
                        showExternalMediaError = true
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    mediaDetails = getMediaDetails(context, currentItem.uri)
                    if (mediaDetails != null) {
                        showDetailsDialog = true
                    }
                }) {
                    Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(uri: Uri, imageLoader: ImageLoader) {
    var scale by rememberSaveable { mutableStateOf(1f) }
    var offsetX by rememberSaveable { mutableStateOf(0f) }
    var offsetY by rememberSaveable { mutableStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var tapCount by remember { mutableStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }

    LaunchedEffect(key1 = uri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
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
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val mediaPlayer = MediaPlayer.create(context, R.raw.pii)
                        mediaPlayer.setOnCompletionListener { it.release() }
                        mediaPlayer.start()
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    //awaitFirstDown()
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

                            if (zoom != 1f || pan != androidx.compose.ui.geometry.Offset.Zero) {
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
