package com.example.nkdsify.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.nkdsify.ui.utils.ViewHistoryRepository
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.CryptoUtils
import com.example.nkdsify.ui.utils.ExternalMediaErrorDialog
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    favorites: List<Uri>,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader,
    onDelete: (List<Uri>) -> Unit,
    onRestore: (List<Uri>) -> Unit,
    onShowTagDialog: (Uri) -> Unit,
    onToggleFavorite: (Uri) -> Unit,
    onShowDetails: (Uri) -> Unit,
    isExternal: Boolean = false,
    isTrashMode: Boolean,
    isSecretMode: Boolean = false, // <-- ДОБАВЬТЕ ЭТО
    isMuteVideoByDefault: Boolean,
    zoomType: ZoomType,
    isLoopVideoEnabled: Boolean,
    isSwipeToDismissEnabled: Boolean
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    val context = LocalContext.current
    var tempFileUri by remember { mutableStateOf<Uri?>(null) }
    if (isSecretMode) {
        DisposableEffect(Unit) {
            onDispose {
                tempFileUri?.path?.let {
                    try {
                        File(it).delete()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            // Clean up previous temp file
            tempFileUri?.path?.let { File(it).delete() }
            tempFileUri = null

            // Decrypt new item
            val item = items[pagerState.currentPage]
            withContext(Dispatchers.IO) {
                val tempFile = File.createTempFile("decrypted_", item.name.substringAfterLast('.'), context.cacheDir)
                try {
                    File(item.uri.path!!).inputStream().use { input ->
                        tempFile.outputStream().use { output ->
                            CryptoUtils.decrypt(input, output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        tempFileUri = Uri.fromFile(tempFile)
                    }
                } catch (e: Exception) {
                    tempFile.delete()
                    // Handle error, maybe show a toast
                }
            }
        }
    }
    val isVibrationEnabled = remember { SettingsRepository.isVibrationEnabled(context) }
    var showExternalMediaError by remember { mutableStateOf(false) }
    var isMuted by remember(pagerState.currentPage) { mutableStateOf(isMuteVideoByDefault) }

    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) {
            onDismiss()
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (!isExternal && !isTrashMode && !isSecretMode) {
            items.getOrNull(pagerState.currentPage)?.let { item ->
                ViewHistoryRepository.addToHistory(context, item.uri)
            }
        }
    }

    // --- Unified Controls Visibility State ---
    var controlsVisible by remember { mutableStateOf(true) }
    val toggleControls = { controlsVisible = !controlsVisible }

    // Auto-hide controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    if (showExternalMediaError) {
        ExternalMediaErrorDialog(onDismiss = { showExternalMediaError = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { items[it].uri },
            userScrollEnabled = isSwipeToDismissEnabled        ) { page ->
            val item = items[page]
            val isVisible by remember { derivedStateOf { pagerState.currentPage == page } }

            if (isSecretMode) {
                if (isVisible && tempFileUri != null) {
                    if (item.isVideo) {
                        VideoPlayerPage(
                            uri = tempFileUri!!,
                            isVisible = isVisible,
                            isMuted = isMuted,
                            controlsVisible = controlsVisible,
                            onToggleControls = toggleControls,
                            onMuteClick = { isMuted = !isMuted },
                            isLoopVideoEnabled = isLoopVideoEnabled
                        )
                    } else {
                        ZoomableImage(
                            uri = tempFileUri!!,
                            imageLoader = imageLoader,
                            zoomType = zoomType,
                            isVibrationEnabled = isVibrationEnabled,
                            onToggleControls = toggleControls
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(id = R.string.decrypting_file), color = Color.White)
                        }
                    }
                }
            } else { // Original logic for non-secret items
                if (item.isVideo) {
                    VideoPlayerPage(
                        uri = item.uri,
                        isVisible = isVisible,
                        isMuted = isMuted,
                        controlsVisible = controlsVisible,
                        onToggleControls = toggleControls,
                        onMuteClick = { isMuted = !isMuted },
                        isLoopVideoEnabled = isLoopVideoEnabled
                    )
                } else {
                    ZoomableImage(
                        uri = item.uri,
                        imageLoader = imageLoader,
                        zoomType = zoomType,
                        isVibrationEnabled = isVibrationEnabled,
                        onToggleControls = toggleControls
                    )
                }
            }
        }

        // --- Unified Top Control Bar ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), shape = CircleShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPage = pagerState.currentPage
                val currentItem = items.getOrNull(currentPage)

                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onDismiss()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Spacer(Modifier.weight(1f))

                if (currentItem != null) {
                    when {
                        isSecretMode -> {
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                onRestore(listOf(currentItem.uri))
                                //onDismiss()
                            }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                onDelete(listOf(currentItem.uri))
                                //onDismiss()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                        isTrashMode -> {
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                onRestore(listOf(currentItem.uri))
                            }) {
                                Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Restore", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                onDelete(listOf(currentItem.uri))
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                        else -> { // Normal mode
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
                            val coroutineScope = rememberCoroutineScope()
                            val scale = remember { Animatable(1f) }

                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                if (isExternal) {
                                    showExternalMediaError = true
                                } else {
                                    // Toggle favorite state first
                                    onToggleFavorite(currentItem.uri)
                                    // Then run the animation
                                    coroutineScope.launch {
                                        scale.animateTo(
                                            targetValue = 1.3f,
                                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
                                        )
                                        scale.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring()
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    modifier = Modifier.scale(scale.value),
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
        }
    }
}

@Composable
fun ZoomableImage(
    uri: Uri,
    imageLoader: ImageLoader,
    zoomType: ZoomType,
    isVibrationEnabled: Boolean,
    onToggleControls: () -> Unit
) {
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
                                offsetX + pan.x + (centroid.x - size.width / 2) * (1 - zoom)
                            val newOffsetY =
                                offsetY + pan.y + (centroid.y - size.height / 2) * (1 - zoom)

                            val maxOffsetX = (size.width * (newScale - 1)) / 2f
                            val maxOffsetY = (size.height * (newScale - 1)) / 2f

                            if (zoom != 1f || pan != Offset.Zero) {
                                event.changes.forEach { it.consume() }
                            }

                            scale = newScale
                            offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
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

enum class SeekDirection {
    FORWARD, BACKWARD
}

@Composable
fun VideoPlayerPage(
    uri: Uri,
    isVisible: Boolean,
    isMuted: Boolean,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onMuteClick: () -> Unit,
    isLoopVideoEnabled: Boolean
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // --- State Management ---
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var playbackState by remember { mutableIntStateOf(exoPlayer.playbackState) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }

    // --- New Feature States ---
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf<SeekDirection?>(null) }
    var isSeeking by remember { mutableStateOf(false) }

    // --- Zoom State ---
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // --- Lifecycle State ---
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var wasPlayingBeforePause by rememberSaveable { mutableStateOf(false) }


    // --- Effects ---
    // Reset states when uri changes
    LaunchedEffect(key1 = uri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        speed = 1f
        exoPlayer.setPlaybackSpeed(1f)
    }
    // Seek feedback visibility
    LaunchedEffect(seekDirection) {
        if (seekDirection != null) {
            delay(800L)
            seekDirection = null
        }
    }

    // Player Lifecycle
    LaunchedEffect(uri, isVisible) {
        if (isVisible) {
            exoPlayer.setMediaItem(Media3Item.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.pause()
        }
    }

    // App Lifecycle Observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayer.isPlaying) {
                        wasPlayingBeforePause = true
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (wasPlayingBeforePause) {
                        exoPlayer.play()
                        wasPlayingBeforePause = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(isLoopVideoEnabled) {
        exoPlayer.repeatMode = if (isLoopVideoEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Player state listener and position updater
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                totalDuration = player.duration.coerceAtLeast(0)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Coroutine to update playback position
    LaunchedEffect(isPlaying, isSeeking) {
        if (!isSeeking) {
            while(isPlaying) {
                playbackPosition = exoPlayer.currentPosition.coerceAtLeast(0)
                delay(500)
            }
        }
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        seekDirection = if (offset.x > size.width / 2) {
                            exoPlayer.seekTo(
                                (exoPlayer.currentPosition + 10000).coerceAtMost(
                                    totalDuration
                                )
                            )
                            SeekDirection.FORWARD
                        } else {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                            SeekDirection.BACKWARD
                        }
                    }
                )
            }
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
                                offsetX + pan.x + (centroid.x - size.width / 2) * (1 - zoom)
                            val newOffsetY =
                                offsetY + pan.y + (centroid.y - size.height / 2) * (1 - zoom)

                            val maxOffsetX = (size.width * (newScale - 1)) / 2f
                            val maxOffsetY = (size.height * (newScale - 1)) / 2f

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
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    player = exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )

        // Scrim for better controls visibility
        AnimatedVisibility(
            visible = controlsVisible || seekDirection != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f))) {}
        }

        val seekAnimSide = if (seekDirection == SeekDirection.FORWARD) Alignment.CenterEnd else Alignment.CenterStart
        AnimatedVisibility(
            visible = seekDirection != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(seekAnimSide)
        ) {
            Icon(
                imageVector = if (seekDirection == SeekDirection.FORWARD) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = "Seek",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(64.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        // Custom Controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (playbackState == Player.STATE_BUFFERING) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            if (playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                            } else {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        val icon = if (isPlaying && playbackState != Player.STATE_ENDED) Icons.Filled.Pause else Icons.Filled.PlayArrow
                        Icon(
                            imageVector = icon,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize(0.7f)
                        )
                    }

                    Slider(
                        value = playbackPosition.toFloat(),
                        onValueChange = { newPosition ->
                            isSeeking = true
                            playbackPosition = newPosition.toLong()
                            exoPlayer.seekTo(newPosition.toLong())
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                        },
                        valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = formatDuration(playbackPosition), color = Color.White)

                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(onClick = onMuteClick) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute/Unmute",
                                    tint = Color.White
                                )
                            }
                            Box {
                                IconButton(onClick = { showSpeedMenu = true }) {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = "Playback Speed", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speedValue ->
                                        DropdownMenuItem(
                                            text = { Text(text = "${speedValue}x") },
                                            onClick = {
                                                speed = speedValue
                                                exoPlayer.setPlaybackSpeed(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(text = formatDuration(totalDuration), color = Color.White)
                    }
                }
            }
        }
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

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
