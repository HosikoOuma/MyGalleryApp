package com.example.nkdsify.ui.impl

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.ui.screens.TrashScreen
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.toImmutableList

@Composable
fun TrashScreenImpl(
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    context: android.content.Context
) {
    TrashScreen(
        items = myAppState.trashedItems,
        selectedItems = myAppState.selectedItems.toList(),
        imageLoader = imageLoader,
        onItemClick = { item ->
            keyboardController?.hide()
            myAppState.viewerState = MediaViewerState(items = myAppState.trashedItems, startIndex = myAppState.trashedItems.indexOf(item))
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
        gridState = gridState
    )
}