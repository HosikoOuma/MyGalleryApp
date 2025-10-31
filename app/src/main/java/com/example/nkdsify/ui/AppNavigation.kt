package com.example.nkdsify.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.components.MediaGrid
import com.example.nkdsify.ui.screens.FavoritesScreen
import com.example.nkdsify.ui.screens.FoldersGrid
import com.example.nkdsify.ui.screens.SettingsScreen
import com.example.nkdsify.ui.screens.TagManagementScreen
import com.example.nkdsify.ui.screens.TrashScreen
import com.example.nkdsify.ui.utils.VibrationStrength

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun AppNavigation(
    currentScreen: Screen,
    allFolders: List<MediaFolder>,
    hiddenFolders: Set<String>,
    searchQuery: String,
    isSearchActive: Boolean,
    favoriteItems: List<MediaItem>,
    allMedia: List<MediaItem>,
    imageLoader: ImageLoader,
    onFolderClick: (MediaFolder) -> Unit,
    isBlurEnabled: Boolean,
    foldersGridState: LazyGridState,
    favorites: MutableList<Uri>,
    selectedItems: MutableList<Uri>,
    setViewerState: (MediaViewerState) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    tags: Map<String, Set<String>>,
    favoritesGridState: LazyGridState,
    onClearSelection: () -> Unit,
    onClearSearch: () -> Unit,
    onTrashBlurEnabledChange: (Boolean) -> Unit,
    isTrashBlurEnabled: Boolean,
    onMuteVideoByDefaultChange: (Boolean) -> Unit,
    isMuteVideoByDefault: Boolean,
    onEasterEggClick: () -> Unit,
    selectedTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    onManageHiddenFoldersClick: () -> Unit,
    selectedZoomType: ZoomType,
    onZoomTypeChange: (ZoomType) -> Unit,
    onManageTagsClick: () -> Unit,
    onBackupAndRestoreClick: () -> Unit,
    onDeleteTag: (String) -> Unit,
    onEditTag: (String, String) -> Unit,
    trashedItems: List<MediaItem>,
    onClearTrash: () -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    isBlurAllMediaEnabled: Boolean,
    onBlurAllMediaEnabledChange: (Boolean) -> Unit,
    selectedVibrationStrength: VibrationStrength,
    onVibrationStrengthChange: (VibrationStrength) -> Unit,
    onOpenAlbum: (String) -> Unit,
    isShowFileCountEnabled: Boolean,
    onShowFileCountChange: (Boolean) -> Unit
) {
    val visibleFolders = remember(allFolders, hiddenFolders, isSearchActive, searchQuery) {
        val folders = allFolders.filterNot { hiddenFolders.contains(it.id.toString()) }
        if (isSearchActive && searchQuery.isNotEmpty()) {
            folders.mapNotNull { folder ->
                val filteredItems = folder.items.filter { it.name.contains(searchQuery, ignoreCase = true) }
                if (filteredItems.isNotEmpty()) {
                    folder.copy(items = filteredItems)
                } else {
                    null
                }
            }
        } else {
            folders
        }
    }

    val filteredFavoriteItems = remember(favoriteItems, isSearchActive, searchQuery) {
        if (isSearchActive && searchQuery.isNotEmpty()) {
            favoriteItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            favoriteItems
        }
    }

    val filteredAllMedia = remember(allMedia, isSearchActive, searchQuery) {
        if (isSearchActive && searchQuery.isNotEmpty()) {
            allMedia.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            allMedia
        }
    }

    AnimatedContent(targetState = currentScreen, transitionSpec = {
        if ((targetState is Screen.FolderContent && initialState is Screen.Folders) || (targetState is Screen.Favorites && (initialState as? Screen.Favorites)?.openAlbumName == null)) {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it } + fadeOut()
        } else if ((targetState is Screen.Folders && initialState is Screen.FolderContent) || (targetState is Screen.Favorites && (targetState as? Screen.Favorites)?.openAlbumName == null)) {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it } + fadeOut()
        } else if (targetState is Screen.TagManagement && initialState is Screen.Settings) {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it } + fadeOut()
        } else if (targetState is Screen.Settings && initialState is Screen.TagManagement) {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it } + fadeOut()
        } else {
            fadeIn() togetherWith fadeOut()
        }
    }, label = "Screen Animation") { screen ->
        when (screen) {
            is Screen.Folders -> FoldersGrid(
                folders = visibleFolders,
                imageLoader = imageLoader,
                onFolderClick = {
                    keyboardController?.hide()
                    onFolderClick(it)
                },
                isBlurEnabled = isBlurEnabled,
                gridState = foldersGridState,
                isShowFileCountEnabled = isShowFileCountEnabled
            )

            is Screen.FolderContent -> {
                val folder = allFolders.find { it.id == screen.folder.id } ?: screen.folder
                val items = if (isSearchActive && searchQuery.isNotEmpty()) {
                    folder.items.filter { it.name.contains(searchQuery, ignoreCase = true) }
                } else {
                    folder.items
                }
                MediaGrid(
                    items = items,
                    favorites = favorites,
                    selectedItems = selectedItems,
                    imageLoader = imageLoader,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        setViewerState(MediaViewerState(items = items, startIndex = items.indexOf(item)))
                    },
                    onToggleSelection = { item ->
                        if (selectedItems.contains(item.uri)) {
                            selectedItems.remove(item.uri)
                        } else {
                            selectedItems.add(item.uri)
                        }
                    }
                )
            }

            is Screen.Favorites -> {
                FavoritesScreen(
                    items = filteredFavoriteItems,
                    favorites = favorites,
                    selectedItems = selectedItems,
                    imageLoader = imageLoader,
                    tags = tags,
                    onItemClick = { items, item ->
                        keyboardController?.hide()
                        setViewerState(MediaViewerState(items = items, startIndex = items.indexOf(item)))
                    },
                    onToggleSelection = { item ->
                        if (selectedItems.contains(item.uri)) {
                            selectedItems.remove(item.uri)
                        } else {
                            selectedItems.add(item.uri)
                        }
                    },
                    isBlurEnabled = isBlurEnabled,
                    gridState = favoritesGridState,
                    openAlbumName = screen.openAlbumName,
                    onOpenAlbum = onOpenAlbum,
                    isShowFileCountEnabled = isShowFileCountEnabled
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    isBlurEnabled = isBlurEnabled,
                    onBlurEnabledChange = onBlurEnabledChange,
                    isTrashBlurEnabled = isTrashBlurEnabled,
                    onTrashBlurEnabledChange = onTrashBlurEnabledChange,
                    isMuteVideoByDefault = isMuteVideoByDefault,
                    onMuteVideoByDefaultChange = onMuteVideoByDefaultChange,
                    isBlurAllMediaEnabled = isBlurAllMediaEnabled,
                    onBlurAllMediaEnabledChange = onBlurAllMediaEnabledChange,
                    onEasterEggClick = onEasterEggClick,
                    selectedTheme = selectedTheme,
                    onThemeChange = onThemeChange,
                    onManageHiddenFoldersClick = onManageHiddenFoldersClick,
                    selectedZoomType = selectedZoomType,
                    onZoomTypeChange = onZoomTypeChange,
                    onManageTagsClick = onManageTagsClick,
                    onBackupAndRestoreClick = onBackupAndRestoreClick,
                    selectedVibrationStrength = selectedVibrationStrength,
                    onVibrationStrengthChange = onVibrationStrengthChange,
                    isShowFileCountEnabled = isShowFileCountEnabled,
                    onShowFileCountChange = onShowFileCountChange
                )
            }

            is Screen.TagManagement -> {
                TagManagementScreen(
                    allTags = tags.values.flatten().toSet(),
                    onDeleteTag = onDeleteTag,
                    onEditTag = onEditTag
                )
            }

            is Screen.Trash -> {
                TrashScreen(
                    items = trashedItems,
                    selectedItems = selectedItems.toList(),
                    imageLoader = imageLoader,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        setViewerState(MediaViewerState(items = trashedItems, startIndex = trashedItems.indexOf(item)))
                    },
                    onToggleSelection = { item ->
                        if (selectedItems.contains(item.uri)) {
                            selectedItems.remove(item.uri)
                        } else {
                            selectedItems.add(item.uri)
                        }
                    },
                    onClearTrash = onClearTrash,
                    isTrashBlurEnabled = isTrashBlurEnabled
                )
            }
            is Screen.AllMedia -> {
                MediaGrid(
                    items = filteredAllMedia,
                    favorites = favorites,
                    selectedItems = selectedItems,
                    imageLoader = imageLoader,
                    isBlurEnabled = isBlurAllMediaEnabled,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        setViewerState(MediaViewerState(items = filteredAllMedia, startIndex = filteredAllMedia.indexOf(item)))
                    },
                    onToggleSelection = { item ->
                        if (selectedItems.contains(item.uri)) {
                            selectedItems.remove(item.uri)
                        } else {
                            selectedItems.add(item.uri)
                        }
                    }
                )
            }
        }
    }
}
