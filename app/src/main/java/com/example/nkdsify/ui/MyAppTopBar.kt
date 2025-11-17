package com.example.nkdsify.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nkdsify.FileOperation
import com.example.nkdsify.MyAppState
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
            val allUris = myAppState.trashedItems.map { it.uri }
            if (myAppState.selectedItems.containsAll(allUris)) {
                myAppState.selectedItems.removeAll(allUris)
            } else {
                myAppState.selectedItems.addAll(allUris)
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
            myAppState.selectedItems.clear()
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(currentSelected))
                type = "*/*"
            }
            context.startActivity(Intent.createChooser(shareIntent, null))
        },
        onTrash = {
            myAppState.itemsToTrash = myAppState.selectedItems.toList()
            myAppState.showConfirmTrashDialog = true
        },
        onToggleFavorite = {
            if (isVibrationEnabled) {
                performVibration(context)
            }
            if (myAppState.currentScreen is com.example.nkdsify.data.Screen.Favorites) {
                val urisToUnfavorite = myAppState.selectedItems.toList()
                favorites.removeAll(urisToUnfavorite.toSet())
                myAppState.favoriteItems = myAppState.favoriteItems.filterNot { it.uri in urisToUnfavorite.toSet() }
            } else {
                val urisToAdd = myAppState.selectedItems.filterNot { favorites.contains(it) }
                if (urisToAdd.isNotEmpty()) {
                    favorites.addAll(urisToAdd)
                }
            }
            myAppState.selectedItems.clear()
        },
        isFavoritesScreen = myAppState.currentScreen is com.example.nkdsify.data.Screen.Favorites,
        isSearchActive = myAppState.isSearchActive,
        searchQuery = myAppState.searchQuery,
        onSearchQueryChange = { myAppState.searchQuery = it },
        title = title,
        onBackClick = { myAppState.currentScreen = com.example.nkdsify.data.Screen.Folders },
        onBackClickS = { myAppState.currentScreen = com.example.nkdsify.data.Screen.Settings },
        onBackClickTM = { myAppState.currentScreen = com.example.nkdsify.data.Screen.TagManagement },
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
            myAppState.selectedItems.clear()
        },
        onMove = {
            myAppState.filesToProcess = myAppState.selectedItems.toList()
            myAppState.currentFileOperation = FileOperation.MOVE
            myAppState.showFolderSelectionDialog = true
            myAppState.selectedItems.clear()
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
        }
    )
}
