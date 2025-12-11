package com.example.nkdsify.ui

import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.FabAction
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
    val coroutineScope = myAppState.coroutineScope ?: rememberCoroutineScope()
    val isVibrationEnabled = myAppState.isVibrationEnabled
    val useLargeFab = myAppState.useLargeFab

    // Allow FAB on MediaByTag as well; we'll handle shuffling tag-album content
    if (myAppState.isShuffleButtonVisible && myAppState.currentScreen !is Screen.Trash
        && myAppState.currentScreen !is Screen.Settings
        && myAppState.currentScreen !is Screen.TagManagement
        // && myAppState.currentScreen !is Screen.MediaByTag -- removed exclusion
        && myAppState.currentScreen !is Screen.SecretStorage
        && myAppState.currentScreen !is Screen.ViewHistory
        && myAppState.currentScreen !is Screen.About) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "FabScale")

        val allFavoritesAlbumName = stringResource(id = R.string.album_name_all_favorites)
        val onClick: () -> Unit = {
            if (isVibrationEnabled) performVibration(context)

            when (myAppState.selectedFabAction) {
                FabAction.SHUFFLE -> {
                    coroutineScope.launch {
                        // Build base itemsToShuffle depending on screen
                        val itemsToShuffle: List<com.example.nkdsify.data.MediaItem> = when (val screen = myAppState.currentScreen) {
                            is Screen.FolderContent -> screen.folder.items
                            is Screen.AllMedia -> {
                                // Respect media type filter
                                withContext(Dispatchers.Default) {
                                    when (myAppState.mediaTypeFilter) {
                                        com.example.nkdsify.data.MediaTypeFilter.PHOTOS -> myAppState.allMedia.filter { !it.isVideo }
                                        com.example.nkdsify.data.MediaTypeFilter.VIDEOS -> myAppState.allMedia.filter { it.isVideo }
                                        else -> myAppState.allMedia
                                    }
                                }
                            }
                            is Screen.Folders -> myAppState.allMedia
                            is Screen.Favorites -> {
                                if (screen.openAlbumName != null) {
                                    val taggedAlbums = myAppState.favoriteItems
                                        .flatMap { item -> (myAppState.tags[item.absolutePath] ?: emptySet()).map { tag -> tag to item } }
                                        .groupBy({ it.first }, { it.second })
                                    if (screen.openAlbumName == allFavoritesAlbumName) myAppState.favoriteItems else taggedAlbums[screen.openAlbumName]
                                        ?: emptyList()
                                } else {
                                    myAppState.favoriteItems
                                }
                            }
                            is Screen.MediaByTag -> {
                                // Items matching the opened tag
                                myAppState.allMedia.filter { item ->
                                    val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()
                                    itemTags.contains(screen.tag)
                                }
                            }
                            else -> emptyList()
                        }

                        // If search is active, further filter itemsToShuffle by parsed query (supports +tag/-tag and plain terms)
                        val filtered = if (myAppState.isSearchActive && myAppState.searchQuery.isNotBlank()) {
                            val parsed = parseQueryString(myAppState.searchQuery)
                            itemsToShuffle.filter { item ->
                                val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()
                                // selectedDate filter
                                myAppState.selectedDate?.let {
                                    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = it }
                                    val itemCalendar = java.util.Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                                    if (!(calendar.get(java.util.Calendar.YEAR) == itemCalendar.get(java.util.Calendar.YEAR) &&
                                                calendar.get(java.util.Calendar.DAY_OF_YEAR) == itemCalendar.get(java.util.Calendar.DAY_OF_YEAR))) return@filter false
                                }

                                // Tag includes/excludes
                                if (parsed.includedTags.isNotEmpty() && !itemTags.containsAll(parsed.includedTags)) return@filter false
                                if (parsed.excludedTags.isNotEmpty() && itemTags.any { it in parsed.excludedTags }) return@filter false

                                // Text terms
                                if (parsed.searchTerms.isNotEmpty()) {
                                    val name = item.name.lowercase()
                                    if (!parsed.searchTerms.all { name.contains(it.lowercase()) }) return@filter false
                                }

                                true
                            }
                        } else {
                            // also apply selectedDate even when search not active
                            if (myAppState.selectedDate != null) {
                                itemsToShuffle.filter { item ->
                                    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = myAppState.selectedDate!! }
                                    val itemCalendar = java.util.Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                                    calendar.get(java.util.Calendar.YEAR) == itemCalendar.get(java.util.Calendar.YEAR) &&
                                            calendar.get(java.util.Calendar.DAY_OF_YEAR) == itemCalendar.get(java.util.Calendar.DAY_OF_YEAR)
                                }
                            } else itemsToShuffle
                        }

                        if (filtered.isNotEmpty()) {
                            val shuffledItems = withContext(Dispatchers.Default) { filtered.shuffled() }
                            myAppState.viewerState = MediaViewerState(items = shuffledItems.toImmutableList(), startIndex = 0)
                        }
                    }
                }
                FabAction.CAMERA -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        if (useLargeFab) {
            LargeFloatingActionButton(
                onClick = onClick,
                modifier = Modifier.scale(scale),
                interactionSource = interactionSource
            ) {
                when (myAppState.selectedFabAction) {
                    FabAction.SHUFFLE -> Icon(Icons.Filled.Photo, contentDescription = stringResource(id = R.string.content_description_shuffle_play), modifier = Modifier.size(40.dp))
                    FabAction.CAMERA -> Icon(Icons.Filled.Camera, contentDescription = "Open Camera", modifier = Modifier.size(40.dp))
                }
            }
        } else {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier.scale(scale),
                interactionSource = interactionSource
            ) {
                when (myAppState.selectedFabAction) {
                    FabAction.SHUFFLE -> Icon(Icons.Filled.Photo, contentDescription = stringResource(id = R.string.content_description_shuffle_play), modifier = Modifier.size(24.dp))
                    FabAction.CAMERA -> Icon(Icons.Filled.Camera, contentDescription = "Open Camera", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
