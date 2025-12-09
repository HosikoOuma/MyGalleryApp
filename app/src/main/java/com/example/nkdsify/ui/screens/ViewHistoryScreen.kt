package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import com.example.nkdsify.R
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.MediaGrid

@Composable
fun ViewHistoryScreen(
    items: List<MediaItem>,
    favorites: List<String>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    onItemClick: (List<MediaItem>, MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    onClearSelection: () -> Unit,
    gridState: LazyGridState,
    isBlurEnabled: Boolean,
    blurType: BlurType
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(id = R.string.history_is_empty))
        }
    } else {
        Column {
            MediaGrid(
                items = items,
                favorites = favorites,
                selectedItems = selectedItems,
                imageLoader = imageLoader,
                onItemClick = { item -> onItemClick(items, item) },
                onToggleSelection = onToggleSelection,
                onClearSelection = onClearSelection,
                gridState = gridState,
                isBlurEnabled = isBlurEnabled,
                blurType = blurType
            )
        }
    }
}
