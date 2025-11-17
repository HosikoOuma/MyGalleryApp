package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.nkdsify.R
import com.example.nkdsify.data.BlurType
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
    onClearSelection: () -> Unit,
    blurType: BlurType,
    gridState: LazyGridState
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = stringResource(id = R.string.trash_is_empty),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaGrid(
            items = items,
            favorites = emptyList(),
            selectedItems = selectedItems,
            imageLoader = imageLoader,
            onItemClick = onItemClick,
            onToggleSelection = onToggleSelection,
            isBlurEnabled = isTrashBlurEnabled,
            onClearSelection = onClearSelection,
            blurType = blurType,
            gridState = gridState
        )

        AnimatedVisibility(
            visible = selectedItems.isEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Button(
                onClick = onClearTrash,
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(stringResource(id = R.string.clear_trash_button))
            }
        }
    }
}
