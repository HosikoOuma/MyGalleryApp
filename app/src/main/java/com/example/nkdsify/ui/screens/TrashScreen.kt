package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.MediaGrid

@Composable
fun TrashScreen(
    items: List<MediaItem>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    onItemClick: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    onClearTrash: () -> Unit,
    isTrashBlurEnabled: Boolean,
    onClearSelection: () -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Trash is empty")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onClearTrash,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear Trash")
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            MediaGrid(
                items = items,
                favorites = emptyList(),
                selectedItems = selectedItems,
                imageLoader = imageLoader,
                onItemClick = onItemClick,
                onToggleSelection = onToggleSelection,
                isBlurEnabled = isTrashBlurEnabled,
                onClearSelection = onClearSelection
            )
        }
    }
}
