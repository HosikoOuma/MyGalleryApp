package com.example.nkdsify.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nkdsify.FileOperation
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun MyAppTopBar(
    myAppState: MyAppState,
    isVibrationEnabled: Boolean,
    title: String,
    favorites: SnapshotStateList<Uri>,
    context: Context
) {
    TopBar(
        onMediaTypeFilterChange = { myAppState.mediaTypeFilter = it },
        mediaTypeFilter = myAppState.mediaTypeFilter,
        isSelectionMode = myAppState.isSelectionMode,
        selectedItems = myAppState.selectedItems,
        onCloseSelection = { myAppState.selectedItems.clear() },
        currentScreen = myAppState.currentScreen,
        onSelectAll = {
            val currentItemsUris = when (val screen = myAppState.currentScreen) {
                is Screen.FolderContent -> screen.folder.items.map { it.uri }
                is Screen.Favorites -> {
                    if (screen.openAlbumName != null) {
                        val taggedAlbums = myAppState.favoriteItems
                            .flatMap { item -> (myAppState.tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                            .groupBy({ it.first }, { it.second })
                        taggedAlbums[screen.openAlbumName]?.map { it.uri } ?: emptyList()
                    } else {
                        myAppState.favoriteItems.map { it.uri }
                    }
                }
                is Screen.MediaByTag -> {
                    val urisWithTag = myAppState.tags.filter { it.value.contains(screen.tag) }.keys.map { Uri.parse(it) }.toSet()
                    myAppState.allMedia.filter { it.uri in urisWithTag }.map { it.uri }
                }
                is Screen.Trash -> myAppState.trashedItems.map { it.uri }
                else -> emptyList()
            }.distinct()

            if (myAppState.selectedItems.size == currentItemsUris.size && myAppState.selectedItems.containsAll(currentItemsUris)) {
                myAppState.selectedItems.clear()
            } else {
                myAppState.selectedItems.clear()
                myAppState.selectedItems.addAll(currentItemsUris)
            }
        },
        onRestore = {
            myAppState.itemsToRestore = myAppState.selectedItems.toList()
            myAppState.showConfirmRestoreDialog = true
        },
        onDeletePermanently = {
            myAppState.itemsToDelete = myAppState.selectedItems.toList()
            myAppState.showConfirmDeleteDialog = true
        },
        onEditTags = { myAppState.showBulkTagDialog = true },
        onShare = {
            val currentSelected = myAppState.selectedItems.toList()
            val uris = ArrayList(currentSelected)
            if (uris.isEmpty()) {
                return@TopBar
            }

            val intent = Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (uris.size == 1) {
                val uri = uris.first()
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.type = context.contentResolver.getType(uri) ?: "*/*"
            } else {
                val areAllImages = uris.all { uri -> context.contentResolver.getType(uri)?.startsWith("image/") == true }
                val areAllVideos = uris.all { uri -> context.contentResolver.getType(uri)?.startsWith("video/") == true }

                intent.action = Intent.ACTION_SEND_MULTIPLE
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                intent.type = when {
                    areAllImages -> "image/*"
                    areAllVideos -> "video/*"
                    else -> "*/*"
                }
            }
            context.startActivity(Intent.createChooser(intent, null))
        },
        onTrash = {
            myAppState.itemsToTrash = myAppState.selectedItems.toList()
            myAppState.showConfirmTrashDialog = true
        },
        onToggleFavorite = {
            if (isVibrationEnabled) {
                performVibration(context)
            }
            val selectedUris = myAppState.selectedItems.toList()
            selectedUris.forEach { uri ->
                if (favorites.contains(uri)) {
                    favorites.remove(uri)
                } else {
                    favorites.add(uri)
                }
            }
        },
        isFavoritesScreen = myAppState.currentScreen is Screen.Favorites,
        isSearchActive = myAppState.isSearchActive,
        searchQuery = myAppState.searchQuery,
        onSearchQueryChange = { myAppState.searchQuery = it },
        title = title,
        onBackClick = { myAppState.currentScreen = Screen.Folders },
        onBackClickS = { myAppState.currentScreen = Screen.Settings },
        onBackClickTM = { myAppState.currentScreen = Screen.TagManagement },
        onCloseSearch = {
            myAppState.isSearchActive = false
            myAppState.searchQuery = ""
        },
        onSearchClick = { myAppState.isSearchActive = true },
        onFilterByDateClick = { myAppState.showDatePicker = true },
        onSortTypeChange = { myAppState.sortType = it },
        onReverseSort = { myAppState.sortAscending = !myAppState.sortAscending },
        selectedDate = myAppState.selectedDate,
        onResetDateFilter = { myAppState.selectedDate = null },
        onDetailsClick = { myAppState.showAlbumDetailsDialog = true },
        context = context,
        isVibrationEnabled = isVibrationEnabled,
        onCopy = {
            myAppState.filesToProcess = myAppState.selectedItems.toList()
            myAppState.currentFileOperation = FileOperation.COPY
            myAppState.showFolderSelectionDialog = true
        },
        onMove = {
            myAppState.filesToProcess = myAppState.selectedItems.toList()
            myAppState.currentFileOperation = FileOperation.MOVE
            myAppState.showFolderSelectionDialog = true
        },
        onMoveToSecret = {
            myAppState.showConfirmMoveToSecretDialog = true
        },
        onRestoreFromSecret = {
            myAppState.itemsToRestoreFromSecret = myAppState.selectedItems.toList()
            myAppState.showConfirmRestoreFromSecretDialog = true
        },
        onDeleteFromSecret = {
            myAppState.itemsToDeleteFromSecret = myAppState.selectedItems.toList()
            myAppState.showConfirmDeleteFromSecretDialog = true
        },
        onAddNewTag = {
            myAppState.showAddDialog = true
        },
        onSelectionDetailsClick = {
            if (myAppState.selectedItems.size == 1 && myAppState.currentScreen !is Screen.SecretStorage) {
                myAppState.showDetailsDialog = myAppState.selectedItems.first()
            } else if (myAppState.selectedItems.isNotEmpty()) {
                val count = myAppState.selectedItems.size
                val totalSize = myAppState.selectedItems.sumOf { getMediaDetails(context, it)?.size ?: 0L }
                val formattedSize = android.text.format.Formatter.formatShortFileSize(context, totalSize)
                myAppState.selectionDetails = context.getString(R.string.selection_details_text, count, formattedSize)
                myAppState.showSelectionDetailsDialog = true
            }
        },
        onClearHistoryClick = {
            myAppState.showClearHistoryDialog = true
        }
    )
}
