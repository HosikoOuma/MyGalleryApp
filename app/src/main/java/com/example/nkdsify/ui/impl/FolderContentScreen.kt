package com.example.nkdsify.ui.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.components.MediaGrid
import com.example.nkdsify.ui.utils.parseQueryString
import kotlinx.collections.immutable.toImmutableList
import java.util.Comparator

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FolderContentScreen(
    screen: Screen.FolderContent,
    sortComparator: Comparator<MediaItem>,
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    keyboardController: SoftwareKeyboardController?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val folder = myAppState.allFolders.find { it.id == screen.folder.id } ?: screen.folder

    val items by remember(folder.items, sortComparator, myAppState.isSearchActive, myAppState.searchQuery, myAppState.tags) {
        derivedStateOf {
            val sortedItems = folder.items.sortedWith(sortComparator)

            if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
                val parsed = parseQueryString(myAppState.searchQuery)
                sortedItems.filter { item ->
                    val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()

                    val textMatch = parsed.searchTerms.all { term -> item.name.contains(term, ignoreCase = true) }
                    val tagMatch = parsed.includedTagGroups.all { group -> group.any { tag -> itemTags.contains(tag) } } &&
                            (parsed.excludedTags.isEmpty() || !itemTags.any { it in parsed.excludedTags })

                    textMatch && tagMatch
                }.toImmutableList()
            } else {
                sortedItems.toImmutableList()
            }
        }
    }

    MediaGrid(
        items = items,
        favorites = myAppState.favoritesList,
        selectedItems = myAppState.selectedItems,
        imageLoader = imageLoader,
        isBlurEnabled = myAppState.isBlurInFolderEnabled,
        onItemClick = { item ->
            keyboardController?.hide()
            val state = MediaViewerState(items = items, startIndex = items.indexOf(item))
            myAppState.currentScreen = Screen.MediaViewer(state)
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
        gridState = gridState,
        blurredUris = myAppState.blurredUris,
        isVideoPreviewSlideshowEnabled = myAppState.isVideoPreviewSlideshowEnabled,
        videoSlideshowIntervalMs = myAppState.videoSlideshowIntervalMs,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}
