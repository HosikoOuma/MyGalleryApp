package com.example.nkdsify.ui.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.components.MediaGrid
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaByTagScreenImpl(
    myAppState: MyAppState,
    screen: Screen.MediaByTag,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val mediaWithTag = remember(myAppState.allMedia, myAppState.tags, screen.tag) {
        val urisWithTag = myAppState.tags.filter { it.value.contains(screen.tag) }.keys
        myAppState.allMedia.filter { it.absolutePath in urisWithTag }.toImmutableList()
    }
    MediaGrid(
        items = mediaWithTag,
        favorites = myAppState.favoritesList,
        selectedItems = myAppState.selectedItems,
        imageLoader = imageLoader,
        isBlurEnabled = myAppState.isBlurInFolderEnabled,
        onItemClick = { item ->
            keyboardController?.hide()
            val state = MediaViewerState(items = mediaWithTag, startIndex = mediaWithTag.indexOf(item))
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
