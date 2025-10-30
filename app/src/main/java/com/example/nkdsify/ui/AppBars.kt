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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.SortType
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.nkdsify.R

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
    onSettingsClick: () -> Unit
) {
    if (isSelectionMode) {
        TopAppBar(
            title = { Text("${selectedItems.size} selected") },
            navigationIcon = {
                IconButton(onClick = onCloseSelection) {
                    Icon(Icons.Filled.Close, contentDescription = "Close selection")
                }
            },
            actions = {
                if (currentScreen is Screen.Trash) {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                    }
                    IconButton(onClick = onDeletePermanently) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Permanently")
                    }
                } else {
                    IconButton(onClick = onEditTags) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Edit Tags")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = onToggleFavorite) {
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
                if (currentScreen is Screen.FolderContent || currentScreen is Screen.TagManagement || currentScreen is Screen.AllMedia) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                if (currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement) {
                    if (isSearchActive) {
                        IconButton(onClick = onCloseSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        var menuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = onFilterByDateClick) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Filter by date")
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("By Date Modified") },
                                    onClick = { onSortTypeChange(SortType.DATE_MODIFIED); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("By Date Added") },
                                    onClick = { onSortTypeChange(SortType.DATE_ADDED); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("By Name") },
                                    onClick = { onSortTypeChange(SortType.NAME); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reverse") },
                                    onClick = { onReverseSort(); menuExpanded = false }
                                )
                                if (selectedDate != null) {
                                    DropdownMenuItem(
                                        text = { Text("Reset Date Filter") },
                                        onClick = { onResetDateFilter(); menuExpanded = false }
                                    )
                                 }
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
    haptics: HapticFeedback,
    onScreenChange: (Screen) -> Unit,
    context: Context
) {
    var lastTap by rememberSaveable { mutableLongStateOf(0L) }
    var tapCount by rememberSaveable { mutableIntStateOf(0) }
    
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Delete, contentDescription = "Trash") },
            label = { Text("Trash") },
            selected = currentScreen is Screen.Trash,
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onScreenChange(Screen.Trash) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "Folders") },
            label = { Text("Folders") },
            selected = currentScreen is Screen.Folders || currentScreen is Screen.FolderContent,
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onScreenChange(Screen.Folders) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PermMedia, contentDescription = "All Media") },
            label = { Text("All Media") },
            selected = currentScreen is Screen.AllMedia,
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onScreenChange(Screen.AllMedia) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") },
            selected = currentScreen is Screen.Favorites,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onScreenChange(Screen.Favorites)
                val now = System.currentTimeMillis()
                if (now - lastTap < 500) {
                    tapCount++
                } else {
                    tapCount = 1
                }
                lastTap = now

                if (tapCount == 10) {
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
