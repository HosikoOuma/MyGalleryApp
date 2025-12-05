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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.*
import com.example.nkdsify.ui.components.MediaGrid
import com.example.nkdsify.ui.screens.*
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MyAppNavigation(
    myAppState: MyAppState,
    imageLoader: ImageLoader,
    foldersGridState: LazyGridState,
    folderContentGridState: LazyGridState,
    favoritesGridState: LazyGridState,
    favoritesContentGridState: LazyGridState,
    trashGridState: LazyGridState,
    allMediaGridState: LazyGridState,
    secretGridState: LazyGridState,
    viewHistoryGridState: LazyGridState,
    filteredViewHistory: ImmutableList<MediaItem>,
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
    onAutoDeleteTrashDaysChange: (Int) -> Unit,
    selectedFontFamily: AppFontFamily,
    onFontFamilyChange: (AppFontFamily) -> Unit,
    onFabActionChange: (FabAction) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sortComparator by remember(myAppState.sortType, myAppState.sortAscending) {
        derivedStateOf {
            val baseComparator: Comparator<MediaItem> = when (myAppState.sortType) {
                SortType.DATE_MODIFIED -> compareBy { it.dateModified }
                SortType.DATE_ADDED -> compareBy { it.dateAdded }
                SortType.ALPHABET -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                SortType.SIZE -> compareBy { it.size }
            }
            if (myAppState.sortAscending) baseComparator else baseComparator.reversed()
        }
    }

    val visibleFolders by remember(myAppState.allFolders, myAppState.hiddenFolders, sortComparator) {
        derivedStateOf {
            myAppState.allFolders
                .filterNot { myAppState.hiddenFolders.contains(it.id.toString()) }
                .map { folder ->
                    folder.copy(items = folder.items.sortedWith(sortComparator).toImmutableList())
                }
                .toImmutableList()
        }
    }

    val filteredFavoriteItems by remember(myAppState.favoriteItems, myAppState.searchQuery, myAppState.isSearchActive, sortComparator, myAppState.selectedDate) {
        derivedStateOf {
            myAppState.favoriteItems
                .filter { item ->
                    myAppState.selectedDate?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        val itemCalendar = Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                        calendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                                calendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
                    } ?: true
                }
                .filter { item ->
                    if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
                        item.name.contains(myAppState.searchQuery, ignoreCase = true)
                    } else {
                        true
                    }
                }
                .sortedWith(sortComparator)
                .toImmutableList()
        }
    }

    val filteredAllMedia by remember(myAppState.allMedia, myAppState.isSearchActive, myAppState.searchQuery, myAppState.mediaTypeFilter, sortComparator, myAppState.selectedDate) {
        derivedStateOf {
            myAppState.allMedia
                .filter { item ->
                    when (myAppState.mediaTypeFilter) {
                        MediaTypeFilter.PHOTOS -> !item.isVideo
                        MediaTypeFilter.VIDEOS -> item.isVideo
                        else -> true
                    }
                }
                .filter { item ->
                    myAppState.selectedDate?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        val itemCalendar = Calendar.getInstance().apply { timeInMillis = item.dateAdded * 1000 }
                        calendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                                calendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
                    } ?: true
                }
                .filter { item ->
                    if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
                        item.name.contains(myAppState.searchQuery, ignoreCase = true)
                    } else {
                        true
                    }
                }
                .sortedWith(sortComparator)
                .toImmutableList()
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
            is Screen.ViewHistory -> 6
            is Screen.FolderContent -> 10
            is Screen.TagManagement -> 14
            is Screen.MediaByTag -> 15
            is Screen.About -> 20
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
            is Screen.About -> {
                AboutScreen()
            }

            is Screen.FolderContent -> {
                val folder = myAppState.allFolders.find { it.id == screen.folder.id } ?: screen.folder
                val items = if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
                    folder.items.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }.toImmutableList()
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
                    blurType = myAppState.selectedBlurType,
                    gridState = folderContentGridState
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
                        myAppState.viewerState = MediaViewerState(items = items.toImmutableList(), startIndex = items.indexOf(item))
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
                    contentGridState = favoritesContentGridState,
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
                val settingsState = SettingsState(
                    isBlurEnabled = isBlurEnabled,
                    isBlurInFolderEnabled = isBlurInFolderEnabled,
                    isTrashBlurEnabled = isTrashBlurEnabled,
                    isMuteVideoByDefault = myAppState.isMuteVideoByDefault,
                    isBlurAllMediaEnabled = isBlurAllMediaEnabled,
                    selectedTheme = myAppState.selectedTheme,
                    selectedZoomType = myAppState.selectedZoomType,
                    isVibrationEnabled = isVibrationEnabled,
                    isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
                    isShuffleButtonVisible = myAppState.isShuffleButtonVisible,
                    isShakeToBlurEnabled = isShakeToBlurEnabled,
                    isLoopVideoEnabled = isLoopVideoEnabled,
                    selectedBlurType = myAppState.selectedBlurType,
                    isSwipeToDismissEnabled = isSwipeToDismissEnabled,
                    useLargeFab = useLargeFab,
                    autoDeleteTrashEnabled = autoDeleteTrashEnabled,
                    autoDeleteTrashDays = autoDeleteTrashDays,
                    selectedLanguage = myAppState.selectedLanguage,
                    currentVersion = myAppState.currentVersion,
                    selectedFabAction = myAppState.selectedFabAction,
                    selectedFontFamily = selectedFontFamily,
                    isKeepControlsVisible = myAppState.isKeepControlsVisible
                )

                val settingsActions = SettingsActions(
                    onBlurEnabledChange = {
                        onBlurEnabledChange(it)
                        SettingsRepository.setBlurEnabled(context, it)
                    },
                    onBlurInFolderEnabledChange = {
                        onBlurInFolderEnabledChange(it)
                        SettingsRepository.setBlurInFolderEnabled(context, it)
                    },
                    onTrashBlurEnabledChange = {
                        onTrashBlurEnabledChange(it)
                        SettingsRepository.setTrashBlurEnabled(context, it)
                    },
                    onMuteVideoByDefaultChange = {
                        myAppState.isMuteVideoByDefault = it
                        SettingsRepository.setMuteVideoByDefault(context, it)
                    },
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
                    onThemeChange = { theme ->
                        myAppState.selectedTheme = theme
                        SettingsRepository.setTheme(context, theme)
                    },
                    onManageHiddenFoldersClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.showHiddenFoldersDialog = true
                    },
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
                    onViewHistoryClick = { myAppState.currentScreen = Screen.ViewHistory },
                    onVibrationEnabledChange = {
                        onVibrationEnabledChange(it)
                        SettingsRepository.setVibrationEnabled(context, it)
                    },
                    onShowFileCountChange = {
                        myAppState.isShowFileCountEnabled = it
                        SettingsRepository.setShowFileCount(context, it)
                    },
                    onShuffleButtonVisibleChange = {
                        myAppState.isShuffleButtonVisible = it
                        SettingsRepository.setShuffleButtonVisible(context, it)
                    },
                    onShakeToBlurEnabledChange = {
                        onShakeToBlurEnabledChange(it)
                        SettingsRepository.setShakeToBlurEnabled(context, it)
                    },
                    onLoopVideoEnabledChange = {
                        onLoopVideoEnabledChange(it)
                        SettingsRepository.setLoopVideoEnabled(context, it)
                    },
                    onBlurTypeChange = {
                        myAppState.selectedBlurType = it
                        SettingsRepository.setBlurType(context, it)
                    },
                    onSwipeToDismissEnabledChange = {
                        onSwipeToDismissEnabledChange(it)
                        SettingsRepository.setSwipeToDismissEnabled(context, it)
                    },
                    onUseLargeFabChange = {
                        onUseLargeFabChange(it)
                        SettingsRepository.setUseLargeFab(context, it)
                    },
                    onAutoDeleteTrashEnabledChange = {
                        onAutoDeleteTrashEnabledChange(it)
                        SettingsRepository.setAutoDeleteTrashEnabled(context, it)
                    },
                    onAutoDeleteTrashDaysChange = {
                        onAutoDeleteTrashDaysChange(it)
                        SettingsRepository.setAutoDeleteTrashDays(context, it)
                    },
                    onLanguageChange = { language -> myAppState.selectedLanguage = language },
                    onCheckForUpdates = {
                        coroutineScope.launch {
                            myAppState.checkForUpdates(true)
                        }
                    },
                    onAboutClick = { myAppState.currentScreen = Screen.About },
                    onFontFamilyChange = onFontFamilyChange,
                    onFabActionChange = {
                        onFabActionChange(it)
                        SettingsRepository.setFabAction(context, it)
                    },
                    onKeepControlsVisibleChange = {
                        myAppState.isKeepControlsVisible = it
                        SettingsRepository.setKeepControlsVisible(context, it)
                    }
                )

                SettingsScreen(state = settingsState, actions = settingsActions)
            }

            is Screen.TagManagement -> {
                TagManagementScreen(
                    onDeleteTag = {
                        if (isVibrationEnabled) performVibration(context)
                        TagsRepository.removeTagFromAllItems(context, it)
                        myAppState.tags = TagsRepository.getTags(context)
                        myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
                    },
                    onEditTag = { oldTag, newTag ->
                        if (isVibrationEnabled) performVibration(context)
                        TagsRepository.renameTag(context, oldTag, newTag)
                        myAppState.tags = TagsRepository.getTags(context)
                        myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
                    },
                    onTagClick = { tag -> myAppState.currentScreen = Screen.MediaByTag(tag) },
                    allTags = myAppState.allTags,
                    onAddNewTag = onAddNewTag,
                    onMoveTag = onMoveTag,
                    showAddDialog = myAppState.showAddDialog,
                    onDismissAddDialog = { myAppState.showAddDialog = false }
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
                        myAppState.itemsToDelete = myAppState.trashedItems.map { it.uri }.toImmutableList()
                        myAppState.showConfirmDeleteDialog = true
                    },
                    isTrashBlurEnabled = isTrashBlurEnabled,
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType,
                    gridState = trashGridState
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
                        myAppState.viewerState = MediaViewerState(items = filteredAllMedia.toImmutableList(), startIndex = filteredAllMedia.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = { myAppState.selectedItems.clear() },
                    blurType = myAppState.selectedBlurType,
                    gridState = allMediaGridState
                )
            }
            is Screen.MediaByTag -> {
                val mediaWithTag = remember(myAppState.allMedia, myAppState.tags, screen.tag) {
                    val urisWithTag = myAppState.tags.filter { it.value.contains(screen.tag) }.keys.asSequence().map { Uri.parse(it) }.toSet()
                    myAppState.allMedia.filter { it.uri in urisWithTag }.toImmutableList()
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
                    blurType = myAppState.selectedBlurType,
                    gridState = favoritesGridState
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
                        myAppState.secretViewerState = MediaViewerState(items = items.toImmutableList(), startIndex = items.indexOf(item))
                    },
                    isBlurEnabled = isBlurEnabled,
                    blurType = myAppState.selectedBlurType,
                    gridState = secretGridState
                )
            }
            is Screen.ViewHistory -> {
                ViewHistoryScreen(
                    items = filteredViewHistory, // Use filtered list
                    favorites = favorites,
                    selectedItems = myAppState.selectedItems,
                    imageLoader = imageLoader,
                    onItemClick = { items, item ->
                        keyboardController?.hide()
                        myAppState.viewerState = MediaViewerState(items = items.toImmutableList(), startIndex = items.indexOf(item))
                    },
                    onToggleSelection = { item ->
                        if (myAppState.selectedItems.contains(item.uri)) {
                            myAppState.selectedItems.remove(item.uri)
                        } else {
                            myAppState.selectedItems.add(item.uri)
                        }
                    },
                    onClearSelection = { myAppState.selectedItems.clear() },
                    gridState = viewHistoryGridState,
                    blurType = myAppState.selectedBlurType,
                    isBlurEnabled = isBlurEnabled
                )
            }
        }
    }
}
