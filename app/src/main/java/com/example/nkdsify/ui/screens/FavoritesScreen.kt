package com.example.nkdsify.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material3.ripple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.nkdsify.R
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.MediaGrid

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    items: List<MediaItem>,
    favorites: List<String>,
    selectedItems: List<Uri>,
    imageLoader: ImageLoader,
    tags: Map<String, Set<String>>,
    onItemClick: (List<MediaItem>, MediaItem) -> Unit,
    onToggleSelection: (MediaItem) -> Unit,
    isBlurEnabled: Boolean,
    isBlurInFolderEnabled: Boolean,
    gridState: LazyGridState,
    contentGridState: LazyGridState,
    openAlbumName: String?,
    onOpenAlbum: (String) -> Unit,
    isShowFileCountEnabled: Boolean,
    onClearSelection: () -> Unit,
    blurType: BlurType,
    blurredUris: Set<String> = emptySet(),
    isVideoPreviewSlideshowEnabled: Boolean = false,
    videoSlideshowIntervalMs: Long = 800L,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val taggedAlbums = items
        .flatMap { item -> (tags[item.absolutePath] ?: emptySet()).map { tag -> tag to item } }
        .groupBy({ it.first }, { it.second })

    val displayAlbums = mutableListOf<Pair<String, List<MediaItem>>>()
    val allFavoritesAlbumName = stringResource(id = R.string.album_name_all_favorites)
    if (items.isNotEmpty()) {
        displayAlbums.add(allFavoritesAlbumName to items)
    }
    displayAlbums.addAll(taggedAlbums.entries.map { it.key to it.value }.sortedBy { it.first })

    val albumToShow = if (openAlbumName != null) displayAlbums.find { it.first == openAlbumName } else null

    if (albumToShow != null) {
        MediaGrid(
            items = albumToShow.second,
            favorites = favorites,
            selectedItems = selectedItems,
            imageLoader = imageLoader,
            onItemClick = { item -> onItemClick(albumToShow.second, item) },
            onToggleSelection = onToggleSelection,
            onClearSelection = onClearSelection,
            blurType = blurType,
            isBlurEnabled = isBlurInFolderEnabled,
            gridState = contentGridState,
            blurredUris = blurredUris,
            isVideoPreviewSlideshowEnabled = isVideoPreviewSlideshowEnabled,
            videoSlideshowIntervalMs = videoSlideshowIntervalMs,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    } else {
        if (displayAlbums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(id = R.string.no_favorites_yet))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
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
                                    indication = ripple(),
                                    onClick = { onOpenAlbum(albumName) }
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    if (isBlurEnabled && blurType == BlurType.PLACEHOLDER) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FrontHand,
                                                contentDescription = stringResource(id = R.string.hidden_content_placeholder),
                                                tint = Color.Gray,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    } else {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = albumItems.first().uri, imageLoader = imageLoader),
                                            contentDescription = stringResource(id = R.string.album_cover_content_description, albumName),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (isBlurEnabled && blurType == BlurType.BLUR) Modifier.blur(16.dp) else Modifier)
                                        )
                                    }
                                }
                                if (isShowFileCountEnabled) {
                                    Text(text = "$albumName (${albumItems.size})", modifier = Modifier.padding(8.dp))
                                } else {
                                    Text(text = albumName, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
