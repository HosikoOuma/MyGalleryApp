package com.example.nkdsify.ui.impl

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.screens.FavoritesScreen
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun FavoritesScreenImpl(
    filteredFavoriteItems: ImmutableList<MediaItem>,
    favorites: MutableList<String>,
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    contentGridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    context: android.content.Context
) {
    FavoritesScreen(
        items = filteredFavoriteItems,
        favorites = favorites,
        selectedItems = myAppState.selectedItems,
        imageLoader = imageLoader,
        tags = myAppState.tags,
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
        isBlurEnabled = myAppState.isBlurEnabled,
        isBlurInFolderEnabled = myAppState.isBlurInFolderEnabled,
        gridState = gridState,
        contentGridState = contentGridState,
        openAlbumName = null,
        onOpenAlbum = { albumName ->
            if (myAppState.isVibrationEnabled) performVibration(context)
            myAppState.currentScreen = Screen.Favorites(openAlbumName = albumName)
        },
        isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
        onClearSelection = { myAppState.selectedItems.clear() },
        blurType = myAppState.selectedBlurType
    )
}
