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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.ImageLoader
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.CryptoUtils
import com.example.nkdsify.ui.utils.ExternalMediaErrorDialog
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.ViewHistoryRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    favorites: List<String>,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader,
    onDelete: (List<Uri>) -> Unit,
    onRestore: (List<Uri>) -> Unit,
    onShowTagDialog: (Uri) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShowDetails: (Uri) -> Unit,
    isExternal: Boolean = false,
    isTrashMode: Boolean,
    isSecretMode: Boolean = false, // <-- ДОБАВЬТЕ ЭТО
    isMuteVideoByDefault: Boolean,
    zoomType: ZoomType,
    isLoopVideoEnabled: Boolean,
    isSwipeToDismissEnabled: Boolean,
    isKeepControlsVisible: Boolean
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    val context = LocalContext.current
    var tempFileUri by remember { mutableStateOf<Uri?>(null) }

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
    val toggleControls = {
        if (!isKeepControlsVisible) {
            controlsVisible = !controlsVisible
        }
    }

    // Auto-hide controls
    // Auto-hide controls
    // Auto-hide controls
    LaunchedEffect(controlsVisible, isKeepControlsVisible) {
        if (controlsVisible && !isKeepControlsVisible) {
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
            userScrollEnabled = isSwipeToDismissEnabled
        ) { page ->
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
                                    onToggleFavorite(currentItem.absolutePath)
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
                                    imageVector = if (favorites.contains(currentItem.absolutePath)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (favorites.contains(currentItem.absolutePath)) Color.Red else Color.White
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