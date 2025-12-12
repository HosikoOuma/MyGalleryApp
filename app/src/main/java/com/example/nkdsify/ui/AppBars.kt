package com.example.nkdsify.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.R
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaTypeFilter
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.SortType
import com.example.nkdsify.ui.components.TagVisualTransformation
import com.example.nkdsify.ui.utils.parseQueryString
import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    myAppState: MyAppState,
    favorites: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    title: String,
    context: Context
) {
    val isSelectionMode = myAppState.isSelectionMode
    val selectedItems = myAppState.selectedItems
    val currentScreen = myAppState.currentScreen
    val isFavoritesScreen = myAppState.currentScreen is Screen.Favorites
    val isSearchActive = myAppState.isSearchActive
    val selectedDate = myAppState.selectedDate
    val albumNameAllFavorites = stringResource(id = R.string.album_name_all_favorites)

    // helper: toggle select all for the current screen
    val toggleSelectAll = {
        // helper: build visible items according to the current screen and current filters (search, tags, date, mediaType)
        fun matchesParsedQuery(item: com.example.nkdsify.data.MediaItem, query: String?): Boolean {
            if (query.isNullOrBlank()) return true
            val parsed = parseQueryString(query)
            val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()

            val textMatch = parsed.searchTerms.all { term -> item.name.contains(term, ignoreCase = true) }

            val tagMatch = parsed.includedTagGroups.all { group -> group.any { tag -> itemTags.contains(tag) } } &&
                    (parsed.excludedTags.isEmpty() || !itemTags.any { it in parsed.excludedTags })

            return textMatch && tagMatch
        }

        val currentItemsUris = when (val screen = myAppState.currentScreen) {
            is Screen.FolderContent -> {
                val folder = myAppState.allFolders.find { it.id == screen.folder.id } ?: screen.folder
                val sorted = folder.items
                val filtered = if (myAppState.isSearchActive && myAppState.searchQuery.isNotBlank()) {
                    sorted.filter { matchesParsedQuery(it, myAppState.searchQuery) }
                } else sorted
                filtered.map { it.uri }
            }
            is Screen.AllMedia -> {
                val base = when (myAppState.mediaTypeFilter) {
                    MediaTypeFilter.PHOTOS -> myAppState.allMedia.filter { !it.isVideo }
                    MediaTypeFilter.VIDEOS -> myAppState.allMedia.filter { it.isVideo }
                    else -> myAppState.allMedia
                }
                val withDate = myAppState.selectedDate?.let { date ->
                    base.filter { item ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = date }
                        val ic = java.util.Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                        cal.get(java.util.Calendar.YEAR) == ic.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.DAY_OF_YEAR) == ic.get(java.util.Calendar.DAY_OF_YEAR)
                    }
                } ?: base
                val filtered = if (myAppState.isSearchActive && myAppState.searchQuery.isNotBlank()) withDate.filter { matchesParsedQuery(it, myAppState.searchQuery) } else withDate
                filtered.map { it.uri }
            }
            is Screen.Favorites -> {
                // build album contents similar to FavoritesScreen
                val taggedAlbums = myAppState.favoriteItems
                    .flatMap { item -> (myAppState.tags[item.absolutePath] ?: emptySet()).map { tag -> tag to item } }
                    .groupBy({ it.first }, { it.second })

                val albumItems = if (screen.openAlbumName != null) {
                    if (screen.openAlbumName == albumNameAllFavorites) myAppState.favoriteItems else taggedAlbums[screen.openAlbumName] ?: emptyList()
                } else myAppState.favoriteItems

                val filtered = if (myAppState.isSearchActive && myAppState.searchQuery.isNotBlank()) albumItems.filter { matchesParsedQuery(it, myAppState.searchQuery) } else albumItems
                filtered.map { it.uri }
            }
            is Screen.MediaByTag -> {
                val base = myAppState.allMedia.filter { item -> (myAppState.tags[item.absolutePath] ?: emptySet()).contains(screen.tag) }
                val filtered = if (myAppState.isSearchActive && myAppState.searchQuery.isNotBlank()) base.filter { matchesParsedQuery(it, myAppState.searchQuery) } else base
                filtered.map { it.uri }
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
    }

    val showSelectionDetails = {
        if (myAppState.selectedItems.size == 1 && myAppState.currentScreen !is Screen.SecretStorage) {
            myAppState.showDetailsDialog = myAppState.selectedItems.first()
        } else if (myAppState.selectedItems.isNotEmpty()) {
            val count = myAppState.selectedItems.size
            val totalSize = myAppState.selectedItems.sumOf { getMediaDetails(context, it)?.size ?: 0L }
            val formattedSize = android.text.format.Formatter.formatShortFileSize(context, totalSize)
            myAppState.selectionDetails = context.getString(R.string.selection_details_text, count, formattedSize)
            myAppState.showSelectionDetailsDialog = true
        }
    }

    val performCopy = {
        myAppState.filesToProcess = myAppState.selectedItems.toImmutableList()
        myAppState.currentFileOperation = com.example.nkdsify.FileOperation.COPY
        myAppState.showFolderSelectionDialog = true
    }

    val performMove = {
        myAppState.filesToProcess = myAppState.selectedItems.toImmutableList()
        myAppState.currentFileOperation = com.example.nkdsify.FileOperation.MOVE
        myAppState.showFolderSelectionDialog = true
    }

    val performMoveToSecret = {
        myAppState.showConfirmMoveToSecretDialog = true
    }

    if (isSelectionMode) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.selected_items_title, selectedItems.size)) },
            navigationIcon = {
                IconButton(onClick = {
                    if (myAppState.isVibrationEnabled) performVibration(context)
                    myAppState.selectedItems.clear()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.close_selection_content_description))
                }
            },
            actions = {
                when (currentScreen) {
                    is Screen.Trash -> {
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            toggleSelectAll()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(id = R.string.select_all_content_description))
                        }
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.itemsToRestore = myAppState.selectedItems.toImmutableList()
                            myAppState.showConfirmRestoreDialog = true
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(id = R.string.restore_content_description))
                        }
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.itemsToDelete = myAppState.selectedItems.toImmutableList()
                            myAppState.showConfirmDeleteDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_permanently_content_description))
                        }
                    }
                    is Screen.SecretStorage -> {
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.itemsToRestoreFromSecret = myAppState.selectedItems.toImmutableList()
                            myAppState.showConfirmRestoreFromSecretDialog = true
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(id = R.string.restore_from_secret_storage_content_description))
                        }
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.itemsToDeleteFromSecret = myAppState.selectedItems.toImmutableList()
                            myAppState.showConfirmDeleteFromSecretDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_content_description))
                        }
                    }
                    else -> {
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.showBulkTagDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit_tags_content_description))
                        }
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            val currentSelected = myAppState.selectedItems.toImmutableList()
                            val uris = ArrayList(currentSelected)
                            if (uris.isEmpty()) return@IconButton
                            val intent = android.content.Intent().addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            if (uris.size == 1) {
                                val uri = uris.first()
                                intent.action = android.content.Intent.ACTION_SEND
                                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                intent.type = context.contentResolver.getType(uri) ?: "*/*"
                            } else {
                                val areAllImages = uris.all { uri -> context.contentResolver.getType(uri)?.startsWith("image/") == true }
                                val areAllVideos = uris.all { uri -> context.contentResolver.getType(uri)?.startsWith("video/") == true }
                                intent.action = android.content.Intent.ACTION_SEND_MULTIPLE
                                intent.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                intent.type = when {
                                    areAllImages -> "image/*"
                                    areAllVideos -> "video/*"
                                    else -> "*/*"
                                }
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_content_description))
                        }
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.itemsToTrash = myAppState.selectedItems.toImmutableList()
                            myAppState.showConfirmTrashDialog = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_content_description))
                        }
                        val coroutineScope = rememberCoroutineScope()
                        val scale = remember { Animatable(1f) }
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            val selectedPaths = myAppState.selectedItems.mapNotNull { uri -> myAppState.allMedia.find { it.uri == uri }?.absolutePath }
                            selectedPaths.forEach { path ->
                                if (favorites.contains(path)) favorites.remove(path) else favorites.add(path)
                            }
                            coroutineScope.launch {
                                scale.animateTo(
                                    targetValue = 1.3f,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
                                )
                                scale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring()
                                )
                            }
                        }) {
                            Icon(
                                modifier = Modifier.scale(scale.value),
                                imageVector = if (isFavoritesScreen) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavoritesScreen) stringResource(id = R.string.remove_from_favorites_content_description) else stringResource(id = R.string.add_to_favorites_content_description),
                                tint = if (isFavoritesScreen) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = {
                                if (myAppState.isVibrationEnabled) performVibration(context)
                                menuExpanded = true
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options_content_description))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.select_all_content_description)) },
                                    onClick = { if (myAppState.isVibrationEnabled) performVibration(context); toggleSelectAll(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = stringResource(id = R.string.select_all_content_description)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.details_content_description)) },
                                    onClick = { if (myAppState.isVibrationEnabled) performVibration(context); showSelectionDetails(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = stringResource(id = R.string.details_content_description)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.copy_button)) },
                                    onClick = { if (myAppState.isVibrationEnabled) performVibration(context); performCopy(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_button)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.move_button)) },
                                    onClick = { if (myAppState.isVibrationEnabled) performVibration(context); performMove(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(id = R.string.move_button)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.move_to_secret_storage_button)) },
                                    onClick = { if (myAppState.isVibrationEnabled) performVibration(context); performMoveToSecret(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = stringResource(id = R.string.move_to_secret_storage_button)) }
                                )
                            }
                        }
                    }
                }
            }
        )
    } else {
        val focusRequester = remember { FocusRequester() }

        if (isSearchActive) {
            BackHandler {
                myAppState.isSearchActive = false
                myAppState.searchQuery = ""
            }
        }

        LaunchedEffect(myAppState.isSearchActive) {
            if (myAppState.isSearchActive) focusRequester.requestFocus()
        }

        TopAppBar(
            title = {
                AnimatedContent(targetState = isSearchActive, label = "Search bar animation") { targetState ->
                    if (targetState) {
                        TextField(
                            value = myAppState.searchQuery,
                            onValueChange = { myAppState.searchQuery = it },
                            placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            visualTransformation = TagVisualTransformation(MaterialTheme.colorScheme.secondaryContainer),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(title)
                    }
                }
            },
            modifier = Modifier.statusBarsPadding(),
            navigationIcon = {
                val currentScreen = myAppState.currentScreen
                if (currentScreen is Screen.FolderContent || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null) || currentScreen is Screen.ViewHistory) {
                    IconButton(onClick = {
                        if (myAppState.isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Folders
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_content_description)
                        )
                    }
                }
                if (currentScreen is Screen.TagManagement || currentScreen is Screen.SecretStorage) {
                    IconButton(onClick = {
                        if (myAppState.isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Settings
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_content_description))
                    }
                }
                if (currentScreen is Screen.MediaByTag) {
                    IconButton(onClick = {
                        if (myAppState.isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.TagManagement
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_content_description))
                    }
                }
            },
            actions = {
                AnimatedContent(targetState = isSearchActive, label = "Search actions animation") { targetState ->
                    if (targetState) {
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            myAppState.isSearchActive = false
                            myAppState.searchQuery = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.close_search_content_description))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentScreen is Screen.AllMedia) {
                                IconButton(onClick = {
                                    val nextFilter = when (myAppState.mediaTypeFilter) {
                                        MediaTypeFilter.ALL -> MediaTypeFilter.PHOTOS
                                        MediaTypeFilter.PHOTOS -> MediaTypeFilter.VIDEOS
                                        MediaTypeFilter.VIDEOS -> MediaTypeFilter.ALL
                                    }
                                    myAppState.mediaTypeFilter = nextFilter
                                }) {
                                    val icon = when (myAppState.mediaTypeFilter) {
                                        MediaTypeFilter.PHOTOS -> Icons.Default.PhotoLibrary
                                        MediaTypeFilter.VIDEOS -> Icons.Default.VideoLibrary
                                        else -> Icons.Default.FilterList
                                    }
                                    Icon(icon, contentDescription = stringResource(id = R.string.filter_media_type))
                                }
                            }
                            if (currentScreen is Screen.TagManagement) {
                                IconButton(onClick = {
                                    if (myAppState.isVibrationEnabled) performVibration(context)
                                    myAppState.showAddDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_new_tag_content_description))
                                }
                            }

                            if (currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement && currentScreen !is Screen.SecretStorage && currentScreen !is Screen.About) {

                                if (currentScreen !is Screen.Trash) {
                                    IconButton(onClick = {
                                        if (myAppState.isVibrationEnabled) performVibration(context)
                                        myAppState.isSearchActive = true
                                    }) {
                                        Icon(Icons.Filled.Search, contentDescription = stringResource(id = R.string.search_content_description))
                                    }
                                    if (currentScreen !is Screen.ViewHistory) {
                                        IconButton(onClick = {
                                            if (myAppState.isVibrationEnabled) performVibration(context)
                                            myAppState.showDatePicker = true
                                        }) {
                                            Icon(Icons.Filled.DateRange, contentDescription = stringResource(id = R.string.filter_by_date_content_description))
                                        }
                                    }
                                }

                                if (currentScreen is Screen.ViewHistory) {
                                    IconButton(onClick = {
                                        if (myAppState.isVibrationEnabled) performVibration(context)
                                        myAppState.showClearHistoryDialog = true
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.clear_history_title))
                                    }
                                } else {
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = {
                                            if (myAppState.isVibrationEnabled) performVibration(context)
                                            menuExpanded = true
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(id = R.string.sort_by_content_description))
                                        }
                                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.sort_by_date_modified)) },
                                                onClick = { if (myAppState.isVibrationEnabled) performVibration(context); myAppState.sortType = SortType.DATE_MODIFIED; menuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.sort_by_date_added)) },
                                                onClick = { if (myAppState.isVibrationEnabled) performVibration(context); myAppState.sortType = SortType.DATE_ADDED; menuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.sort_by_alphabet)) },
                                                onClick = { if (myAppState.isVibrationEnabled) performVibration(context); myAppState.sortType = SortType.ALPHABET; menuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.sort_by_size)) },
                                                onClick = { if (myAppState.isVibrationEnabled) performVibration(context); myAppState.sortType = SortType.SIZE; menuExpanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.reverse_sort)) },
                                                trailingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = stringResource(id = R.string.reverse_sort_content_description)) },
                                                onClick = { if (myAppState.isVibrationEnabled) performVibration(context); myAppState.sortAscending = !myAppState.sortAscending; menuExpanded = false }
                                            )
                                            if (selectedDate != null && currentScreen !is Screen.Trash) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(id = R.string.reset_date_filter)) },
                                                    onClick = { if (myAppState.isVibrationEnabled) performVibration(context); myAppState.selectedDate = null; menuExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (currentScreen is Screen.FolderContent || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null)) {
                                    IconButton(onClick = {
                                        if (myAppState.isVibrationEnabled) performVibration(context)
                                        myAppState.showAlbumDetailsDialog = true
                                    }) {
                                        Icon(Icons.Filled.Info, contentDescription = stringResource(id = R.string.details_content_description))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
