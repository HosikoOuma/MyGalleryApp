package com.example.nkdsify.ui.editor.video

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.nkdsify.R
import com.example.nkdsify.ui.editor.CropHandle
import kotlinx.coroutines.delay
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val videoUri by viewModel.videoUri.collectAsState()
    
    LaunchedEffect(Unit) {
        videoUri?.let { viewModel.loadVideo(context, it) }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var videoContainerSize by remember { mutableStateOf(IntSize.Zero) }
    
    LaunchedEffect(controlsVisible, viewModel.isCropMode) {
        if (controlsVisible && !viewModel.isCropMode) {
            delay(3000)
            controlsVisible = false
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = { 
                    Text(
                        stringResource(R.string.video_editor_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            viewModel.export(context) { if (it) onBack() }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.save_action), color = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            VideoBottomDesign(viewModel)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(viewModel.isCropMode) {
                    if (!viewModel.isCropMode) {
                        detectTapGestures { controlsVisible = !controlsVisible }
                    }
                }
        ) {
            videoUri?.let { _ ->
                // 1. Исходные пропорции видео
                val videoAspectRatio = if (viewModel.videoWidth > 0 && viewModel.videoHeight > 0) {
                    viewModel.videoWidth.toFloat() / viewModel.videoHeight.toFloat()
                } else 16 / 9f

                // 2. Пропорции контейнера (динамические)
                val currentAspectRatio = if (!viewModel.isCropMode) {
                    // После кропа подстраиваем под вырезанную область
                    (viewModel.videoWidth * viewModel.cropRect.width) /
                            (viewModel.videoHeight * viewModel.cropRect.height).coerceAtLeast(0.01f)
                } else {
                    // В режиме кропа — исходное видео
                    videoAspectRatio
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoAspectRatio)
                            .onGloballyPositioned { videoContainerSize = it.size }
                            .clipToBounds()
                            .background(Color.Black)
                    ) {
                        val isCropMode = viewModel.isCropMode
                        val crop = viewModel.cropRect

                        // ❗ ТОЛЬКО СДВИГ — БЕЗ SCALE
                        val offsetX = if (!isCropMode)
                            -crop.left * videoContainerSize.width
                        else 0f

                        val offsetY = if (!isCropMode)
                            -crop.top * videoContainerSize.height
                        else 0f

                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    player = viewModel.player
                                    // ✅ сохраняет пропорции, даёт чёрные полосы
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = offsetX
                                    translationY = offsetY
                                }
                        )

                        if (isCropMode) {
                            VideoCropOverlay(viewModel)
                        }
                    }

                    // ... кнопка Play/Pause ...


            if (!viewModel.isCropMode) {
                        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                            IconButton(
                                onClick = { viewModel.togglePlay() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.Black.copy(0.4f), CircleShape)
                            ) {
                                Icon(
                                    if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (viewModel.isExporting) {
                ExportProgressOverlay()
            }
        }
    }
}

@Composable
fun VideoBottomDesign(viewModel: VideoEditorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (viewModel.isCropMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.isCropMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                        ) {
                            Text(stringResource(R.string.dialog_cancel))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.isCropMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    }
                } else {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            viewModel.thumbnails.forEach { 
                                Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(), contentScale = ContentScale.Crop)
                            }
                        }
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (viewModel.videoDuration > 0) {
                                val progress = viewModel.currentPosition.toFloat() / viewModel.videoDuration.toFloat()
                                val xPos = progress * size.width
                                drawLine(
                                    color = Color.White,
                                    start = Offset(xPos, 0f),
                                    end = Offset(xPos, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        RangeSlider(
                            value = viewModel.trimStart.toFloat()..viewModel.trimEnd.toFloat(),
                            onValueChange = { range ->
                                viewModel.onTrimChanged(range.start.toLong(), range.endInclusive.toLong())
                            },
                            valueRange = 0f..viewModel.videoDuration.toFloat().coerceAtLeast(1f),
                            modifier = Modifier.fillMaxSize(),
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.Transparent, 
                                inactiveTrackColor = Color.Black.copy(alpha = 0.5f),
                                thumbColor = Color.White
                            )
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.isMuted = !viewModel.isMuted }) {
                            Icon(
                                if (viewModel.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp, 
                                null,
                                tint = if (viewModel.isMuted) Color.Red else Color.White
                            )
                        }
                        
                        Text(
                            "${(viewModel.trimEnd - viewModel.trimStart) / 1000}s",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        IconButton(onClick = { viewModel.isCropMode = true }) {
                            Icon(Icons.Default.Crop, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCropOverlay(viewModel: VideoEditorViewModel) {
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlaySize = it.size }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = overlaySize.width.toFloat()
                        val h = overlaySize.height.toFloat()
                        val r = viewModel.cropRect
                        val hit = 40.dp.toPx()
                        fun ds(x1: Float, y1: Float, x2: Float, y2: Float) =
                            (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)

                        activeHandle = when {
                            ds(
                                offset.x,
                                offset.y,
                                r.left * w,
                                r.top * h
                            ) < hit * hit -> CropHandle.TOP_LEFT

                            ds(
                                offset.x,
                                offset.y,
                                r.right * w,
                                r.top * h
                            ) < hit * hit -> CropHandle.TOP_RIGHT

                            ds(
                                offset.x,
                                offset.y,
                                r.left * w,
                                r.bottom * h
                            ) < hit * hit -> CropHandle.BOTTOM_LEFT

                            ds(
                                offset.x,
                                offset.y,
                                r.right * w,
                                r.bottom * h
                            ) < hit * hit -> CropHandle.BOTTOM_RIGHT

                            offset.x in (r.left * w)..(r.right * w) && offset.y in (r.top * h)..(r.bottom * h) -> CropHandle.CENTER
                            else -> CropHandle.NONE
                        }
                    },
                    onDrag = { change, drag ->
                        if (activeHandle == CropHandle.NONE) return@detectDragGestures
                        change.consume()
                        val w = overlaySize.width.toFloat()
                        val h = overlaySize.height.toFloat()
                        val dx = drag.x / w
                        val dy = drag.y / h
                        val r = viewModel.cropRect

                        viewModel.cropRect = when (activeHandle) {
                            CropHandle.TOP_LEFT -> Rect(
                                (r.left + dx).coerceIn(0f, r.right - 0.1f),
                                (r.top + dy).coerceIn(0f, r.bottom - 0.1f),
                                r.right,
                                r.bottom
                            )

                            CropHandle.TOP_RIGHT -> Rect(
                                r.left,
                                (r.top + dy).coerceIn(0f, r.bottom - 0.1f),
                                (r.right + dx).coerceIn(r.left + 0.1f, 1f),
                                r.bottom
                            )

                            CropHandle.BOTTOM_LEFT -> Rect(
                                (r.left + dx).coerceIn(
                                    0f,
                                    r.right - 0.1f
                                ), r.top, r.right, (r.bottom + dy).coerceIn(r.top + 0.1f, 1f)
                            )

                            CropHandle.BOTTOM_RIGHT -> Rect(
                                r.left,
                                r.top,
                                (r.right + dx).coerceIn(r.left + 0.1f, 1f),
                                (r.bottom + dy).coerceIn(r.top + 0.1f, 1f)
                            )

                            CropHandle.CENTER -> {
                                val nl = (r.left + dx).coerceIn(0f, 1f - r.width)
                                val nt = (r.top + dy).coerceIn(0f, 1f - r.height)
                                Rect(nl, nt, nl + r.width, nt + r.height)
                            }

                            else -> r
                        }
                    },
                    onDragEnd = { activeHandle = CropHandle.NONE }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = viewModel.cropRect
            val rl = r.left * size.width
            val rt = r.top * size.height
            val rr = r.right * size.width
            val rb = r.bottom * size.height

            drawRect(color = Color.Black.copy(0.6f))
            clipRect(rl, rt, rr, rb, ClipOp.Difference) {}
            drawRect(color = Color.White, topLeft = Offset(rl, rt), size = Size(rr-rl, rb-rt), style = Stroke(2.dp.toPx()))
            
            val s = 20.dp.toPx(); val t = 4.dp.toPx()
            drawRect(Color.White, Offset(rl-t/2, rt-t/2), Size(s, t))
            drawRect(Color.White, Offset(rl-t/2, rt-t/2), Size(t, s))
            drawRect(Color.White, Offset(rr-s+t/2, rt-t/2), Size(s, t))
            drawRect(Color.White, Offset(rr-t/2, rt-t/2), Size(t, s))
            drawRect(Color.White, Offset(rl-t/2, rb-t/2), Size(s, t))
            drawRect(Color.White, Offset(rl-t/2, rb-s+t/2), Size(t, s))
            drawRect(Color.White, Offset(rr-s+t/2, rb-t/2), Size(s, t))
            drawRect(Color.White, Offset(rr-t/2, rb-s+t/2), Size(t, s))
        }
    }
}

@Composable
fun ExportProgressOverlay() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(text = stringResource(R.string.editor_saving_video), style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}
