package com.example.nkdsify.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.nkdsify.ui.components.utils.calculateCentroid
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

enum class SeekDirection {
    FORWARD, BACKWARD
}

@Composable
fun VideoPlayerPage(
    uri: Uri,
    isFullyVisible: Boolean,
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
    val lifecycleOwner = LocalLifecycleOwner.current
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

    // Player Lifecycle - Prepare the player only when the URI changes
    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    // Player Lifecycle - Control play/pause based on visibility
    LaunchedEffect(isFullyVisible) {
        exoPlayer.playWhenReady = isFullyVisible
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

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
