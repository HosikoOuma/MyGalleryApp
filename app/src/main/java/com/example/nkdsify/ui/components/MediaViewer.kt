package com.example.nkdsify.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.ImageLoader
import com.example.nkdsify.R
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.utils.rememberCoilImageLoader
import com.example.nkdsify.ui.utils.CryptoUtils
import com.example.nkdsify.ui.utils.ExternalMediaErrorDialog
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.ViewHistoryRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import com.example.nkdsify.data.BlurType
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.FrontHand

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    myAppState: MyAppState,
    items: List<MediaItem>,
    startIndex: Int,
    favorites: MutableList<String>? = null,
    imageLoader: ImageLoader? = null,
    isExternal: Boolean = false,
    isTrashMode: Boolean = false,
    isSecretMode: Boolean = false
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    val context = LocalContext.current
    var decryptedUri by remember { mutableStateOf<Uri?>(null) }
    var isDecrypting by remember { mutableStateOf(false) }

    val view = LocalView.current
    val window = (view.context as Activity).window

    DisposableEffect(view) {
        val originalLightStatusBars = WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // Dark icons for light theme

        onDispose {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = originalLightStatusBars
        }
    }

    if (isSecretMode) {
        LaunchedEffect(pagerState.currentPage) {
            val item = items[pagerState.currentPage]
            
            // Skip full decryption for videos - they will be streamed
            if (item.isVideo) {
                isDecrypting = false
                decryptedUri = null
                return@LaunchedEffect
            }

            isDecrypting = true
            decryptedUri = null

            val decryptedFile = withContext(Dispatchers.IO) {
                val cacheDir = context.cacheDir.resolve("decrypted_media")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val originalFile = File(item.uri.path!!)
                val cachedFile = File(cacheDir, originalFile.name)

                if (cachedFile.exists() && cachedFile.length() > 0) {
                    cachedFile
                } else {
                    try {
                        originalFile.inputStream().use { input ->
                            cachedFile.outputStream().use { output ->
                                CryptoUtils.decrypt(input, output)
                            }
                        }
                        cachedFile
                    } catch (e: Exception) {
                        cachedFile.delete() // Clean up on error
                        null
                    }
                }
            }

            decryptedUri = decryptedFile?.let { Uri.fromFile(it) }
            isDecrypting = false
        }
    }

    val isVibrationEnabled = remember { SettingsRepository.isVibrationEnabled(context) }
    var showExternalMediaError by remember { mutableStateOf(false) }
    var isMuted by remember(pagerState.currentPage) { mutableStateOf(myAppState.isMuteVideoByDefault) }

    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) {
            myAppState.viewerState = null
        }
    }

    // --- Unified Controls Visibility State ---
    var controlsVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(pagerState.currentPage, myAppState.isKeepControlsVisible) {
        val currentItem = items.getOrNull(pagerState.currentPage)
        val isVideoItem = currentItem?.isVideo == true
        
        // Force show controls if it's a photo and "keep controls visible" is enabled
        if (!isVideoItem && myAppState.isKeepControlsVisible) {
            controlsVisible = true
        }

        if (!isExternal && !isTrashMode && !isSecretMode) {
            currentItem?.let { item ->
                ViewHistoryRepository.addToHistory(context, item.uri)
                // Also update in-memory history so UI reflects changes immediately
                try {
                    val mutable = myAppState.viewHistory.toMutableList()
                    // remove existing entry for this uri
                    mutable.removeAll { it.uri == item.uri }
                    // add to front
                    mutable.add(0, item)
                    // limit size to repository limit
                    val maxSize = 200
                    val trimmed = if (mutable.size > maxSize) mutable.subList(0, maxSize) else mutable
                    myAppState.viewHistory = trimmed.toImmutableList()
                } catch (_: Exception) {
                    // ignore any concurrency issues
                }
            }
        }
    }

    val isVideo = items.getOrNull(pagerState.currentPage)?.isVideo == true
    val toggleControls = {
        if (isVideo || !myAppState.isKeepControlsVisible) {
            controlsVisible = !controlsVisible
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, myAppState.isKeepControlsVisible, pagerState.currentPage) {
        if (controlsVisible && (isVideo || !myAppState.isKeepControlsVisible)) {
            delay(4000)
            controlsVisible = false
        }
    }

    val favoritesListMutable: MutableList<String> = favorites ?: myAppState.favoritesList
    val imageLoaderUsed: ImageLoader = imageLoader ?: myAppState.imageLoader ?: rememberCoilImageLoader(context)

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
            userScrollEnabled = myAppState.isSwipeToDismissEnabled
        ) { page ->
            val item = items[page]
            val isFullyVisible by remember { derivedStateOf { !pagerState.isScrollInProgress && pagerState.currentPage == page } }
            val isIndividualBlurred = item.uri.toString() in myAppState.blurredUris

            Box(modifier = Modifier.fillMaxSize()) {
                if (isSecretMode) {
                    if (item.isVideo) {
                        VideoPlayerPage(
                            uri = item.uri, // Original encrypted URI
                            isFullyVisible = isFullyVisible,
                            isMuted = isMuted,
                            controlsVisible = controlsVisible,
                            onToggleControls = toggleControls,
                            onMuteClick = { isMuted = !isMuted },
                            isLoopVideoEnabled = myAppState.isLoopVideoEnabled,
                            isSecretMode = true // Use streaming decryption
                        )
                    } else {
                        if (isDecrypting) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(id = R.string.decrypting_file), color = Color.White)
                                }
                            }
                        } else if (decryptedUri != null) {
                            ZoomableImage(
                                uri = decryptedUri!!,
                                imageLoader = imageLoaderUsed,
                                zoomType = myAppState.selectedZoomType,
                                isVibrationEnabled = isVibrationEnabled,
                                onToggleControls = toggleControls
                            )
                        }
                    }
                } else { // Original logic for non-secret items
                    if (item.isVideo) {
                        VideoPlayerPage(
                            uri = item.uri,
                            isFullyVisible = isFullyVisible,
                            isMuted = isMuted,
                            controlsVisible = controlsVisible,
                            onToggleControls = toggleControls,
                            onMuteClick = { isMuted = !isMuted },
                            isLoopVideoEnabled = myAppState.isLoopVideoEnabled,
                            isSecretMode = false
                        )
                    } else {
                        ZoomableImage(
                            uri = item.uri,
                            imageLoader = imageLoaderUsed,
                            zoomType = myAppState.selectedZoomType,
                            isVibrationEnabled = isVibrationEnabled,
                            onToggleControls = toggleControls,
                            modifier = if (isIndividualBlurred && myAppState.selectedBlurType == BlurType.BLUR) Modifier.blur(30.dp) else Modifier
                        )
                    }
                }

                if (isIndividualBlurred && myAppState.selectedBlurType == BlurType.PLACEHOLDER && !item.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FrontHand,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                    }
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
                    myAppState.viewerState = null
                    myAppState.secretViewerState = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Spacer(Modifier.weight(1f))

                if (currentItem != null) {
                    when {
                        isSecretMode -> {
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                // Restore in secret mode
                                myAppState.itemsToRestoreFromSecret = listOf(currentItem.uri).toImmutableList()
                                myAppState.showConfirmRestoreFromSecretDialog = true
                            }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                myAppState.itemsToDeleteFromSecret = listOf(currentItem.uri).toImmutableList()
                                myAppState.showConfirmDeleteFromSecretDialog = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                        isTrashMode -> {
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                myAppState.itemsToRestore = listOf(currentItem.uri).toImmutableList()
                                myAppState.showConfirmRestoreDialog = true
                            }) {
                                Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Restore", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                myAppState.itemsToDelete = listOf(currentItem.uri).toImmutableList()
                                myAppState.showConfirmDeleteDialog = true
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
                                    myAppState.showTagDialog = currentItem.uri
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags", tint = Color.White)
                            }

                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                val uri = currentItem.uri
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    type = context.contentResolver.getType(uri) ?: if (currentItem.isVideo) "video/*" else "image/*"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                if (isExternal) {
                                    showExternalMediaError = true
                                } else {
                                    // Delete -> move to trash
                                    myAppState.itemsToTrash = listOf(currentItem.uri).toImmutableList()
                                    myAppState.showConfirmTrashDialog = true
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
                                    if (favoritesListMutable.contains(currentItem.absolutePath)) {
                                        favoritesListMutable.remove(currentItem.absolutePath)
                                    } else {
                                        favoritesListMutable.add(currentItem.absolutePath)
                                    }
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
                                    imageVector = if (favoritesListMutable.contains(currentItem.absolutePath)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (favoritesListMutable.contains(currentItem.absolutePath)) Color.Red else Color.White
                                )
                            }
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                myAppState.showDetailsDialog = currentItem.uri
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
