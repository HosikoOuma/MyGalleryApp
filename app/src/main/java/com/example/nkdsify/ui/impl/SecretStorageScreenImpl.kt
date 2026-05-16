package com.example.nkdsify.ui.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.screens.SecretStorageScreen
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SecretStorageScreenImpl(
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    SecretStorageScreen(
        items = myAppState.secretItems,
        imageLoader = imageLoader,
        selectedItems = myAppState.selectedItems,
        onToggleSelection = { item ->
            if (myAppState.selectedItems.contains(item.uri)) {
                myAppState.selectedItems.remove(item.uri)
            } else {
                myAppState.selectedItems.add(item.uri)
            }
        },
        onClearSelection = { myAppState.selectedItems.clear() },
        onItemClick = { items, item ->
            keyboardController?.hide()
            val state = MediaViewerState(items = items.toImmutableList(), startIndex = items.indexOf(item))
            myAppState.currentScreen = Screen.MediaViewer(state, isSecret = true)
        },
        isBlurEnabled = myAppState.isBlurEnabled,
        blurType = myAppState.selectedBlurType,
        gridState = gridState,
        isVideoPreviewSlideshowEnabled = myAppState.isVideoPreviewSlideshowEnabled,
        videoSlideshowIntervalMs = myAppState.videoSlideshowIntervalMs,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}
