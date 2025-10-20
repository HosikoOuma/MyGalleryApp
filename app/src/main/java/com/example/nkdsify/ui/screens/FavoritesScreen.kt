package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.MediaGrid

@Composable
fun FavoritesScreen(
    items: List<MediaItem>,
    favorites: List<Uri>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    tags: Map<String, Set<String>>,
    onItemClick: (MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    isBlurEnabled: Boolean
) {
    val taggedAlbums = items
        .flatMap { item -> (tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
        .groupBy({ it.first }, { it.second })

    val displayAlbums = mutableListOf<Pair<String, List<MediaItem>>>()
    if (items.isNotEmpty()) {
        displayAlbums.add("All Favorites" to items)
    }
    displayAlbums.addAll(taggedAlbums.entries.map { it.key to it.value }.sortedBy { it.first })

    var openAlbumName by remember { mutableStateOf<String?>(null) }

    if (openAlbumName != null && displayAlbums.find { it.first == openAlbumName } == null) {
        openAlbumName = null
    }

    BackHandler(enabled = openAlbumName != null) {
        openAlbumName = null
    }

    val albumToShow = if (openAlbumName != null) displayAlbums.find { it.first == openAlbumName } else null

    if (albumToShow != null) {
        MediaGrid(
            items = albumToShow.second,
            favorites = favorites,
            selectedItems = selectedItems,
            imageLoader = imageLoader,
            onItemClick = onItemClick,
            onToggleSelection = onToggleSelection
        )
    } else {
        if (displayAlbums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No favorites yet")
            }
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(displayAlbums, key = { it.first }) { (albumName, albumItems) ->
                if (albumItems.isNotEmpty()) { 
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .aspectRatio(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(),
                                onClick = { openAlbumName = albumName }
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(model = albumItems.first().uri, imageLoader = imageLoader),
                                contentDescription = "Album cover for $albumName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .then(if (isBlurEnabled) Modifier.blur(16.dp) else Modifier)
                            )
                            Text(text = albumName, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}
