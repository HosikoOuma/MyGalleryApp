package com.example.nkdsify.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    modifier: Modifier = Modifier,
    items: List<MediaItem>,
    favorites: List<String>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    onItemClick: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    onClearSelection: () -> Unit,
    isBlurEnabled: Boolean = false,
    blurType: BlurType,
    gridState: LazyGridState
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No media files found")
        }
        return
    }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isSelectionMode = selectedItems.isNotEmpty()
    val isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(context)) }

    BackHandler(enabled = isSelectionMode) {
        onClearSelection()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.uri }, contentType = { "media" }) { item ->
                val isSelected = selectedItems.contains(item.uri)
                Box(modifier = Modifier.padding(4.dp)) {
                    Card(modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .pointerInput(item, isSelectionMode) {
                            detectTapGestures(onTap = {
                                if (isSelectionMode) {
                                    if (isVibrationEnabled) performVibration(context)
                                    onToggleSelection(item)
                                } else {
                                    onItemClick(item)
                                }
                            }, onLongPress = { _ ->
                                if (!isSelectionMode) {
                                    if (isVibrationEnabled) performVibration(context)
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleSelection(item)
                                }
                            })
                        }) {
                        if (isBlurEnabled && blurType == BlurType.PLACEHOLDER) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FrontHand,
                                    contentDescription = "Hidden content",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .bitmapConfig(Bitmap.Config.RGB_565)
                                    .allowHardware(false)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (isSelected) Modifier.alpha(0.5f) else Modifier)
                                    .then(if (isBlurEnabled && blurType == BlurType.BLUR) Modifier.blur(20.dp) else Modifier)
                            )
                        }
                    }
                    if (item.isVideo) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(24.dp)
                        )
                    }
                    if (favorites.contains(item.absolutePath)) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
        CustomVerticalScrollbar(
            gridState = gridState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )

        // Date bubble: show date of first visible item while scrolling (and briefly after)
        val isScrollInProgress by remember { derivedStateOf { gridState.isScrollInProgress } }
        val firstVisibleIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
        var showDateBubble by remember { mutableStateOf(false) }
        val firstVisibleItem = remember(items, firstVisibleIndex) { items.getOrNull(firstVisibleIndex) }
        val bubbleText = remember(firstVisibleItem) {
            firstVisibleItem?.let {
                try {
                    val instant = Instant.ofEpochSecond(it.dateModified)
                    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())
                    fmt.format(instant)
                } catch (e: Exception) { "" }
            } ?: ""
        }

        // show while scrolling, hide shortly after scroll finishes
        LaunchedEffect(isScrollInProgress, firstVisibleIndex) {
            if (isScrollInProgress) {
                showDateBubble = true
            } else {
                // keep visible briefly after scroll stops
                delay(700)
                showDateBubble = false
            }
        }

        AnimatedVisibility(
            visible = showDateBubble && bubbleText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 0.dp)
                .offset(y = (-16).dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = bubbleText, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CustomVerticalScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isScrollInProgress by remember { derivedStateOf { gridState.isScrollInProgress } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerInput(gridState) { detectDragGestures { change, _ ->
                change.consume()
                val trackHeight = size.height.toFloat()
                val dragProgress = (change.position.y / trackHeight).coerceIn(0f, 1f)

                val totalItems = gridState.layoutInfo.totalItemsCount
                if (totalItems > 0) {
                    coroutineScope.launch {
                        val targetIndex = (dragProgress * (totalItems - 1)).toInt()
                        gridState.scrollToItem(targetIndex)
                    }
                }
            } }
    ) {
        val scrollbarState by remember(gridState, maxHeight) {
            derivedStateOf {
                val layoutInfo = gridState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0 || layoutInfo.visibleItemsInfo.isEmpty()) {
                    null
                } else {
                    val visibleItemsCount = layoutInfo.visibleItemsInfo.size.toFloat()
                    val scrollableItems = (totalItems - visibleItemsCount).coerceAtLeast(1f)
                    val scrollProgress = gridState.firstVisibleItemIndex.toFloat() / scrollableItems
                    val thumbHeight = (maxHeight * (visibleItemsCount / totalItems)).coerceAtLeast(20.dp)
                    val thumbOffsetY = (maxHeight - thumbHeight) * scrollProgress
                    Pair(thumbHeight, thumbOffsetY)
                }
            }
        }

        AnimatedVisibility(
            visible = isScrollInProgress && scrollbarState != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            scrollbarState?.let { (thumbHeight, thumbOffsetY) ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(thumbHeight)
                        .offset(y = thumbOffsetY)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
