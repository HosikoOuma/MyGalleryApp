package com.example.nkdsify.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.components.MediaGrid
import com.example.nkdsify.ui.screens.FavoritesScreen
import com.example.nkdsify.ui.screens.FoldersGrid
import com.example.nkdsify.ui.screens.SecretStorageScreen
import com.example.nkdsify.ui.screens.SettingsScreen
import com.example.nkdsify.ui.screens.TagManagementScreen
import com.example.nkdsify.ui.screens.TrashScreen
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MyAppNavigation(
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    foldersGridState: LazyGridState,
    favoritesGridState: LazyGridState,
    favorites: MutableList<Uri>,
    keyboardController: SoftwareKeyboardController?,
    onMoveTag: (Int, Int) -> Unit,
    onAddNewTag: (String) -> Unit,
    isVibrationEnabled: Boolean,
    isBlurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
    isBlurInFolderEnabled: Boolean,
    onBlurInFolderEnabledChange: (Boolean) -> Unit,
    isTrashBlurEnabled: Boolean,
    onTrashBlurEnabledChange: (Boolean) -> Unit,
    isBlurAllMediaEnabled: Boolean,
    onBlurAllMediaEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    isShakeToBlurEnabled: Boolean,
    onShakeToBlurEnabledChange: (Boolean) -> Unit,
    isLoopVideoEnabled: Boolean,
    onLoopVideoEnabledChange: (Boolean) -> Unit,
    isSwipeToDismissEnabled: Boolean,
    onSwipeToDismissEnabledChange: (Boolean) -> Unit,
    useLargeFab: Boolean,
    onUseLargeFabChange: (Boolean) -> Unit,
    autoDeleteTrashEnabled: Boolean,
    onAutoDeleteTrashEnabledChange: (Boolean) -> Unit,
    autoDeleteTrashDays: Int,
    onAutoDeleteTrashDaysChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val visibleFolders = remember(myAppState.allFolders, myAppState.hiddenFolders, myAppState.isSearchActive, myAppState.searchQuery) {
        val folders = myAppState.allFolders.filterNot { myAppState.hiddenFolders.contains(it.id.toString()) }
        if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
            folders.mapNotNull { folder ->
                val filteredItems = folder.items.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }
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

    val filteredFavoriteItems = remember(myAppState.favoriteItems, myAppState.isSearchActive, myAppState.searchQuery) {
        if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
            myAppState.favoriteItems.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }
        } else {
            myAppState.favoriteItems
        }
    }

    val filteredAllMedia = remember(myAppState.allMedia, myAppState.isSearchActive, myAppState.searchQuery) {
        if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
            myAppState.allMedia.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }
        } else {
            myAppState.allMedia
        }
    }

    AnimatedContent(targetState = myAppState.currentScreen, transitionSpec = {
        fun getScreenOrder(screen: Screen): Int = when (screen) {
            is Screen.Folders -> 0
            is Screen.AllMedia -> 1
            is Screen.Favorites -> if (screen.openAlbumName == null) 2 else 12
            is Screen.Trash -> 3
            is Screen.Settings -> 4
            is Screen.SecretStorage -> 5
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
                onFolderClick = { folder ->
                    keyboardController?.hide()
                    myAppState.currentScreen = Screen.FolderContent(folder)
                },
                isBlurEnabled = isBlurEnabled,
                gridState = foldersGridState,
                isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
                blurType = myAppState.selectedBlurType
            )

            is Screen.FolderContent -> {
                val folder = myAppState.allFolders.find { it.id == screen.folder.id } ?: screen.folder
                val items = if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
                    folder.items.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }
                } else {
                    folder.items
                }
                MediaGrid(
                    items = items,
                    favorites = favorites,
                    selectedItems = myAppState.selectedItems,
                    imageLoader = imageLoader,
                    isBlurEnabled = isBlurInFolderEnabled,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        myAppState.viewerState = MediaViewerState(items = items, startIndex = items.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType
                )
            }

            is Screen.Favorites -> {
                FavoritesScreen(
                    items = filteredFavoriteItems,
                    favorites = favorites,
                    selectedItems = myAppState.selectedItems,
                    imageLoader = imageLoader,
                    tags = myAppState.tags,
                    onItemClick = { items, item ->
                        keyboardController?.hide()
                        myAppState.viewerState = MediaViewerState(items = items, startIndex = items.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    isBlurEnabled = isBlurEnabled,
                    isBlurInFolderEnabled = isBlurInFolderEnabled,
                    gridState = favoritesGridState,
                    openAlbumName = screen.openAlbumName,
                    onOpenAlbum = { albumName ->
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Favorites(openAlbumName = albumName)
                    },
                    isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    isBlurEnabled = isBlurEnabled,
                    onBlurEnabledChange = {
                        onBlurEnabledChange(it)
                        SettingsRepository.setBlurEnabled(context, it)
                    },
                    isBlurInFolderEnabled = isBlurInFolderEnabled,
                    onBlurInFolderEnabledChange = {
                        onBlurInFolderEnabledChange(it)
                        SettingsRepository.setBlurInFolderEnabled(context, it)
                    },
                    isTrashBlurEnabled = isTrashBlurEnabled,
                    onTrashBlurEnabledChange = {
                        onTrashBlurEnabledChange(it)
                        SettingsRepository.setTrashBlurEnabled(context, it)
                    },
                    isMuteVideoByDefault = myAppState.isMuteVideoByDefault,
                    onMuteVideoByDefaultChange = {
                        myAppState.isMuteVideoByDefault = it
                        SettingsRepository.setMuteVideoByDefault(context, it)
                    },
                    isBlurAllMediaEnabled = isBlurAllMediaEnabled,
                    onBlurAllMediaEnabledChange = {
                        onBlurAllMediaEnabledChange(it)
                        SettingsRepository.setBlurAllMediaEnabled(context, it)
                    },
                    onEasterEggClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.easterEggTapCount++
                        if (myAppState.easterEggTapCount == 10) {
                            myAppState.easterEggTapCount = 0
                            myAppState.showEasterEggDialog = true
                            val mediaPlayer = android.media.MediaPlayer.create(context, com.example.nkdsify.R.raw.uwu)
                            mediaPlayer.setOnCompletionListener { it.release() }
                            mediaPlayer.start()
                        }
                    },
                    selectedTheme = myAppState.selectedTheme,
                    onThemeChange = { theme ->
                        myAppState.selectedTheme = theme
                        SettingsRepository.setTheme(context, theme)
                    },
                    onManageHiddenFoldersClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.showHiddenFoldersDialog = true
                    },
                    selectedZoomType = myAppState.selectedZoomType,
                    onZoomTypeChange = {
                        myAppState.selectedZoomType = it
                        SettingsRepository.setZoomType(context, it)
                    },
                    onManageTagsClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.TagManagement
                    },
                    onBackupAndRestoreClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.showBackupAndRestoreDialog = true
                    },
                    onGoToSecretStorage = {
                        BiometricUtils.authenticate(
                            activity = context as AppCompatActivity,
                            onSuccess = { myAppState.currentScreen = Screen.SecretStorage },
                            onError = { _, _ -> /* Do nothing */ },
                            onFailed = { /* Do nothing */ }
                        )
                    },
                    isVibrationEnabled = isVibrationEnabled,
                    onVibrationEnabledChange = {
                        onVibrationEnabledChange(it)
                        SettingsRepository.setVibrationEnabled(context, it)
                    },
                    isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
                    onShowFileCountChange = {
                        myAppState.isShowFileCountEnabled = it
                        SettingsRepository.setShowFileCount(context, it)
                    },
                    isShuffleButtonVisible = myAppState.isShuffleButtonVisible,
                    onShuffleButtonVisibleChange = {
                        myAppState.isShuffleButtonVisible = it
                        SettingsRepository.setShuffleButtonVisible(context, it)
                    },
                    isShakeToBlurEnabled = isShakeToBlurEnabled,
                    onShakeToBlurEnabledChange = {
                        onShakeToBlurEnabledChange(it)
                        SettingsRepository.setShakeToBlurEnabled(context, it)
                    },
                    isLoopVideoEnabled = isLoopVideoEnabled,
                    onLoopVideoEnabledChange = {
                        onLoopVideoEnabledChange(it)
                        SettingsRepository.setLoopVideoEnabled(context, it)
                    },
                    selectedBlurType = myAppState.selectedBlurType,
                    onBlurTypeChange = {
                        myAppState.selectedBlurType = it
                        SettingsRepository.setBlurType(context, it)
                    },
                    isSwipeToDismissEnabled = isSwipeToDismissEnabled,
                    onSwipeToDismissEnabledChange = {
                        onSwipeToDismissEnabledChange(it)
                        SettingsRepository.setSwipeToDismissEnabled(context, it)
                    },
                    useLargeFab = useLargeFab,
                    onUseLargeFabChange = {
                        onUseLargeFabChange(it)
                        SettingsRepository.setUseLargeFab(context, it)
                    },
                    autoDeleteTrashEnabled = autoDeleteTrashEnabled,
                    onAutoDeleteTrashEnabledChange = {
                        onAutoDeleteTrashEnabledChange(it)
                        SettingsRepository.setAutoDeleteTrashEnabled(context, it)
                    },
                    autoDeleteTrashDays = autoDeleteTrashDays,
                    onAutoDeleteTrashDaysChange = {
                        onAutoDeleteTrashDaysChange(it)
                        SettingsRepository.setAutoDeleteTrashDays(context, it)
                    },
                    selectedLanguage = myAppState.selectedLanguage,
                    onLanguageChange = { language -> myAppState.selectedLanguage = language },
                    onCheckForUpdates = {
                        coroutineScope.launch {
                            myAppState.checkForUpdates(true)
                        }
                    },
                    currentVersion = myAppState.currentVersion,
                    selectedFabAction = myAppState.selectedFabAction,
                    onFabActionChange = {
                        myAppState.selectedFabAction = it
                        SettingsRepository.setFabAction(context, it)
                    }
                )
            }

            is Screen.TagManagement -> {
                TagManagementScreen(
                    onDeleteTag = {
                        if (isVibrationEnabled) performVibration(context)
                        TagsRepository.removeTagFromAllItems(context, it)
                        myAppState.tags = TagsRepository.getTags(context)
                        myAppState.allTags = TagsRepository.getAllTags(context)
                    },
                    onEditTag = { oldTag, newTag ->
                        if (isVibrationEnabled) performVibration(context)
                        TagsRepository.renameTag(context, oldTag, newTag)
                        myAppState.tags = TagsRepository.getTags(context)
                        myAppState.allTags = TagsRepository.getAllTags(context)
                    },
                    onTagClick = { tag -> myAppState.currentScreen = Screen.MediaByTag(tag) },
                    allTags = myAppState.allTags,
                    onAddNewTag = onAddNewTag,
                    onMoveTag = onMoveTag,
                )
            }

            is Screen.Trash -> {
                TrashScreen(
                    items = myAppState.trashedItems,
                    selectedItems = myAppState.selectedItems.toList(),
                    imageLoader = imageLoader,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        myAppState.viewerState = MediaViewerState(items = myAppState.trashedItems, startIndex = myAppState.trashedItems.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearTrash = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.isClearingTrash = true
                        myAppState.itemsToDelete = myAppState.trashedItems.map { it.uri }
                        myAppState.showConfirmDeleteDialog = true
                    },
                    isTrashBlurEnabled = isTrashBlurEnabled,
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType
                )
            }
            is Screen.AllMedia -> {
                MediaGrid(
                    items = filteredAllMedia,
                    favorites = favorites,
                    selectedItems = myAppState.selectedItems,
                    imageLoader = imageLoader,
                    isBlurEnabled = isBlurAllMediaEnabled,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        myAppState.viewerState = MediaViewerState(items = filteredAllMedia, startIndex = filteredAllMedia.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType
                )
            }
            is Screen.MediaByTag -> {
                val mediaWithTag = remember(myAppState.allMedia, myAppState.tags, screen.tag) {
                    val urisWithTag = myAppState.tags.filter { it.value.contains(screen.tag) }.keys.asSequence().map { Uri.parse(it) }.toSet()
                    myAppState.allMedia.filter { it.uri in urisWithTag }
                }
                MediaGrid(
                    items = mediaWithTag,
                    favorites = favorites,
                    selectedItems = myAppState.selectedItems,
                    imageLoader = imageLoader,
                    isBlurEnabled = isBlurInFolderEnabled,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        myAppState.viewerState = MediaViewerState(items = mediaWithTag, startIndex = mediaWithTag.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType
                )
            }
            is Screen.SecretStorage -> {
                SecretStorageScreen(
                    items = myAppState.secretItems,
                    imageLoader = imageLoader,
                    selectedItems = myAppState.selectedItems,
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = { myAppState.selectedItems.clear() },
                    onItemClick = { items, item ->
                        keyboardController?.hide()
                        myAppState.secretViewerState = MediaViewerState(items = items, startIndex = items.indexOf(item))
                    },
                    isBlurEnabled = isBlurEnabled,
                    blurType = myAppState.selectedBlurType
                )
            }
        }
    }
}