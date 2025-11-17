package com.example.nkdsify.ui

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaTypeFilter
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.SortType
import com.example.nkdsify.ui.utils.performVibration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isSelectionMode: Boolean,
    selectedItems: List<android.net.Uri>,
    onCloseSelection: () -> Unit,
    currentScreen: Screen,
    onSelectAll: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    onEditTags: () -> Unit,
    onShare: () -> Unit,
    onTrash: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavoritesScreen: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    title: String,
    onBackClick: () -> Unit,
    onBackClickS: () -> Unit,
    onBackClickTM: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterByDateClick: () -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onReverseSort: () -> Unit,
    selectedDate: Long?,
    onResetDateFilter: () -> Unit,
    onDetailsClick: () -> Unit,
    context: Context,
    onAddNewTag: () -> Unit,
    isVibrationEnabled: Boolean,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onMoveToSecret: () -> Unit,
    onRestoreFromSecret: () -> Unit,
    onDeleteFromSecret: () -> Unit,
    mediaTypeFilter: MediaTypeFilter,
    onMediaTypeFilterChange: (MediaTypeFilter) -> Unit,
    onSelectionDetailsClick: () -> Unit
) {
    if (isSelectionMode) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.selected_items_title, selectedItems.size)) },
            navigationIcon = {
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onCloseSelection()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.close_selection_content_description))
                }
            },
            actions = {
                when (currentScreen) {
                    is Screen.Trash -> {
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onSelectAll()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(id = R.string.select_all_content_description))
                        }
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onRestore()
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(id = R.string.restore_content_description))
                        }
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onDeletePermanently()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_permanently_content_description))
                        }
                    }
                    is Screen.SecretStorage -> {
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onRestoreFromSecret()
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(id = R.string.restore_from_secret_storage_content_description))
                        }
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onDeleteFromSecret()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_content_description))
                        }
                    }
                    else -> {
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onEditTags()
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit_tags_content_description))
                        }
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onShare()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_content_description))
                        }
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onTrash()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_content_description))
                        }
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onToggleFavorite()
                        }) {
                            Icon(
                                imageVector = if (isFavoritesScreen) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavoritesScreen) stringResource(id = R.string.remove_from_favorites_content_description) else stringResource(id = R.string.add_to_favorites_content_description),
                                tint = if (isFavoritesScreen) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = {
                                if (isVibrationEnabled) performVibration(context)
                                menuExpanded = true
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options_content_description))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.details_content_description)) },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onSelectionDetailsClick(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = stringResource(id = R.string.details_content_description)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.copy_button)) },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onCopy(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_button)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.move_button)) },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onMove(); menuExpanded = false },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(id = R.string.move_button)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.move_to_secret_storage_button)) },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onMoveToSecret(); menuExpanded = false },
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
                onCloseSearch()
            }
        }

        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                focusRequester.requestFocus()
            }
        }

        TopAppBar(
            title = {
                AnimatedContent(targetState = isSearchActive, label = "Search bar animation") { targetState ->
                    if (targetState) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Text(title)
                    }
                }
            },
            modifier = Modifier.statusBarsPadding(),
            navigationIcon = {
                if (currentScreen is Screen.FolderContent || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null)) {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onBackClick()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_content_description)
                        )
                    }
                }
                if (currentScreen is Screen.TagManagement || currentScreen is Screen.SecretStorage) {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onBackClickS()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_content_description))
                    }
                }
                if (currentScreen is Screen.MediaByTag) {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onBackClickTM()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_content_description))
                    }
                }
            },
            actions = {
                AnimatedContent(targetState = isSearchActive, label = "Search actions animation") { targetState ->
                    if (targetState) {
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onCloseSearch()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.close_search_content_description))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentScreen is Screen.AllMedia) {
                                IconButton(onClick = {
                                    val nextFilter = when (mediaTypeFilter) {
                                        MediaTypeFilter.ALL -> MediaTypeFilter.PHOTOS
                                        MediaTypeFilter.PHOTOS -> MediaTypeFilter.VIDEOS
                                        MediaTypeFilter.VIDEOS -> MediaTypeFilter.ALL
                                    }
                                    onMediaTypeFilterChange(nextFilter)
                                }) {
                                    val icon = when (mediaTypeFilter) {
                                        MediaTypeFilter.PHOTOS -> Icons.Default.PhotoLibrary
                                        MediaTypeFilter.VIDEOS -> Icons.Default.VideoLibrary
                                        else -> Icons.Default.FilterList
                                    }
                                    Icon(icon, contentDescription = stringResource(id = R.string.filter_media_type))
                                }
                            }
                            if (currentScreen is Screen.TagManagement) {
                                IconButton(onClick = {
                                    if (isVibrationEnabled) performVibration(context)
                                    onAddNewTag()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_new_tag_content_description))
                                }
                            }

                            if (currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement && currentScreen !is Screen.SecretStorage) {

                                if (currentScreen !is Screen.Trash) {
                                    IconButton(onClick = {
                                        if (isVibrationEnabled) performVibration(context)
                                        onSearchClick()
                                    }) {
                                        Icon(Icons.Filled.Search, contentDescription = stringResource(id = R.string.search_content_description))
                                    }
                                    IconButton(onClick = {
                                        if (isVibrationEnabled) performVibration(context)
                                        onFilterByDateClick()
                                    }) {
                                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(id = R.string.filter_by_date_content_description))
                                    }
                                }

                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = {
                                        if (isVibrationEnabled) performVibration(context)
                                        menuExpanded = true
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(id = R.string.sort_by_content_description))
                                    }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_by_date_modified)) },
                                            onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.DATE_MODIFIED); menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_by_date_added)) },
                                            onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.DATE_ADDED); menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_by_alphabet)) },
                                            onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.ALPHABET); menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_by_size)) },
                                            onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.SIZE); menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.reverse_sort)) },
                                            trailingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = stringResource(id = R.string.reverse_sort_content_description)) },
                                            onClick = { if (isVibrationEnabled) performVibration(context); onReverseSort(); menuExpanded = false }
                                        )
                                        if (selectedDate != null && currentScreen !is Screen.Trash) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.reset_date_filter)) },
                                                onClick = { if (isVibrationEnabled) performVibration(context); onResetDateFilter(); menuExpanded = false }
                                            )
                                        }
                                    }
                                }

                                if (currentScreen is Screen.FolderContent || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null)) {
                                    IconButton(onClick = {
                                        if (isVibrationEnabled) performVibration(context)
                                        onDetailsClick()
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
