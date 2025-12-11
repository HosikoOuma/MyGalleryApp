package com.example.nkdsify.ui.impl

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.components.MediaGrid
import kotlinx.collections.immutable.toImmutableList
import java.util.Comparator

@Composable
fun FolderContentScreen(
    screen: Screen.FolderContent,
    sortComparator: Comparator<MediaItem>,
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?
) {
    val folder = myAppState.allFolders.find { it.id == screen.folder.id } ?: screen.folder
    val sortedItems = remember(folder.items, sortComparator) {
        folder.items.sortedWith(sortComparator).toImmutableList()
    }
    val items = if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
        sortedItems.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }.toImmutableList()
    } else {
        sortedItems
    }
    MediaGrid(
        items = items,
        favorites = myAppState.favoritesList,
        selectedItems = myAppState.selectedItems,
        imageLoader = imageLoader,
        isBlurEnabled = myAppState.isBlurInFolderEnabled,
        onItemClick = { item ->
            keyboardController?.hide()
            myAppState.viewerState = MediaViewerState(items = items, startIndex = items.indexOf(item))
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
        gridState = gridState
    )
}


