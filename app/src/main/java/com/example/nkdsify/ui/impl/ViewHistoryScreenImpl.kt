package com.example.nkdsify.ui.impl

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.ui.screens.ViewHistoryScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ViewHistoryScreenImpl(
    filteredViewHistory: ImmutableList<MediaItem>,
    favorites: MutableList<String>,
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?
) {
    ViewHistoryScreen(
        items = filteredViewHistory,
        favorites = favorites,
        selectedItems = myAppState.selectedItems,
        imageLoader = imageLoader,
        onItemClick = { items, item ->
            keyboardController?.hide()
            myAppState.viewerState = MediaViewerState(items = items.toImmutableList(), startIndex = items.indexOf(item))
        },
        onToggleSelection = { item ->
            if (myAppState.selectedItems.contains(item.uri)) {
                myAppState.selectedItems.remove(item.uri)
            } else {
                myAppState.selectedItems.add(item.uri)
            }
        },
        onClearSelection = { myAppState.selectedItems.clear() },
        gridState = gridState,
        blurType = myAppState.selectedBlurType,
        isBlurEnabled = myAppState.isBlurEnabled
    )
}