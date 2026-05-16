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
import com.example.nkdsify.ui.screens.TrashScreen
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrashScreenImpl(
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    context: android.content.Context,
    isNavBarVisible: Boolean, // Добавляем параметр
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    TrashScreen(
        items = myAppState.trashedItems,
        selectedItems = myAppState.selectedItems.toList(),
        imageLoader = imageLoader,
        onItemClick = { item ->
            keyboardController?.hide()
            val state = MediaViewerState(items = myAppState.trashedItems, startIndex = myAppState.trashedItems.indexOf(item))
            myAppState.currentScreen = Screen.MediaViewer(state)
        },
        onToggleSelection = { item ->
            if (myAppState.selectedItems.contains(item.uri)) {
                myAppState.selectedItems.remove(item.uri)
            } else {
                myAppState.selectedItems.add(item.uri)
            }
        },
        onClearTrash = {
            if (myAppState.isVibrationEnabled) performVibration(context)
            myAppState.isClearingTrash = true
            myAppState.itemsToDelete = myAppState.trashedItems.map { it.uri }.toImmutableList()
            myAppState.showConfirmDeleteDialog = true
        },
        isTrashBlurEnabled = myAppState.isTrashBlurEnabled,
        onClearSelection = { myAppState.selectedItems.clear() },
        blurType = myAppState.selectedBlurType,
        gridState = gridState,
        isNavBarVisible = isNavBarVisible,
        blurredUris = myAppState.blurredUris,
        isVideoPreviewSlideshowEnabled = myAppState.isVideoPreviewSlideshowEnabled,
        videoSlideshowIntervalMs = myAppState.videoSlideshowIntervalMs,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}
