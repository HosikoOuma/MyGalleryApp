package com.example.nkdsify.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.nkdsify.ui.components.utils.rememberCoilImageLoader
import com.example.nkdsify.ui.screens.*
import com.example.nkdsify.ui.utils.parseQueryString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.*
import com.example.nkdsify.ui.impl.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MyAppNavigation(
    myAppState: MyAppState,
    isNavBarVisible: Boolean // Добавляем параметр
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imageLoader: ImageLoader = myAppState.imageLoader ?: rememberCoilImageLoader(context)
    val foldersGridState: LazyGridState = myAppState.foldersGridState ?: rememberLazyGridState()
    val folderContentGridState: LazyGridState = myAppState.folderContentGridState ?: rememberLazyGridState()
    val favoritesGridState: LazyGridState = myAppState.favoritesGridState ?: rememberLazyGridState()
    val favoritesContentGridState: LazyGridState = myAppState.favoritesContentGridState ?: rememberLazyGridState()
    val trashGridState: LazyGridState = myAppState.trashGridState ?: rememberLazyGridState()
    val allMediaGridState: LazyGridState = myAppState.allMediaGridState ?: rememberLazyGridState()
    val secretGridState: LazyGridState = myAppState.secretGridState ?: rememberLazyGridState()
    val viewHistoryGridState: LazyGridState = myAppState.viewHistoryGridState ?: rememberLazyGridState()
    val hiddenFoldersListState: LazyListState = myAppState.hiddenFoldersListState ?: rememberLazyListState()
    val filteredViewHistory: ImmutableList<MediaItem> = myAppState.filteredViewHistory
    val favorites: MutableList<String> = myAppState.favoritesList
    val keyboardController: SoftwareKeyboardController? = myAppState.keyboardController

    val onMoveTag = myAppState.onMoveTag ?: { _, _ -> }
    val onAddNewTag = myAppState.onAddNewTag ?: {}
    val onFontFamilyChange = myAppState.onFontFamilyChange ?: {}
    val onFabActionChange = myAppState.onFabActionChange ?: {}

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

    val visibleFolders by remember(myAppState.allFolders, myAppState.hiddenFolders, myAppState.searchQuery, myAppState.isSearchActive, myAppState.tags, myAppState.revelationModeEnabled) {
        derivedStateOf {
            val base = if (myAppState.revelationModeEnabled) {
                myAppState.allFolders
            } else {
                myAppState.allFolders.filterNot { myAppState.hiddenFolders.contains(it.id.toString()) }
            }

            if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty()) {
                val parsed = parseQueryString(myAppState.searchQuery)
                base.filter { folder ->
                    val nameMatch = if (parsed.searchTerms.isNotEmpty()) parsed.searchTerms.all { term -> folder.name.contains(term, ignoreCase = true) } else false
                    val anyItemMatches = folder.items.any { item ->
                        val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()
                        val textMatch = parsed.searchTerms.all { term -> item.name.contains(term, ignoreCase = true) }
                        val tagMatch = parsed.includedTagGroups.all { group -> group.any { tag -> itemTags.contains(tag) } } &&
                                (parsed.excludedTags.isEmpty() || !itemTags.any { it in parsed.excludedTags })
                        textMatch && tagMatch
                    }
                    nameMatch || anyItemMatches
                }.toImmutableList()
            } else {
                base.toImmutableList()
            }
        }
    }

    val filteredFavoriteItems by remember(myAppState.favoriteItems, myAppState.searchQuery, myAppState.isSearchActive, sortComparator, myAppState.selectedDate, myAppState.tags) {
        derivedStateOf {
            val parsedQuery = parseQueryString(myAppState.searchQuery)
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
                        val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()
                        val textMatch = parsedQuery.searchTerms.all { term -> item.name.contains(term, ignoreCase = true) }
                        val tagMatch = parsedQuery.includedTagGroups.all { group -> group.any { tag -> itemTags.contains(tag) } } &&
                                (parsedQuery.excludedTags.isEmpty() || !itemTags.any { it in parsedQuery.excludedTags })
                        textMatch && tagMatch
                    } else {
                        true
                    }
                }
                .sortedWith(sortComparator)
                .toImmutableList()
        }
    }

    val filteredAllMedia by remember(myAppState.allMedia, myAppState.isSearchActive, myAppState.searchQuery, myAppState.mediaTypeFilter, sortComparator, myAppState.selectedDate, myAppState.tags) {
        derivedStateOf {
            val parsedQuery = parseQueryString(myAppState.searchQuery)
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
                        val itemTags = myAppState.tags[item.absolutePath] ?: emptySet()
                        val textMatch = parsedQuery.searchTerms.all { term -> item.name.contains(term, ignoreCase = true) }
                        val tagMatch = parsedQuery.includedTagGroups.all { group -> group.any { tag -> itemTags.contains(tag) } } &&
                                (parsedQuery.excludedTags.isEmpty() || !itemTags.any { it in parsedQuery.excludedTags })
                        textMatch && tagMatch
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
            is Screen.HiddenFolders -> 5
            is Screen.SecretStorage -> 6
            is Screen.ViewHistory -> 7
            is Screen.FolderContent -> 10
            is Screen.TagManagement -> 14
            is Screen.MediaByTag -> 15
            is Screen.About -> 20
            is Screen.Help -> 21
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
            is Screen.Folders -> FoldersScreen(
                visibleFolders = visibleFolders,
                imageLoader = imageLoader,
                keyboardController = keyboardController,
                myAppState = myAppState,
                gridState = foldersGridState
            )

            is Screen.About -> AboutScreen()
            is Screen.Help -> HelpScreen()

            is Screen.HiddenFolders -> HiddenFoldersScreen(
                myAppState = myAppState,
                listState = hiddenFoldersListState
            )

            is Screen.FolderContent -> FolderContentScreen(
                screen = screen,
                sortComparator = sortComparator,
                myAppState = myAppState,
                imageLoader = imageLoader,
                gridState = folderContentGridState,
                keyboardController = keyboardController
            )

            is Screen.Favorites -> FavoritesScreenImpl(
                filteredFavoriteItems = filteredFavoriteItems,
                favorites = favorites,
                myAppState = myAppState,
                imageLoader = imageLoader,
                gridState = favoritesGridState,
                contentGridState = favoritesContentGridState,
                keyboardController = keyboardController,
                context = context,
                openAlbumName = screen.openAlbumName
            )

            is Screen.Settings -> SettingsScreenImpl(
                myAppState = myAppState,
                coroutineScope = coroutineScope,
                context = context,
                onFontFamilyChange = onFontFamilyChange,
                onFabActionChange = onFabActionChange
            )

            is Screen.TagManagement -> TagManagementScreenImpl(
                myAppState = myAppState,
                onAddNewTag = onAddNewTag,
                onMoveTag = onMoveTag,
                context = context
            )

            is Screen.Trash -> TrashScreenImpl(
                myAppState = myAppState,
                imageLoader = imageLoader,
                gridState = trashGridState,
                keyboardController = keyboardController,
                context = context,
                isNavBarVisible = isNavBarVisible // Передаем состояние
            )

            is Screen.AllMedia -> AllMediaScreenImpl(
                filteredAllMedia = filteredAllMedia,
                favorites = favorites,
                myAppState = myAppState,
                imageLoader = imageLoader,
                gridState = allMediaGridState,
                keyboardController = keyboardController
            )

            is Screen.MediaByTag -> MediaByTagScreenImpl(
                myAppState = myAppState,
                screen = screen,
                imageLoader = imageLoader,
                gridState = favoritesGridState,
                keyboardController = keyboardController
            )

            is Screen.SecretStorage -> SecretStorageScreenImpl(
                myAppState = myAppState,
                imageLoader = imageLoader,
                gridState = secretGridState,
                keyboardController = keyboardController
            )

            is Screen.ViewHistory -> ViewHistoryScreenImpl(
                filteredViewHistory = filteredViewHistory,
                favorites = favorites,
                myAppState = myAppState,
                imageLoader = imageLoader,
                gridState = viewHistoryGridState,
                keyboardController = keyboardController
            )
        }
    }
}
