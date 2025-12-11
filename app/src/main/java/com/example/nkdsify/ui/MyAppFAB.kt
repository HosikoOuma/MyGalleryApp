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

    if (myAppState.isShuffleButtonVisible && myAppState.currentScreen !is Screen.Trash
        && myAppState.currentScreen !is Screen.Settings
        && myAppState.currentScreen !is Screen.TagManagement
        && myAppState.currentScreen !is Screen.MediaByTag
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
                        val itemsToShuffle = when (val screen = myAppState.currentScreen) {
                            is Screen.FolderContent -> screen.folder.items
                            is Screen.AllMedia -> {
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
                                        .flatMap { item -> (myAppState.tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                                        .groupBy({ it.first }, { it.second })
                                    if (screen.openAlbumName == allFavoritesAlbumName) myAppState.favoriteItems else taggedAlbums[screen.openAlbumName]
                                        ?: emptyList()
                                } else {
                                    myAppState.favoriteItems
                                }
                            }
                            else -> emptyList()
                        }

                        if (itemsToShuffle.isNotEmpty()) {
                            val shuffledItems = withContext(Dispatchers.Default) {
                                itemsToShuffle.shuffled()
                            }
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
