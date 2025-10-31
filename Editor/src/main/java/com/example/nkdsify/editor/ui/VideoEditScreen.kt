package com.example.nkdsify.editor.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditScreen(
    uri: Uri,
    onSave: (Uri, Long, Long) -> Unit
) {
    val context = LocalContext.current
    var startPositionMs by remember { mutableStateOf(0L) }
    var endPositionMs by remember { mutableStateOf(0L) }
    var videoDurationMs by remember { mutableStateOf(0L) }
    var sliderPositions by remember { mutableStateOf(0f..1f) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    videoDurationMs = exoPlayer.duration
                    endPositionMs = exoPlayer.duration
                    sliderPositions = 0f..videoDurationMs.toFloat()
                }
            }
        }
        exoPlayer.addListener(listener)
    }
    
    // Loop playback within the selected range
    LaunchedEffect(exoPlayer, startPositionMs, endPositionMs) {
        while (true) {
            if (exoPlayer.isPlaying && exoPlayer.currentPosition >= endPositionMs) {
                exoPlayer.seekTo(startPositionMs)
            }
            delay(100)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (videoDurationMs > 0) {
                 RangeSlider(
                    value = startPositionMs.toFloat()..endPositionMs.toFloat(),
                    onValueChange = { newRange ->
                        startPositionMs = newRange.start.toLong()
                        endPositionMs = newRange.endInclusive.toLong()
                        if (!exoPlayer.isPlaying) {
                           exoPlayer.seekTo(startPositionMs)
                        }
                    },
                    valueRange = 0f..videoDurationMs.toFloat(),
                    onValueChangeFinished = {
                        exoPlayer.seekTo(startPositionMs)
                    }
                )
            }
            
            Button(onClick = { 
                exoPlayer.pause()
                onSave(uri, startPositionMs, endPositionMs) 
            }) {
                Text("Save Trimmed Video")
            }
        }
    }
}
