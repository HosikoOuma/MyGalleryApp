package com.example.nkdsify.ui

import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.utils.performVibration
import com.example.nkdsify.ui.utils.parseQueryString
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MyAppFAB(
    myAppState: MyAppState
) {
    val context = LocalContext.current
    val coroutineScope = myAppState.coroutineScope
    val isVibrationEnabled = myAppState.isVibrationEnabled
    val useLargeFab = myAppState.useLargeFab
    val screen = myAppState.currentScreen

    // Allow FAB on screens where shuffle is meaningful
    if (myAppState.isShuffleButtonVisible && screen !is Screen.Trash
        && screen !is Screen.Settings
        && screen !is Screen.TagManagement
        && screen !is Screen.SecretStorage
        && screen !is Screen.ViewHistory
        && screen !is Screen.About
        && screen !is Screen.Help
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "FabScale")

        val allFavoritesAlbumName = stringResource(id = R.string.album_name_all_favorites)
        val onClick: () -> Unit = {
            if (isVibrationEnabled) performVibration(context)

            // Grab state before launching to avoid accessing it from a background thread
            val currentScreen = myAppState.currentScreen
            val mediaTypeFilter = myAppState.mediaTypeFilter
            val allMedia = myAppState.allMedia
            val favoriteItems = myAppState.favoriteItems
            val tags = myAppState.tags
            val isSearchActive = myAppState.isSearchActive
            val searchQuery = myAppState.searchQuery
            val selectedDate = myAppState.selectedDate

            coroutineScope?.launch {
                val viewerState = withContext(Dispatchers.Default) {
                    // Build base itemsToShuffle depending on screen
                    val itemsToShuffle: List<com.example.nkdsify.data.MediaItem> = when (currentScreen) {
                        is Screen.FolderContent -> currentScreen.folder.items
                        is Screen.AllMedia -> {
                            // Respect media type filter
                            when (mediaTypeFilter) {
                                com.example.nkdsify.data.MediaTypeFilter.PHOTOS -> allMedia.filter { !it.isVideo }
                                com.example.nkdsify.data.MediaTypeFilter.VIDEOS -> allMedia.filter { it.isVideo }
                                else -> allMedia
                            }
                        }
                        is Screen.Folders -> allMedia
                        is Screen.Favorites -> {
                            if (currentScreen.openAlbumName != null) {
                                val taggedAlbums = favoriteItems
                                    .flatMap { item -> (tags[item.absolutePath] ?: emptySet()).map { tag -> tag to item } }
                                    .groupBy({ it.first }, { it.second })
                                if (currentScreen.openAlbumName == allFavoritesAlbumName) favoriteItems else taggedAlbums[currentScreen.openAlbumName]
                                    ?: emptyList()
                            } else {
                                favoriteItems
                            }
                        }
                        is Screen.MediaByTag -> {
                            // Items matching the opened tag
                            allMedia.filter { item ->
                                val itemTags = tags[item.absolutePath] ?: emptySet()
                                itemTags.contains(currentScreen.tag)
                            }
                        }
                        else -> emptyList()
                    }

                    // If search is active, further filter itemsToShuffle by parsed query (supports +tag/-tag and plain terms)
                    val filtered = if (isSearchActive && searchQuery.isNotBlank()) {
                        val parsed = parseQueryString(searchQuery)
                        itemsToShuffle.filter { item ->
                            val itemTags = tags[item.absolutePath] ?: emptySet()
                            // selectedDate filter
                            selectedDate?.let {
                                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = it }
                                val itemCalendar = java.util.Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                                if (!(calendar.get(java.util.Calendar.YEAR) == itemCalendar.get(java.util.Calendar.YEAR) &&
                                            calendar.get(java.util.Calendar.DAY_OF_YEAR) == itemCalendar.get(java.util.Calendar.DAY_OF_YEAR))) return@filter false
                            }

                            val textMatch = parsed.searchTerms.all { term -> item.name.contains(term, ignoreCase = true) }
                            val tagMatch = parsed.includedTagGroups.all { group -> group.any { tag -> itemTags.contains(tag) } } &&
                                    (parsed.excludedTags.isEmpty() || !itemTags.any { it in parsed.excludedTags })

                            textMatch && tagMatch
                        }
                    } else {
                        // also apply selectedDate even when search not active
                        if (selectedDate != null) {
                            itemsToShuffle.filter { item ->
                                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val itemCalendar = java.util.Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                                calendar.get(java.util.Calendar.YEAR) == itemCalendar.get(java.util.Calendar.YEAR) &&
                                        calendar.get(java.util.Calendar.DAY_OF_YEAR) == itemCalendar.get(java.util.Calendar.DAY_OF_YEAR)
                            }
                        } else itemsToShuffle
                    }

                    if (filtered.isNotEmpty()) {
                        val shuffledItems = filtered.shuffled()
                        MediaViewerState(items = shuffledItems.toImmutableList(), startIndex = 0)
                    } else null
                }
                viewerState?.let { myAppState.viewerState = it }
            }
        }
        if (useLargeFab) {
            LargeFloatingActionButton(
                onClick = onClick,
                modifier = Modifier.scale(scale),
                interactionSource = interactionSource
            ) {
                Icon(Icons.Filled.Photo, contentDescription = stringResource(id = R.string.content_description_shuffle_play), modifier = Modifier.size(40.dp))
            }
        } else {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier.scale(scale),
                interactionSource = interactionSource
            ) {
                Icon(Icons.Filled.Photo, contentDescription = stringResource(id = R.string.content_description_shuffle_play), modifier = Modifier.size(24.dp))
            }
        }
    }
}
