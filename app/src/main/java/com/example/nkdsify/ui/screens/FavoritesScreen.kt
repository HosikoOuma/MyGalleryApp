package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.compose.runtime.Composable
import coil.ImageLoader
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.MediaGrid

@Composable
fun FavoritesScreen(
    items: List<MediaItem>,
    favorites: List<Uri>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    onItemClick: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit
) {
    MediaGrid(
        items = items,
        favorites = favorites,
        selectedItems = selectedItems,
        imageLoader = imageLoader,
        onItemClick = onItemClick,
        onToggleSelection = onToggleSelection
    )
}
