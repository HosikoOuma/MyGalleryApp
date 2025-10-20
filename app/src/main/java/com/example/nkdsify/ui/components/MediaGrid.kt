package com.example.nkdsify.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.nkdsify.data.MediaItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    favorites: List<Uri>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    onItemClick: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No media files found")
        }
        return
    }
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()
    val isSelectionMode = selectedItems.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
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
                                    onToggleSelection(item)
                                } else {
                                    onItemClick(item)
                                }
                            }, onLongPress = { _ ->
                                if (!isSelectionMode) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleSelection(item)
                                }
                            })
                        }) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = item.uri,
                                imageLoader = imageLoader
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isSelected) Modifier.alpha(0.5f) else Modifier)
                        )
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
                    if (favorites.contains(item.uri) && !isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
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
        val scrollbarState by remember(gridState) {
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
