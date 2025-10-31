@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.nkdsify

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.nkdsify.data.*
import com.example.nkdsify.ui.AppNavigation
import com.example.nkdsify.ui.BottomBar
import com.example.nkdsify.ui.TopBar
import com.example.nkdsify.ui.components.*
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.*
import com.example.nkdsify.ui.utils.deleteMediaPermanently
import com.example.nkdsify.ui.utils.getMediaDetails
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.ContentUris
import androidx.annotation.OptIn

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val initialUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            MyApp(initialUri = initialUri, screenWidth = screenWidth, screenHeight = screenHeight)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MyApp(initialUri: Uri? = null, screenWidth: Int, screenHeight: Int) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(SettingsRepository.getTheme(context)) }
    var selectedZoomType by remember { mutableStateOf(SettingsRepository.getZoomType(context)) }
    var selectedVibrationStrength by remember { mutableStateOf(SettingsRepository.getVibrationStrength(context)) }
    var isShowFileCountEnabled by remember { mutableStateOf(SettingsRepository.isShowFileCountEnabled(context)) }
    val keyboardController = LocalSoftwareKeyboardController.current

    NkdsifyAppTheme(theme = selectedTheme) {
        val haptics = LocalHapticFeedback.current

        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        var hasPermissions by remember { mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) }

        var allFolders by remember { mutableStateOf<List<MediaFolder>>(emptyList()) }
        var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
        var viewerState by remember { mutableStateOf<MediaViewerState?>(null) }

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Folders) }
        val foldersGridState = rememberLazyGridState()
        val favoritesGridState = rememberLazyGridState()

        var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
        var sortAscending by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<Long?>(null) }
        var refreshTrigger by remember { mutableIntStateOf(0) }
        var isBlurEnabled by remember { mutableStateOf(SettingsRepository.isBlurEnabled(context)) }
        var isTrashBlurEnabled by remember { mutableStateOf(SettingsRepository.isTrashBlurEnabled(context)) }
        var isMuteVideoByDefault by remember { mutableStateOf(SettingsRepository.isMuteVideoByDefault(context)) }
        var isBlurAllMediaEnabled by remember { mutableStateOf(SettingsRepository.isBlurAllMediaEnabled(context)) }
        var hiddenFolders by remember { mutableStateOf(SettingsRepository.getHiddenFolders(context)) }

        var showTagDialog by remember { mutableStateOf<Uri?>(null) }
        var showBulkTagDialog by remember { mutableStateOf(false) }
        var showDetailsDialog by remember { mutableStateOf<Uri?>(null) }
        var showAlbumDetailsDialog by remember { mutableStateOf(false) }
        var showConfirmDeleteDialog by remember { mutableStateOf(false) }
        var showConfirmTrashDialog by remember { mutableStateOf(false) }
        var showConfirmRestoreDialog by remember { mutableStateOf(false) }
        var showEasterEggDialog by remember { mutableStateOf(false) }
        var showHiddenFoldersDialog by remember { mutableStateOf(false) }
        var showBackupAndRestoreDialog by remember { mutableStateOf(false) }

        var searchQuery by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }

        var easterEggTapCount by remember { mutableIntStateOf(0) }
        var itemsToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var isClearingTrash by remember { mutableStateOf(false) }
        var itemsToTrash by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var itemsToRestore by remember { mutableStateOf<List<Uri>>(emptyList()) }

        var isSettingWallpaper by remember { mutableStateOf(false) }

        val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
            if (result.isSuccessful) {
                val croppedImageUri = result.uriContent

                if (croppedImageUri != null) {
                    if (isSettingWallpaper) {
                        try {
                            val wallpaperManager = WallpaperManager.getInstance(context)
                            context.contentResolver.openInputStream(croppedImageUri)?.use { inputStream ->
                                wallpaperManager.setStream(inputStream)
                                Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        isSettingWallpaper = false // Reset flag
                        viewerState = null
                    }
                }
            }
            showDetailsDialog = null
        }

        val favorites = remember {
            val initialFavorites = FavoritesRepository.getFavorites(context).map { it.toUri() }
            mutableStateListOf(*initialFavorites.toTypedArray())
        }
        var favoriteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
        var trashedItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
        var tags by remember { mutableStateOf(TagsRepository.getTags(context)) }

        val selectedItems = remember { mutableStateListOf<Uri>() }
        val isSelectionMode = selectedItems.isNotEmpty()

        val imageLoader = remember(context) {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            hasPermissions = permissions.values.all { it }
        }

        val deleteRequestLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (isClearingTrash) {
                    TrashRepository.clearTrash(context)
                    isClearingTrash = false
                } else {
                    TrashRepository.removeFromTrash(context, itemsToDelete)
                }
                refreshTrigger++
                selectedItems.clear()
                itemsToDelete = emptyList()
                viewerState = null // Dismiss viewer on successful deletion
            }
            showConfirmDeleteDialog = false
        }

        val importFavoritesLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val json = BufferedReader(InputStreamReader(inputStream)).readText()
                        val type = object : TypeToken<Set<String>>() {}.type
                        val importedFavorites: Set<String> = Gson().fromJson(json, type)
                        favorites.clear()
                        favorites.addAll(importedFavorites.map { uriString -> uriString.toUri() })
                        refreshTrigger++
                        Toast.makeText(context, "Favorites imported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to import favorites!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val importTagsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val json = BufferedReader(InputStreamReader(inputStream)).readText()
                        val type = object : TypeToken<Map<String, Set<String>>>() {}.type
                        val importedTags: Map<String, Set<String>> = Gson().fromJson(json, type)
                        tags = importedTags
                        TagsRepository.saveTags(context, tags)
                        refreshTrigger++
                        Toast.makeText(context, "Tags imported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to import tags!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val pullRefreshState = rememberPullToRefreshState()
        if (pullRefreshState.isRefreshing) {
            LaunchedEffect(true) {
                delay(1000) // For presentation purposes
                refreshTrigger++
                pullRefreshState.endRefresh()
            }
        }

        LaunchedEffect(initialUri, hasPermissions) {
            if (initialUri != null && hasPermissions) {
                withContext(Dispatchers.IO) {
                    val loadedFolders = loadMediaFolders(context, sortType, sortAscending, null)
                    var targetFolder: MediaFolder? = null
                    var targetItemIndex = -1

                    val mediaUri = if (initialUri.scheme == "file") {
                        val path = initialUri.path
                        context.contentResolver.query(MediaStore.Files.getContentUri("external"), arrayOf(MediaStore.Files.FileColumns._ID), "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(path), null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val id = cursor.getLong(0)
                                ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                            } else null
                        }
                    } else {
                        initialUri
                    }

                    if (mediaUri != null) {
                        for (folder in loadedFolders) {
                            val index = folder.items.indexOfFirst { item -> item.uri == mediaUri }
                            if (index != -1) {
                                targetFolder = folder
                                targetItemIndex = index
                                break
                            }
                        }
                    }

                    if (targetFolder != null) {
                        viewerState = MediaViewerState(targetFolder.items, targetItemIndex)
                    } else {
                        val details = getMediaDetails(context, initialUri)
                        val name = details?.name ?: ""
                        val isVideo = context.contentResolver.getType(initialUri)?.startsWith("video/") == true
                        viewerState = MediaViewerState(listOf(MediaItem(initialUri, name, isVideo, 0, 0, 0)), 0, isExternal = true)
                    }
                }
            }
        }

        LaunchedEffect(favorites.toList()) {
            val favoriteStrings = favorites.map { it.toString() }.toSet()
            FavoritesRepository.saveFavorites(context, favoriteStrings)
        }

        LaunchedEffect(currentScreen) {
            if (currentScreen !is Screen.FolderContent && currentScreen !is Screen.Favorites) {
                isSearchActive = false
                searchQuery = ""
            }
            selectedItems.clear()
        }

        LaunchedEffect(Unit) {
            if (!hasPermissions) {
                permissionLauncher.launch(permissionsToRequest)
            }
        }

        LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, hiddenFolders, refreshTrigger) {
            if (hasPermissions) {
                allFolders = withContext(Dispatchers.IO) { loadMediaFolders(context, sortType, sortAscending, selectedDate) }
                trashedItems = withContext(Dispatchers.IO) { loadTrashedMediaItems(context, sortType, sortAscending) }
                allMedia = withContext(Dispatchers.IO) { loadAllMedia(context, sortType, sortAscending, hiddenFolders, selectedDate) }
            }
        }

        LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, favorites.size, refreshTrigger) {
            if (hasPermissions) {
                favoriteItems = withContext(Dispatchers.IO) { loadFavoriteMediaItems(context, favorites.toSet(), sortType, sortAscending, selectedDate) }
            }
        }

        val title = when (val screen = currentScreen) {
            is Screen.Folders -> "Folders"
            is Screen.FolderContent -> screen.folder.name
            is Screen.Favorites -> if (screen.openAlbumName != null) screen.openAlbumName else "Favorites"
            is Screen.Settings -> "Settings"
            is Screen.TagManagement -> "Manage Tags"
            is Screen.Trash -> "Trash"
            is Screen.AllMedia -> "All Media"
        }

        val datePickerState = rememberDatePickerState()
        var showDatePicker by remember { mutableStateOf(false) }

        if (showTagDialog != null) {
            val uri = showTagDialog!!
            TagEditDialog(
                initialTags = TagsRepository.getTagsForItem(context, uri),
                allTags = tags.values.flatten().toSet(),
                onDismiss = { showTagDialog = null },
                onSave = { tagSet ->
                    TagsRepository.setTagsForItem(context, uri, tagSet)
                    tags = TagsRepository.getTags(context)
                    showTagDialog = null
            })
        }

        if (showBulkTagDialog) {
            val uris = selectedItems.toList()
            val commonTags = if (uris.isNotEmpty()) {
                uris.map { TagsRepository.getTagsForItem(context, it) }.reduce { acc, set -> acc.intersect(set) }
            } else emptySet()

            TagEditDialog(
                initialTags = commonTags,
                allTags = tags.values.flatten().toSet(),
                onDismiss = { showBulkTagDialog = false },
                onSave = { newTags ->
                    val tagsToAdd = newTags - commonTags
                    val tagsToRemove = commonTags - newTags
                    uris.forEach { uri ->
                        val currentTags = TagsRepository.getTagsForItem(context, uri).toMutableSet()
                        currentTags.addAll(tagsToAdd)
                        currentTags.removeAll(tagsToRemove)
                        TagsRepository.setTagsForItem(context, uri, currentTags)
                    }
                    tags = TagsRepository.getTags(context)
                    showBulkTagDialog = false
                    selectedItems.clear()
                }
            )
        }

        if (showDetailsDialog != null) {
            val uri = showDetailsDialog!!
            val details = getMediaDetails(context, uri)
            if (details != null) {
                MediaDetailsDialog(
                    details = details,
                    onDismiss = { showDetailsDialog = null },
                    onSetAsWallpaper = {
                        isSettingWallpaper = true
                        val cropOptions = CropImageContractOptions(uri, CropImageOptions(
                            guidelines = CropImageView.Guidelines.ON,
                            fixAspectRatio = true,
                            aspectRatioX = screenWidth,
                            aspectRatioY = screenHeight,
                            outputRequestWidth = screenWidth,
                            outputRequestHeight = screenHeight,
                            outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT
                        ))
                        cropImageLauncher.launch(cropOptions)
                    }
                )
            }
        }
        if (showAlbumDetailsDialog) {
            val screen = currentScreen
            if (screen is Screen.FolderContent) {
                val folder = screen.folder
                val path = getMediaDetails(context, folder.items.first().uri)?.path?.substringBeforeLast('/') ?: ""
                AlbumDetailsDialog(
                    details = AlbumDetails(path, folder.totalSize, folder.dateRange, folder.itemCount),
                    onDismiss = { showAlbumDetailsDialog = false })
            } else if (screen is Screen.Favorites && screen.openAlbumName != null) {
                val taggedAlbums = favoriteItems
                    .flatMap { item -> (tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                    .groupBy({ it.first }, { it.second })
                val albumItems = if (screen.openAlbumName == "All Favorites") favoriteItems else taggedAlbums[screen.openAlbumName] ?: emptySet()

                if (albumItems.isNotEmpty()) {
                    val totalSize = albumItems.sumOf { it.size }
                    AlbumDetailsDialog(
                        details = AlbumDetails(totalSize = totalSize, itemCount = albumItems.size),
                        onDismiss = { showAlbumDetailsDialog = false })
                }
            }
        }

        if (showEasterEggDialog) {
            EasterEggDialog(onDismiss = { showEasterEggDialog = false })
        }

        if (showHiddenFoldersDialog) {
            HiddenFoldersDialog(
                allFolders = allFolders,
                hiddenFolders = hiddenFolders,
                onDismiss = { showHiddenFoldersDialog = false },
                onFolderHiddenChange = { folderId, isHidden ->
                    val newHiddenFolders = if (isHidden) {
                        hiddenFolders + folderId
                    } else {
                        hiddenFolders - folderId
                    }
                    hiddenFolders = newHiddenFolders
                    SettingsRepository.setHiddenFolders(context, newHiddenFolders)
                }
            )
        }

        if (showBackupAndRestoreDialog) {
            BackupAndRestoreDialog(
                onDismiss = { showBackupAndRestoreDialog = false },
                onExportFavorites = {
                    val json = Gson().toJson(favorites.map { it.toString() })
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "favorites_backup.json")
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(json.toByteArray())
                            }
                            Toast.makeText(context, "Favorites exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Failed to export favorites!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create backup file!", Toast.LENGTH_SHORT).show()
                    }
                },
                onImportFavorites = { importFavoritesLauncher.launch("application/json") },
                onExportTags = {
                    val json = Gson().toJson(tags)
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "tags_backup.json")
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(json.toByteArray())
                            }
                            Toast.makeText(context, "Tags exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Failed to export tags!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create backup file!", Toast.LENGTH_SHORT).show()
                    }
                },
                onImportTags = { importTagsLauncher.launch("application/json") }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
                TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis; showDatePicker = false }) {
                    Text("OK")
                }
            }, dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }) {
                DatePicker(state = datePickerState)
            }
        }

        if (showConfirmDeleteDialog) {
            ConfirmDeleteDialog(onConfirm = {
                val intentSender = deleteMediaPermanently(context, itemsToDelete)
                if (intentSender != null) {
                    deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } else {
                    if (isClearingTrash) {
                        TrashRepository.clearTrash(context)
                        isClearingTrash = false
                    } else {
                        TrashRepository.removeFromTrash(context, itemsToDelete)
                    }
                    refreshTrigger++
                    selectedItems.clear()
                    viewerState = null
                    showConfirmDeleteDialog = false
                }
            }, onDismiss = { showConfirmDeleteDialog = false })
        }

        if (showConfirmTrashDialog) {
            ConfirmTrashDialog(
                onConfirm = {
                    TrashRepository.addToTrash(context, itemsToTrash)
                    refreshTrigger++
                    selectedItems.clear()
                    itemsToTrash = emptyList()
                    showConfirmTrashDialog = false
                    viewerState = null // Dismiss viewer if active
                },
                onDismiss = { showConfirmTrashDialog = false }
            )
        }

        if (showConfirmRestoreDialog) {
            ConfirmRestoreDialog(
                onConfirm = {
                    TrashRepository.removeFromTrash(context, itemsToRestore)
                    selectedItems.clear()
                    refreshTrigger++
                    showConfirmRestoreDialog = false
                },
                onDismiss = { showConfirmRestoreDialog = false }
            )
        }

        Box(Modifier.fillMaxSize()) {
            BackHandler(enabled = isSelectionMode) {
                selectedItems.clear()
            }
            BackHandler(enabled = currentScreen is Screen.FolderContent) {
                currentScreen = Screen.Folders
            }
            BackHandler(enabled = currentScreen is Screen.Favorites && (currentScreen as Screen.Favorites).openAlbumName != null) {
                currentScreen = Screen.Favorites()
            }
            BackHandler(enabled = currentScreen is Screen.Settings) { currentScreen = Screen.Folders }
            BackHandler(enabled = currentScreen is Screen.TagManagement) { currentScreen = Screen.Settings }
            BackHandler(enabled = currentScreen is Screen.Trash) { currentScreen = Screen.Folders }
            BackHandler(enabled = currentScreen is Screen.AllMedia) { currentScreen = Screen.Folders }

            Scaffold(
                topBar = {
                    TopBar(
                        isSelectionMode = isSelectionMode,
                        selectedItems = selectedItems,
                        onCloseSelection = { selectedItems.clear() },
                        currentScreen = currentScreen,
                        onSelectAll = {
                            val allUris = trashedItems.map { it.uri }
                            if (selectedItems.containsAll(allUris)) {
                                selectedItems.removeAll(allUris)
                            } else {
                                selectedItems.addAll(allUris)
                            }
                        },
                        onRestore = {
                            itemsToRestore = selectedItems.toList()
                            showConfirmRestoreDialog = true
                        },
                        onDeletePermanently = {
                            itemsToDelete = selectedItems.toList()
                            showConfirmDeleteDialog = true
                        },
                        onEditTags = { showBulkTagDialog = true },
                        onShare = {
                            val currentSelected = selectedItems.toList()
                            selectedItems.clear()
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(currentSelected))
                                type = "*/*"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        },
                        onTrash = {
                            itemsToTrash = selectedItems.toList()
                            showConfirmTrashDialog = true
                        },
                        onToggleFavorite = {
                            performVibration(haptics, selectedVibrationStrength)
                            if (currentScreen is Screen.Favorites) {
                                val urisToUnfavorite = selectedItems.toList()
                                favorites.removeAll(urisToUnfavorite.toSet())
                                favoriteItems = favoriteItems.filterNot { it.uri in urisToUnfavorite.toSet() }
                            } else {
                                val urisToAdd = selectedItems.filterNot { favorites.contains(it) }
                                if (urisToAdd.isNotEmpty()) {
                                    favorites.addAll(urisToAdd)
                                }
                            }
                            selectedItems.clear()
                        },
                        isFavoritesScreen = currentScreen is Screen.Favorites,
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        title = title,
                        onBackClick = {
                            when (val screen = currentScreen) {
                                is Screen.TagManagement -> currentScreen = Screen.Settings
                                is Screen.Favorites -> currentScreen = Screen.Favorites()
                                else -> currentScreen = Screen.Folders
                            }
                        },
                        onCloseSearch = {
                            isSearchActive = false
                            searchQuery = ""
                        },
                        onSearchClick = { isSearchActive = true },
                        onFilterByDateClick = { showDatePicker = true },
                        onSortTypeChange = { sortType = it },
                        onReverseSort = { sortAscending = !sortAscending },
                        selectedDate = selectedDate,
                        onResetDateFilter = { selectedDate = null },
                        onDetailsClick = { showAlbumDetailsDialog = true }
                    )
                },
                bottomBar = {
                    BottomBar(
                        currentScreen = currentScreen,
                        haptics = haptics,
                        onScreenChange = { currentScreen = it },
                        context = context,
                        onSettingsClick = { currentScreen = Screen.Settings },
                        vibrationStrength = selectedVibrationStrength
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).nestedScroll(pullRefreshState.nestedScrollConnection)) {
                    if (hasPermissions) {
                        AppNavigation(
                            currentScreen = currentScreen,
                            allFolders = allFolders,
                            hiddenFolders = hiddenFolders,
                            searchQuery = searchQuery,
                            isSearchActive = isSearchActive,
                            favoriteItems = favoriteItems,
                            allMedia = allMedia,
                            imageLoader = imageLoader,
                            onFolderClick = { currentScreen = Screen.FolderContent(it) },
                            isBlurEnabled = isBlurEnabled,
                            foldersGridState = foldersGridState,
                            favorites = favorites,
                            selectedItems = selectedItems,
                            setViewerState = { viewerState = it },
                            keyboardController = keyboardController,
                            tags = tags,
                            favoritesGridState = favoritesGridState,
                            onClearSelection = { selectedItems.clear() },
                            onClearSearch = {
                                searchQuery = ""
                                isSearchActive = false
                            },
                            isTrashBlurEnabled = isTrashBlurEnabled,
                            onTrashBlurEnabledChange = {
                                isTrashBlurEnabled = it
                                SettingsRepository.setTrashBlurEnabled(context, it)
                            },
                            isMuteVideoByDefault = isMuteVideoByDefault,
                            onMuteVideoByDefaultChange = {
                                isMuteVideoByDefault = it
                                SettingsRepository.setMuteVideoByDefault(context, it)
                            },
                            onEasterEggClick = {
                                easterEggTapCount++
                                if (easterEggTapCount == 10) {
                                    easterEggTapCount = 0
                                    showEasterEggDialog = true
                                    val mediaPlayer = MediaPlayer.create(context, R.raw.uwu)
                                    mediaPlayer.setOnCompletionListener { it.release() }
                                    mediaPlayer.start()
                                }
                            },
                            selectedTheme = selectedTheme,
                            onThemeChange = { theme ->
                                selectedTheme = theme
                                SettingsRepository.setTheme(context, theme)
                            },
                            onManageHiddenFoldersClick = { showHiddenFoldersDialog = true },
                            selectedZoomType = selectedZoomType,
                            onZoomTypeChange = {
                                selectedZoomType = it
                                SettingsRepository.setZoomType(context, it)
                            },
                            onManageTagsClick = { currentScreen = Screen.TagManagement },
                            onBackupAndRestoreClick = { showBackupAndRestoreDialog = true },
                            onDeleteTag = {
                                TagsRepository.removeTagFromAllItems(context, it)
                                tags = TagsRepository.getTags(context)
                            },
                            onEditTag = { oldTag, newTag ->
                                TagsRepository.renameTag(context, oldTag, newTag)
                                tags = TagsRepository.getTags(context)
                            },
                            trashedItems = trashedItems,
                            onClearTrash = {
                                isClearingTrash = true
                                itemsToDelete = trashedItems.map { it.uri }
                                showConfirmDeleteDialog = true
                            },
                            onBlurEnabledChange = {
                                isBlurEnabled = it
                                SettingsRepository.setBlurEnabled(context, it)
                            },
                            isBlurAllMediaEnabled = isBlurAllMediaEnabled,
                            onBlurAllMediaEnabledChange = {
                                isBlurAllMediaEnabled = it
                                SettingsRepository.setBlurAllMediaEnabled(context, it)
                            },
                            selectedVibrationStrength = selectedVibrationStrength,
                            onVibrationStrengthChange = {
                                selectedVibrationStrength = it
                                SettingsRepository.setVibrationStrength(context, it)
                            },
                            onOpenAlbum = { albumName -> currentScreen = Screen.Favorites(openAlbumName = albumName) },
                            isShowFileCountEnabled = isShowFileCountEnabled,
                            onShowFileCountChange = {
                                isShowFileCountEnabled = it
                                SettingsRepository.setShowFileCount(context, it)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Permission required to access media.")
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                    PullToRefreshContainer(
                        modifier = Modifier.align(Alignment.TopCenter),
                        state = pullRefreshState,
                    )
                }
            }

            if (viewerState != null) {
                BackHandler { viewerState = null }
                MediaViewer(
                    items = viewerState!!.items,
                    startIndex = viewerState!!.startIndex,
                    favorites = favorites,
                    onDismiss = { viewerState = null },
                    imageLoader = imageLoader,
                    isExternal = viewerState!!.isExternal, // Pass the flag
                    onDelete = { uris ->
                        if (currentScreen is Screen.Trash) {
                            itemsToDelete = uris
                            showConfirmDeleteDialog = true
                        } else {
                            itemsToTrash = uris
                            showConfirmTrashDialog = true
                        }
                    },
                    onShowTagDialog = { uri -> showTagDialog = uri },
                    onShowDetails = { uri -> showDetailsDialog = uri },
                    onToggleFavorite = { uri ->
                        if (favorites.contains(uri)) {
                            favorites.remove(uri)
                        } else {
                            favorites.add(uri)
                        }
                    },
                    isMuteVideoByDefault = isMuteVideoByDefault,
                    zoomType = selectedZoomType
                )
            }
        }
    }
}
