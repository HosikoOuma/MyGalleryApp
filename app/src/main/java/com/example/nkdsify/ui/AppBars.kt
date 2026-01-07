package com.example.nkdsify.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Label
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    context: Context,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val isSelectionMode = myAppState.isSelectionMode
    val selectedItems = myAppState.selectedItems
    val currentScreen = myAppState.currentScreen
    val albumNameAllFavorites = stringResource(id = R.string.album_name_all_favorites)

    val actionAlpha by remember(scrollBehavior.state.collapsedFraction) {
        derivedStateOf {
            if (scrollBehavior.state.collapsedFraction > 0.5f) {
                (scrollBehavior.state.collapsedFraction - 0.5f) * 2f
            } else {
                0f
            }
        }
    }

    val appBarColors = TopAppBarDefaults.largeTopAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    @Composable
    fun TopBarBubbleAction(
        icon: ImageVector? = null,
        onClick: () -> Unit,
        contentDescription: String? = null,
        tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        forceVisible: Boolean = false,
        content: @Composable (BoxScope.() -> Unit)? = null
    ) {
        val alphaVal = if (forceVisible) 1f else actionAlpha
        if (alphaVal > 0.01f) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(40.dp)
                    .graphicsLayer {
                        alpha = alphaVal
                        scaleX = if (forceVisible) 1f else 0.8f + (0.2f * alphaVal)
                        scaleY = if (forceVisible) 1f else 0.8f + (0.2f * alphaVal)
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f * alphaVal),
                tonalElevation = 2.dp
            ) {
                IconButton(onClick = onClick) {
                    if (content != null) {
                        Box(contentAlignment = Alignment.Center) { content() }
                    } else if (icon != null) {
                        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    val toggleSelectAll: () -> Unit = {
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
                folder.items.filter { matchesParsedQuery(it, myAppState.searchQuery) }.map { it.uri }
            }
            is Screen.AllMedia -> {
                val base = when (myAppState.mediaTypeFilter) {
                    MediaTypeFilter.PHOTOS -> myAppState.allMedia.filter { !it.isVideo }
                    MediaTypeFilter.VIDEOS -> myAppState.allMedia.filter { it.isVideo }
                    else -> myAppState.allMedia
                }
                base.filter { matchesParsedQuery(it, myAppState.searchQuery) }.map { it.uri }
            }
            is Screen.Favorites -> {
                val albumItems = if (screen.openAlbumName == null || screen.openAlbumName == albumNameAllFavorites) myAppState.favoriteItems 
                                else myAppState.favoriteItems.filter { (myAppState.tags[it.absolutePath] ?: emptySet()).contains(screen.openAlbumName) }
                albumItems.filter { matchesParsedQuery(it, myAppState.searchQuery) }.map { it.uri }
            }
            is Screen.Trash -> myAppState.trashedItems.map { it.uri }
            is Screen.ViewHistory -> myAppState.filteredViewHistory.map { it.uri }
            is Screen.MediaByTag -> myAppState.allMedia.filter { (myAppState.tags[it.absolutePath] ?: emptySet()).contains(screen.tag) }.map { it.uri }
            is Screen.SecretStorage -> myAppState.secretItems.map { it.uri }
            else -> emptyList()
        }

        if (myAppState.selectedItems.size == currentItemsUris.size) {
            myAppState.selectedItems.clear()
        } else {
            myAppState.selectedItems.clear()
            myAppState.selectedItems.addAll(currentItemsUris)
        }
    }

    val showSelectionDetails: () -> Unit = {
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

    val performCopy: () -> Unit = {
        myAppState.filesToProcess = myAppState.selectedItems.toImmutableList()
        myAppState.currentFileOperation = com.example.nkdsify.FileOperation.COPY
        myAppState.showFolderSelectionDialog = true
    }

    val performMove: () -> Unit = {
        myAppState.filesToProcess = myAppState.selectedItems.toImmutableList()
        myAppState.currentFileOperation = com.example.nkdsify.FileOperation.MOVE
        myAppState.showFolderSelectionDialog = true
    }

    val performMoveToSecret: () -> Unit = {
        myAppState.showConfirmMoveToSecretDialog = true
    }

    if (isSelectionMode) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.selected_items_title, selectedItems.size)) },
            navigationIcon = {
                IconButton(onClick = { myAppState.selectedItems.clear() }) { Icon(Icons.Filled.Close, null) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                when (currentScreen) {
                    is Screen.Trash -> {
                        TopBarBubbleAction(icon = Icons.Default.SelectAll, onClick = toggleSelectAll, forceVisible = true)
                        TopBarBubbleAction(icon = Icons.Default.Restore, onClick = { myAppState.itemsToRestore = myAppState.selectedItems.toImmutableList(); myAppState.showConfirmRestoreDialog = true }, forceVisible = true)
                        TopBarBubbleAction(icon = Icons.Default.Delete, onClick = { myAppState.itemsToDelete = myAppState.selectedItems.toImmutableList(); myAppState.showConfirmDeleteDialog = true }, forceVisible = true)
                    }
                    is Screen.SecretStorage -> {
                        TopBarBubbleAction(icon = Icons.Default.SelectAll, onClick = toggleSelectAll, forceVisible = true)
                        TopBarBubbleAction(icon = Icons.Default.Restore, onClick = { myAppState.itemsToRestoreFromSecret = myAppState.selectedItems.toImmutableList(); myAppState.showConfirmRestoreFromSecretDialog = true }, forceVisible = true)
                        TopBarBubbleAction(icon = Icons.Default.Delete, onClick = { myAppState.itemsToDeleteFromSecret = myAppState.selectedItems.toImmutableList(); myAppState.showConfirmDeleteFromSecretDialog = true }, forceVisible = true)
                    }
                    else -> {
                        TopBarBubbleAction(icon = Icons.AutoMirrored.Filled.Label, onClick = { myAppState.showBulkTagDialog = true }, forceVisible = true)
                        TopBarBubbleAction(icon = Icons.Default.Share, onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            val uris = ArrayList(myAppState.selectedItems)
                            if (uris.isEmpty()) return@TopBarBubbleAction
                            val intent = android.content.Intent().addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            if (uris.size == 1) {
                                val uri = uris.first()
                                intent.action = android.content.Intent.ACTION_SEND
                                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                intent.type = context.contentResolver.getType(uri) ?: "*/*"
                            } else {
                                intent.action = android.content.Intent.ACTION_SEND_MULTIPLE
                                intent.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                                intent.type = "*/*"
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                        }, forceVisible = true)

                        val selectedPaths = myAppState.selectedItems.mapNotNull { uri -> myAppState.allMedia.find { it.uri == uri }?.absolutePath }
                        val areAllFavorites = selectedPaths.isNotEmpty() && selectedPaths.all { it in favorites }
                        val coroutineScope = rememberCoroutineScope()
                        val scale = remember { Animatable(1f) }

                        TopBarBubbleAction(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            if (areAllFavorites) favorites.removeAll(selectedPaths.toSet()) else selectedPaths.forEach { if (!favorites.contains(it)) favorites.add(it) }
                            coroutineScope.launch {
                                scale.animateTo(1.3f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                scale.animateTo(1f, spring())
                            }
                        }, forceVisible = true) {
                            Icon(
                                modifier = Modifier.scale(scale.value).size(20.dp),
                                imageVector = if (areAllFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (areAllFavorites) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            TopBarBubbleAction(icon = Icons.Default.MoreVert, onClick = { menuExpanded = true }, forceVisible = true)
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.select_all_content_description)) }, 
                                    onClick = { toggleSelectAll(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.details_content_description)) }, 
                                    onClick = { showSelectionDetails(); menuExpanded = false }, 
                                    leadingIcon = { Icon(Icons.Default.Info, null) }
                                )
                                // ВОССТАНОВЛЕННЫЙ ПУНКТ БЛЮРА
                                val areAllBlurred = myAppState.selectedItems.all { it.toString() in myAppState.blurredUris }
                                DropdownMenuItem(
                                    text = { Text(if (areAllBlurred) stringResource(R.string.unblur_action) else stringResource(R.string.blur_action)) },
                                    onClick = { 
                                        myAppState.toggleBlurForUris(myAppState.selectedItems)
                                        menuExpanded = false 
                                    },
                                    leadingIcon = { Icon(if (areAllBlurred) Icons.Default.BlurOff else Icons.Default.BlurOn, null) }
                                )
                                DropdownMenuItem(text = { Text(stringResource(R.string.copy_button)) }, onClick = { performCopy(); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.move_button)) }, onClick = { performMove(); menuExpanded = false }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.move_to_secret_storage_button)) }, onClick = { performMoveToSecret(); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Lock, null) })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_content_description)) }, 
                                    onClick = { 
                                        myAppState.itemsToTrash = myAppState.selectedItems.toImmutableList()
                                        myAppState.showConfirmTrashDialog = true
                                        menuExpanded = false 
                                    }, 
                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                                )
                            }
                        }
                    }
                }
            },
            windowInsets = TopAppBarDefaults.windowInsets
        )
    } else {
        if (myAppState.isSearchActive) {
            TopAppBar(
                title = {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    TextField(
                        value = myAppState.searchQuery,
                        onValueChange = { myAppState.searchQuery = it },
                        placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        visualTransformation = TagVisualTransformation(MaterialTheme.colorScheme.secondaryContainer),
                        colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                    )
                },
                navigationIcon = { IconButton(onClick = { myAppState.isSearchActive = false; myAppState.searchQuery = "" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { myAppState.searchQuery = "" }) { Icon(Icons.Filled.Close, null) } },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        } else {
            LargeTopAppBar(
                title = { Text(title) },
                modifier = Modifier.fillMaxWidth(),
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = appBarColors,
                navigationIcon = {
                    val currentScreen = myAppState.currentScreen
                    val showBack = currentScreen is Screen.FolderContent || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null) || currentScreen is Screen.ViewHistory || currentScreen is Screen.TagManagement || currentScreen is Screen.SecretStorage || currentScreen is Screen.About || currentScreen is Screen.Help || currentScreen is Screen.MediaByTag
                    if (showBack) {
                        IconButton(onClick = {
                            if (myAppState.isVibrationEnabled) performVibration(context)
                            when (currentScreen) {
                                is Screen.FolderContent, is Screen.ViewHistory -> myAppState.currentScreen = Screen.Folders
                                is Screen.TagManagement, is Screen.SecretStorage, is Screen.About, is Screen.Help -> myAppState.currentScreen = Screen.Settings
                                is Screen.MediaByTag -> myAppState.currentScreen = Screen.TagManagement
                                is Screen.Favorites -> if (currentScreen.openAlbumName != null) myAppState.currentScreen = Screen.Favorites() else myAppState.currentScreen = Screen.Folders
                                else -> {}
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        if (currentScreen is Screen.AllMedia) {
                            TopBarBubbleAction(icon = if (myAppState.mediaTypeFilter == MediaTypeFilter.PHOTOS) Icons.Default.PhotoLibrary else if (myAppState.mediaTypeFilter == MediaTypeFilter.VIDEOS) Icons.Default.VideoLibrary else Icons.Default.FilterList, 
                                onClick = { 
                                    myAppState.mediaTypeFilter = when (myAppState.mediaTypeFilter) { 
                                        MediaTypeFilter.ALL -> MediaTypeFilter.PHOTOS
                                        MediaTypeFilter.PHOTOS -> MediaTypeFilter.VIDEOS
                                        else -> MediaTypeFilter.ALL 
                                    } 
                                })
                        }
                        if (currentScreen is Screen.TagManagement) TopBarBubbleAction(icon = Icons.Default.Add, onClick = { myAppState.showAddDialog = true })
                        
                        val canSearch = currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement && currentScreen !is Screen.SecretStorage && currentScreen !is Screen.About && currentScreen !is Screen.Help && currentScreen !is Screen.Trash
                        if (canSearch) {
                            TopBarBubbleAction(icon = Icons.Filled.Search, onClick = { myAppState.isSearchActive = true })
                            if (currentScreen !is Screen.ViewHistory) {
                                if (myAppState.selectedDate != null) TopBarBubbleAction(icon = Icons.Default.EventBusy, onClick = { myAppState.selectedDate = null })
                                TopBarBubbleAction(icon = Icons.Filled.DateRange, onClick = { myAppState.showDatePicker = true })
                            }
                        }
                        if (currentScreen is Screen.ViewHistory) TopBarBubbleAction(icon = Icons.Default.Delete, onClick = { myAppState.showClearHistoryDialog = true })
                        else if (canSearch) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                TopBarBubbleAction(icon = Icons.AutoMirrored.Filled.Sort, onClick = { menuExpanded = true })
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_date_modified)) }, onClick = { myAppState.sortType = SortType.DATE_MODIFIED; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.DateRange, null) })
                                    DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_date_added)) }, onClick = { myAppState.sortType = SortType.DATE_ADDED; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.AddCircleOutline, null) })
                                    DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_alphabet)) }, onClick = { myAppState.sortType = SortType.ALPHABET; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.SortByAlpha, null) })
                                    DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_size)) }, onClick = { myAppState.sortType = SortType.SIZE; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Storage, null) })
                                    DropdownMenuItem(text = { Text(stringResource(id = R.string.reverse_sort)) }, onClick = { myAppState.sortAscending = !myAppState.sortAscending; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.SwapVert, null) })
                                }
                            }
                        }
                        if (currentScreen is Screen.FolderContent || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null)) TopBarBubbleAction(icon = Icons.Filled.Info, onClick = { myAppState.showAlbumDetailsDialog = true })
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    }
}
