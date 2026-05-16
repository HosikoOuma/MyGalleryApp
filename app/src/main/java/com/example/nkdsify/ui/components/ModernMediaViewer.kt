package com.example.nkdsify.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.ViewerControlsPosition
import com.example.nkdsify.ui.components.utils.rememberCoilImageLoader
import com.example.nkdsify.ui.utils.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun ModernMediaViewer(
    myAppState: MyAppState,
    state: MediaViewerState,
    isSecretMode: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val items = state.items
    val pagerState = rememberPagerState(initialPage = state.startIndex, pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    
    var decryptedUri by remember { mutableStateOf<Uri?>(null) }
    var isDecrypting by remember { mutableStateOf(false) }

    val view = LocalView.current
    val window = (view.context as Activity).window

    // Общий ExoPlayer
    val exoPlayer = remember {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(5000, 10000, 500, 1000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // Управление источником данных
    LaunchedEffect(pagerState.currentPage) {
        val item = items[pagerState.currentPage]
        if (item.isVideo) {
            val mediaItem = Media3MediaItem.fromUri(item.uri)
            if (isSecretMode) {
                val dataSourceFactory = AesDataSourceFactory()
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                exoPlayer.setMediaSource(mediaSource)
            } else {
                exoPlayer.setMediaItem(mediaItem)
            }
            exoPlayer.prepare()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    // Сохранение и восстановление состояния Appearance (цвета иконок) статус-бара
    DisposableEffect(view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val originalLightStatusBars = controller?.isAppearanceLightStatusBars ?: false
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = originalLightStatusBars
        }
    }

    // Дешифровка для секретного режима
    if (isSecretMode) {
        LaunchedEffect(pagerState.currentPage) {
            val item = items[pagerState.currentPage]
            if (item.isVideo) { isDecrypting = false; decryptedUri = null; return@LaunchedEffect }
            isDecrypting = true
            decryptedUri = null
            val decryptedFile = withContext(Dispatchers.IO) {
                val cacheDir = context.cacheDir.resolve("decrypted_media")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val originalFile = File(item.uri.path!!)
                val cachedFile = File(cacheDir, originalFile.name)
                if (cachedFile.exists() && cachedFile.length() > 0) cachedFile else {
                    try {
                        originalFile.inputStream().use { input ->
                            cachedFile.outputStream().use { output -> CryptoUtils.decrypt(input, output) }
                        }
                        cachedFile
                    } catch (e: Exception) { cachedFile.delete(); null }
                }
            }
            decryptedUri = decryptedFile?.let { Uri.fromFile(it) }
            isDecrypting = false
        }
    }

    val isVibrationEnabled = remember { SettingsRepository.isVibrationEnabled(context) }
    var isMuted by remember(pagerState.currentPage) { mutableStateOf(myAppState.isMuteVideoByDefault) }
    var controlsVisible by remember { mutableStateOf(true) }

    // Авто-скрытие контролов
    LaunchedEffect(controlsVisible, pagerState.currentPage) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Свайп вниз для закрытия
    var offsetY by remember { mutableStateOf(0f) }
    val alpha by animateFloatAsState(targetValue = (1f - (offsetY / 600f)).coerceIn(0f, 1f))

    val navigateBack = {
        myAppState.currentScreen = myAppState.previousScreen
    }

    BackHandler { navigateBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (offsetY > 300f) {
                            navigateBack()
                        } else {
                            offsetY = 0f
                        }
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .graphicsLayer {
                    val scale = (1f - (offsetY / 2000f)).coerceIn(0.8f, 1f)
                    scaleX = scale
                    scaleY = scale
                },
            key = { items[it].uri },
            beyondViewportPageCount = 0
        ) { page ->
            val item = items[page]
            val isFullyVisible = !pagerState.isScrollInProgress && pagerState.currentPage == page
            
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            rememberSharedContentState(key = "item_${item.uri}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSecretMode && !item.isVideo) {
                        if (isDecrypting) {
                            CircularProgressIndicator(color = Color.White)
                        } else if (decryptedUri != null) {
                            ZoomableImage(
                                uri = decryptedUri!!,
                                imageLoader = myAppState.imageLoader ?: rememberCoilImageLoader(context),
                                zoomType = myAppState.selectedZoomType,
                                isVibrationEnabled = isVibrationEnabled,
                                onToggleControls = { controlsVisible = !controlsVisible }
                            )
                        }
                    } else if (item.isVideo) {
                        VideoPlayerPage(
                            uri = item.uri,
                            exoPlayer = exoPlayer,
                            isFullyVisible = isFullyVisible,
                            isCurrentPage = pagerState.currentPage == page,
                            isMuted = isMuted,
                            controlsVisible = controlsVisible,
                            onToggleControls = { controlsVisible = !controlsVisible },
                            onMuteClick = { isMuted = !isMuted },
                            isLoopVideoEnabled = myAppState.isLoopVideoEnabled,
                            isSecretMode = isSecretMode,
                            bottomPadding = if (myAppState.viewerControlsPosition == ViewerControlsPosition.BOTTOM) 80.dp else 0.dp
                        )
                    } else {
                        ZoomableImage(
                            uri = item.uri,
                            imageLoader = myAppState.imageLoader ?: rememberCoilImageLoader(context),
                            zoomType = myAppState.selectedZoomType,
                            isVibrationEnabled = isVibrationEnabled,
                            onToggleControls = { controlsVisible = !controlsVisible }
                        )
                    }
                }
            }
        }

        // Кнопки управления (Glassmorphism)
        val currentItem = items.getOrNull(pagerState.currentPage)
        
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ViewerTopBar(
                onBack = { navigateBack() },
                title = currentItem?.name ?: ""
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ViewerBottomBar(
                myAppState = myAppState,
                item = currentItem,
                isSecretMode = isSecretMode,
                isVibrationEnabled = isVibrationEnabled
            )
        }
    }
}

@Composable
fun ViewerTopBar(onBack: () -> Unit, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            maxLines = 1
        )
    }
}

@Composable
fun ViewerBottomBar(
    myAppState: MyAppState,
    item: MediaItem?,
    isSecretMode: Boolean,
    isVibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val favoritesList = myAppState.favoritesList
    
    Row(
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (item == null) return@Row

        if (isSecretMode) {
            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                myAppState.itemsToRestoreFromSecret = listOf(item.uri).toImmutableList()
                myAppState.showConfirmRestoreFromSecretDialog = true
            }) { Icon(Icons.Default.Restore, null, tint = Color.White) }

            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                myAppState.itemsToDeleteFromSecret = listOf(item.uri).toImmutableList()
                myAppState.showConfirmDeleteFromSecretDialog = true
            }) { Icon(Icons.Default.Delete, null, tint = Color.White) }
        } else {
            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                myAppState.showTagDialog = item.uri
            }) { Icon(Icons.AutoMirrored.Filled.Label, null, tint = Color.White) }

            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, item.uri)
                    type = context.contentResolver.getType(item.uri) ?: if (item.isVideo) "video/*" else "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }) { Icon(Icons.Default.Share, null, tint = Color.White) }

            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                myAppState.itemsToTrash = listOf(item.uri).toImmutableList()
                myAppState.showConfirmTrashDialog = true
            }) { Icon(Icons.Default.Delete, null, tint = Color.White) }

            val isFavorite = favoritesList.contains(item.absolutePath)
            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                coroutineScope.launch(Dispatchers.IO) {
                    if (isFavorite) favoritesList.remove(item.absolutePath) else favoritesList.add(item.absolutePath)
                }
            }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.White
                )
            }

            IconButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                myAppState.showDetailsDialog = item.uri
            }) { Icon(Icons.Default.Info, null, tint = Color.White) }
        }
    }
}
