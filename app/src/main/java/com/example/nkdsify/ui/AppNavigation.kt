package com.example.nkdsify.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
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
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.Language
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

@OptIn(ExperimentalComposeUiApi::class)
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
    isBlurInFolderEnabled: Boolean,
    onBlurInFolderEnabledChange: (Boolean) -> Unit,
    foldersGridState: LazyGridState,
    favorites: MutableList<Uri>,
    selectedItems: MutableList<Uri>,
    setViewerState: (MediaViewerState) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    tags: Map<String, Set<String>>,
    favoritesGridState: LazyGridState,
    onClearSelection: () -> Unit,
    isTrashBlurEnabled: Boolean,
    onTrashBlurEnabledChange: (Boolean) -> Unit,
    isMuteVideoByDefault: Boolean,
    onMuteVideoByDefaultChange: (Boolean) -> Unit,
    onEasterEggClick: () -> Unit,
    selectedTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    onManageHiddenFoldersClick: () -> Unit,
    selectedZoomType: ZoomType,
    onZoomTypeChange: (ZoomType) -> Unit,
    onManageTagsClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onBackupAndRestoreClick: () -> Unit,
    onDeleteTag: (String) -> Unit,
    onEditTag: (String, String) -> Unit,
    trashedItems: List<MediaItem>,
    onClearTrash: () -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    isBlurAllMediaEnabled: Boolean,
    onBlurAllMediaEnabledChange: (Boolean) -> Unit,
    isVibrationEnabled: Boolean,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onOpenAlbum: (String) -> Unit,
    isShowFileCountEnabled: Boolean,
    onShowFileCountChange: (Boolean) -> Unit,
    isShuffleButtonVisible: Boolean,
    onShuffleButtonVisibleChange: (Boolean) -> Unit,
    isShakeToBlurEnabled: Boolean,
    onShakeToBlurEnabledChange: (Boolean) -> Unit,
    isLoopVideoEnabled: Boolean,
    onLoopVideoEnabledChange: (Boolean) -> Unit,
    selectedBlurType: BlurType,
    onBlurTypeChange: (BlurType) -> Unit,
    allTags: Set<String>,
    onAddNewTag: (String) -> Unit,
    isSwipeToDismissEnabled: Boolean,
    onSwipeToDismissEnabledChange: (Boolean) -> Unit,
    useLargeFab: Boolean,
    onUseLargeFabChange: (Boolean) -> Unit,
    autoDeleteTrashEnabled: Boolean,
    onAutoDeleteTrashEnabledChange: (Boolean) -> Unit,
    autoDeleteTrashDays: Int,
    onAutoDeleteTrashDaysChange: (Int) -> Unit,
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    onCheckForUpdates: () -> Unit,
    currentVersion: String
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
        fun getScreenOrder(screen: Screen): Int = when (screen) {
            is Screen.Folders -> 0
            is Screen.AllMedia -> 1
            is Screen.Favorites -> if (screen.openAlbumName == null) 2 else 12
            is Screen.Trash -> 3
            is Screen.Settings -> 4
            is Screen.FolderContent -> 10
            is Screen.TagManagement -> 14
            is Screen.MediaByTag -> 15
        }

        val initialOrder = getScreenOrder(initialState)
        val targetOrder = getScreenOrder(targetState)

        if (targetOrder > initialOrder) {
            (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
        } else if (targetOrder < initialOrder) {
            (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
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
                isShowFileCountEnabled = isShowFileCountEnabled,
                blurType = selectedBlurType
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
                    isBlurEnabled = isBlurInFolderEnabled,
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
                    },
                    onClearSelection = onClearSelection,
                    blurType = selectedBlurType
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
                    isBlurInFolderEnabled = isBlurInFolderEnabled,
                    gridState = favoritesGridState,
                    openAlbumName = screen.openAlbumName,
                    onOpenAlbum = onOpenAlbum,
                    isShowFileCountEnabled = isShowFileCountEnabled,
                    onClearSelection = onClearSelection,
                    blurType = selectedBlurType
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    isBlurEnabled = isBlurEnabled,
                    onBlurEnabledChange = onBlurEnabledChange,
                    isBlurInFolderEnabled = isBlurInFolderEnabled,
                    onBlurInFolderEnabledChange = onBlurInFolderEnabledChange,
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
                    isVibrationEnabled = isVibrationEnabled,
                    onVibrationEnabledChange = onVibrationEnabledChange,
                    isShowFileCountEnabled = isShowFileCountEnabled,
                    onShowFileCountChange = onShowFileCountChange,
                    isShuffleButtonVisible = isShuffleButtonVisible,
                    onShuffleButtonVisibleChange = onShuffleButtonVisibleChange,
                    isShakeToBlurEnabled = isShakeToBlurEnabled,
                    onShakeToBlurEnabledChange = onShakeToBlurEnabledChange,
                    isLoopVideoEnabled = isLoopVideoEnabled,
                    onLoopVideoEnabledChange = onLoopVideoEnabledChange,
                    selectedBlurType = selectedBlurType,
                    onBlurTypeChange = onBlurTypeChange,
                    isSwipeToDismissEnabled = isSwipeToDismissEnabled,
                    onSwipeToDismissEnabledChange = onSwipeToDismissEnabledChange,
                    useLargeFab = useLargeFab,
                    onUseLargeFabChange = onUseLargeFabChange,
                    autoDeleteTrashEnabled = autoDeleteTrashEnabled,
                    onAutoDeleteTrashEnabledChange = onAutoDeleteTrashEnabledChange,
                    autoDeleteTrashDays = autoDeleteTrashDays,
                    onAutoDeleteTrashDaysChange = onAutoDeleteTrashDaysChange,
                    selectedLanguage = selectedLanguage,
                    onLanguageChange = onLanguageChange,
                    onCheckForUpdates = onCheckForUpdates,
                    currentVersion = currentVersion
                )
            }

            is Screen.TagManagement -> {
                TagManagementScreen(
                    onDeleteTag = onDeleteTag,
                    onEditTag = onEditTag,
                    onTagClick = onTagClick,
                    allTags = allTags,
                    onAddNewTag = onAddNewTag
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
                    isTrashBlurEnabled = isTrashBlurEnabled,
                    onClearSelection = onClearSelection,
                    blurType = selectedBlurType
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
                    },
                    onClearSelection = onClearSelection,
                    blurType = selectedBlurType
                )
            }
            is Screen.MediaByTag -> {
                val mediaWithTag = remember(allMedia, tags, screen.tag) {
                    val urisWithTag = tags.filter { it.value.contains(screen.tag) }.keys.asSequence().map { Uri.parse(it) }.toSet()
                    allMedia.filter { it.uri in urisWithTag }
                }
                MediaGrid(
                    items = mediaWithTag,
                    favorites = favorites,
                    selectedItems = selectedItems,
                    imageLoader = imageLoader,
                    isBlurEnabled = isBlurInFolderEnabled,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        setViewerState(MediaViewerState(items = mediaWithTag, startIndex = mediaWithTag.indexOf(item)))
                    },
                    onToggleSelection = { item ->
                        if (selectedItems.contains(item.uri)) {
                            selectedItems.remove(item.uri)
                        } else {
                            selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = onClearSelection,
                    blurType = selectedBlurType
                )
            }
        }
    }
}
