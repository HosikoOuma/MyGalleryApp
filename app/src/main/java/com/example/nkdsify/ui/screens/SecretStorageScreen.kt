package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.nkdsify.R
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.MediaGrid

@Composable
fun SecretStorageScreen(
    items: List<MediaItem>,
    imageLoader: ImageLoader,
    selectedItems: List<Uri>,
    onToggleSelection: (MediaItem) -> Unit,
    onClearSelection: () -> Unit,
    onItemClick: (List<MediaItem>, MediaItem) -> Unit,
    isBlurEnabled: Boolean,
    blurType: BlurType,
    gridState: LazyGridState
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(id = R.string.no_secret_items))
        }
    } else {
        MediaGrid(
            items = items,
            favorites = emptyList(), // No favorites in secret storage
            selectedItems = selectedItems,
            imageLoader = imageLoader,
            isBlurEnabled = isBlurEnabled,
            blurType = blurType,
            onItemClick = { item -> onItemClick(items, item) },
            onToggleSelection = onToggleSelection,
            onClearSelection = onClearSelection,
            gridState = gridState
        )
    }
}