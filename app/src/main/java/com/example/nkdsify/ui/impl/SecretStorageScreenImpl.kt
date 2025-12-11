package com.example.nkdsify.ui.impl

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.ui.screens.SecretStorageScreen
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SecretStorageScreenImpl(
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?
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
            myAppState.secretViewerState = MediaViewerState(items = items.toImmutableList(), startIndex = items.indexOf(item))
        },
        isBlurEnabled = myAppState.isBlurEnabled,
        blurType = myAppState.selectedBlurType,
        gridState = gridState
    )
}