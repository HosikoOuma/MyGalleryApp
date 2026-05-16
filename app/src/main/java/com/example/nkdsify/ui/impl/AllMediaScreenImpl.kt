package com.example.nkdsify.ui.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.components.MediaGrid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AllMediaScreenImpl(
    filteredAllMedia: ImmutableList<MediaItem>,
    favorites: MutableList<String>,
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    MediaGrid(
        items = filteredAllMedia,
        favorites = favorites,
        selectedItems = myAppState.selectedItems,
        imageLoader = imageLoader,
        isBlurEnabled = myAppState.isBlurAllMediaEnabled,
        onItemClick = { item ->
            keyboardController?.hide()
            val state = MediaViewerState(items = filteredAllMedia.toImmutableList(), startIndex = filteredAllMedia.indexOf(item))
            myAppState.currentScreen = Screen.MediaViewer(state)
        },
        onToggleSelection = { item ->
            if (myAppState.selectedItems.contains(item.uri)) {
                myAppState.selectedItems.remove(item.uri)
            } else {
                myAppState.selectedItems.add(item.uri)
            }
        },
        onClearSelection = { myAppState.selectedItems.clear() },
        blurType = myAppState.selectedBlurType,
        gridState = gridState,
        blurredUris = myAppState.blurredUris,
        isVideoPreviewSlideshowEnabled = myAppState.isVideoPreviewSlideshowEnabled,
        videoSlideshowIntervalMs = myAppState.videoSlideshowIntervalMs,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}
