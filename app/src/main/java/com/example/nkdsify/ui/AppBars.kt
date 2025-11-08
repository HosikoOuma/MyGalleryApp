package com.example.nkdsify.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.SortType
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.nkdsify.R
import com.example.nkdsify.ui.utils.performVibration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isSelectionMode: Boolean,
    selectedItems: List<Uri>,
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
    onCloseSearch: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterByDateClick: () -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onReverseSort: () -> Unit,
    selectedDate: Long?,
    onResetDateFilter: () -> Unit,
    onDetailsClick: () -> Unit,
    context: Context,
    isVibrationEnabled: Boolean
) {
    if (isSelectionMode) {
        TopAppBar(
            title = { Text("${selectedItems.size} selected") },
            navigationIcon = {
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onCloseSelection()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close selection")
                }
            },
            actions = {
                if (currentScreen is Screen.Trash) {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onSelectAll()
                    }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onRestore()
                    }) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                    }
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onDeletePermanently()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Permanently")
                    }
                } else {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onEditTags()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Edit Tags")
                    }
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onShare()
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onTrash()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onToggleFavorite()
                    }) {
                        Icon(
                            imageVector = if (isFavoritesScreen) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                            contentDescription = if (isFavoritesScreen) "Remove from Favorites" else "Add to Favorites"
                        )
                    }
                }
            }
        )
    } else {
        TopAppBar(
            title = {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(title)
                }
            },
            modifier = Modifier.statusBarsPadding(),
            navigationIcon = {
                if (currentScreen is Screen.FolderContent || currentScreen is Screen.TagManagement || currentScreen is Screen.AllMedia || (currentScreen is Screen.Favorites && currentScreen.openAlbumName != null)) {
                    IconButton(onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                if (currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement) {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onCloseSearch()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onSearchClick()
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        var menuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            onFilterByDateClick()
                        }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Filter by date")
                        }

                        Box {
                            IconButton(onClick = { 
                                if (isVibrationEnabled) performVibration(context)
                                menuExpanded = true 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("By Date Modified") },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.DATE_MODIFIED); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("By Date Added") },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.DATE_ADDED); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("By Alphabet") },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.ALPHABET); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("By Size") },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onSortTypeChange(SortType.SIZE); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reverse") },
                                    trailingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = "Reverse Sort") },
                                    onClick = { if (isVibrationEnabled) performVibration(context); onReverseSort(); menuExpanded = false }
                                )
                                if (selectedDate != null) {
                                    DropdownMenuItem(
                                        text = { Text("Reset Date Filter") },
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
                                Icon(Icons.Filled.Info, contentDescription = "Details")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun BottomBar(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    context: Context,
    onSettingsClick: () -> Unit,
    isVibrationEnabled: Boolean
) {
    var lastTap by rememberSaveable { mutableLongStateOf(0L) }
    var tapCount by rememberSaveable { mutableIntStateOf(0) }
    
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentScreen is Screen.Settings,
            onClick = { if (isVibrationEnabled) performVibration(context); onSettingsClick() }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Delete, contentDescription = "Trash") },
            label = { Text("Trash") },
            selected = currentScreen is Screen.Trash,
            onClick = { if (isVibrationEnabled) performVibration(context); onScreenChange(Screen.Trash) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "Folders") },
            label = { Text("Folders") },
            selected = currentScreen is Screen.Folders || currentScreen is Screen.FolderContent,
            onClick = { if (isVibrationEnabled) performVibration(context); onScreenChange(Screen.Folders) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PermMedia, contentDescription = "All Media") },
            label = { Text("All Media") },
            selected = currentScreen is Screen.AllMedia,
            onClick = { if (isVibrationEnabled) performVibration(context); onScreenChange(Screen.AllMedia) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") },
            selected = currentScreen is Screen.Favorites,
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onScreenChange(Screen.Favorites())
                val now = System.currentTimeMillis()
                if (now - lastTap < 500) {
                    tapCount++
                } else {
                    tapCount = 1
                }
                lastTap = now

                if (tapCount == 10) {
                    if (isVibrationEnabled) performVibration(context)
                    tapCount = 0
                    Toast.makeText(context, "UwU", Toast.LENGTH_SHORT).show()
                    val mediaPlayer = MediaPlayer.create(context, R.raw.uwu)
                    mediaPlayer.setOnCompletionListener { it.release() }
                    mediaPlayer.start()
                }
            }
        )
    }
}
