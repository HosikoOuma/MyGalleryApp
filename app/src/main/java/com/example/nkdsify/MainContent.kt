// Вынесенный MyApp composable для очистки MainActivity
@file:kotlin.OptIn(ExperimentalMaterial3Api::class)
package com.example.nkdsify

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.example.nkdsify.ui.utils.getMediaDetails
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.ContentUris
import androidx.annotation.OptIn
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.ui.components.FolderSelectionDialog
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MyApp(initialUri: Uri? = null, screenWidth: Int, screenHeight: Int,
          isShakeToBlurEnabled: Boolean, onShakeToBlurEnabledChange: (Boolean) -> Unit,
          isBlurEnabled: Boolean, onBlurEnabledChange: (Boolean) -> Unit,
          isVibrationEnabled: Boolean, onVibrationEnabledChange: (Boolean) -> Unit,
          isBlurInFolderEnabled: Boolean, onBlurInFolderEnabledChange: (Boolean) -> Unit,
          onViewerOpenChange: (Boolean) -> Unit,
          isLoopVideoEnabled: Boolean, onLoopVideoEnabledChange: (Boolean) -> Unit,
          isSwipeToDismissEnabled: Boolean, onSwipeToDismissEnabledChange: (Boolean) -> Unit,
          useLargeFab: Boolean, onUseLargeFabChange: (Boolean) -> Unit,
          isBlurAllMediaEnabled: Boolean,
          isTrashBlurEnabled: Boolean,
          onBlurAllMediaEnabledChange: (Boolean) -> Unit,
          onTrashBlurEnabledChange: (Boolean) -> Unit,
          autoDeleteTrashEnabled: Boolean, onAutoDeleteTrashEnabledChange: (Boolean) -> Unit,
          autoDeleteTrashDays: Int, onAutoDeleteTrashDaysChange: (Int) -> Unit) {
    // ...весь код функции MyApp точно такой же, как был в MainActivity.kt...
    // Для краткости отсюда и ниже вставлен полный оригинальный код функции.

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // ViewModel: перенос бизнес-логики и данных
    val vm: MainViewModel = viewModel()
    val allFolders by vm.allFolders
    val allMedia by vm.allMedia
    val favoriteItems by vm.favoriteItems
    val trashedItems by vm.trashedItems
    val tags by vm.tags
    val favorites = vm.favorites

    var selectedTheme by remember { mutableStateOf(SettingsRepository.getTheme(context)) }
    var selectedZoomType by remember { mutableStateOf(SettingsRepository.getZoomType(context)) }
    var selectedBlurType by remember { mutableStateOf(SettingsRepository.getBlurType(context)) }
    var isShowFileCountEnabled by remember { mutableStateOf(SettingsRepository.isShowFileCountEnabled(context)) }
    var isShuffleButtonVisible by remember { mutableStateOf(SettingsRepository.isShuffleButtonVisible(context)) }
    var selectedLanguage by remember { mutableStateOf(SettingsRepository.getLanguage(context)) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    val currentVersion = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0" }

    // Используем вынесенную утилиту для проверки обновлений (см. UpdateUtils.kt)
    val checkForUpdatesAction: (Boolean) -> Unit = { isTriggeredByUser ->
        checkForUpdates(
            context = context,
            coroutineScope = coroutineScope,
            currentVersion = currentVersion,
            isTriggeredByUser = isTriggeredByUser,
            onNewVersion = { tagName ->
                latestVersion = tagName
                showUpdateDialog = true
            },
            onNoUpdate = {
                Toast.makeText(context, context.getString(R.string.no_updates_available), Toast.LENGTH_SHORT).show()
            }
        )
    }

    LaunchedEffect(Unit) {
        checkForUpdatesAction(false)
    }

    LaunchedEffect(selectedLanguage) {
        if (selectedLanguage.code != SettingsRepository.getLanguage(context).code) {
            SettingsRepository.setLanguage(context, selectedLanguage)
            (context as? Activity)?.recreate()
        }
    }

    NkdsifyAppTheme(theme = selectedTheme) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        var hasPermissions by remember { mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) }
        var hasManageStoragePermission by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true) }

        // данные берутся из ViewModel: allFolders/allMedia/favoriteItems/trashedItems/tags
        var viewerState by remember { mutableStateOf<MediaViewerState?>(null) }

        LaunchedEffect(viewerState) {
            onViewerOpenChange(viewerState != null)
        }

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Folders) }
        val foldersGridState = rememberLazyGridState()
        val favoritesGridState = rememberLazyGridState()

        var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
        var sortAscending by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<Long?>(null) }
        var refreshTrigger by remember { mutableIntStateOf(0) }
        var isMuteVideoByDefault by remember { mutableStateOf(SettingsRepository.isMuteVideoByDefault(context)) }
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
        var showFolderSelectionDialog by remember { mutableStateOf(false) }
        var showRenameDialog by remember { mutableStateOf<Uri?>(null) }
        var filesToProcess by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var currentFileOperation by remember { mutableStateOf<FileOperation?>(null) }

        var searchQuery by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }

        var easterEggTapCount by remember { mutableIntStateOf(0) }
        var itemsToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var isClearingTrash by remember { mutableStateOf(false) }
        var itemsToTrash by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var itemsToRestore by remember { mutableStateOf<List<Uri>>(emptyList()) }

        var isSettingWallpaper by remember { mutableStateOf(false) }

        // selection остаётся в UI
        val selectedItems = remember { mutableStateListOf<Uri>() }
        val isSelectionMode = selectedItems.isNotEmpty()

        val imageLoader = remember(context) {
            ImageLoader.Builder(context)
                .components {
                    add(ImageDecoderDecoder.Factory())
                    add(GifDecoder.Factory())
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        }

        // Название "Все избранные" заранее
        val allFavoritesAlbumName = stringResource(id = R.string.album_name_all_favorites)

        // Все launchers вынесены в вспомогательную структуру
        val appLaunchers = rememberAppLaunchers(
             onPermissionsResult = { permissions -> hasPermissions = permissions.values.all { it } },
             onManageStorageResult = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) hasManageStoragePermission = Environment.isExternalStorageManager() },
             onCropResult = { result ->
                 if (result.isSuccessful) {
                     val croppedImageUri = result.uriContent
                     if (croppedImageUri != null && isSettingWallpaper) {
                         try {
                             val wallpaperManager = WallpaperManager.getInstance(context)
                             context.contentResolver.openInputStream(croppedImageUri)?.use { inputStream ->
                                 wallpaperManager.setStream(inputStream)
                                 Toast.makeText(context, context.getString(R.string.wallpaper_set_successfully), Toast.LENGTH_SHORT).show()
                             }
                         } catch (e: Exception) {
                             Toast.makeText(context, context.getString(R.string.failed_to_set_wallpaper, e.message), Toast.LENGTH_SHORT).show()
                         }
                         isSettingWallpaper = false
                         viewerState = null
                     }
                 }
                 showDetailsDialog = null
             },
             onImportFavoritesResult = { uri ->
                 vm.importFavoritesFromUri(uri) { context.contentResolver }
             },
             onImportTagsResult = { uri ->
                 vm.importTagsFromUri(uri) { context.contentResolver }
             }
         )

        val pullToRefreshEnabled = currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement
        val pullRefreshState = rememberPullToRefreshState()
        if (pullToRefreshEnabled && pullRefreshState.isRefreshing) {
            LaunchedEffect(true) {
                delay(1000)
                refreshTrigger++
                pullRefreshState.endRefresh()
            }
        }

        LaunchedEffect(Unit) {
            // Перенесено в ViewModel
            vm.deleteExpiredTrashIfNeeded()
        }

        LaunchedEffect(initialUri, hasPermissions) {
            if (initialUri != null && hasPermissions) {
                // Загружаем данные в VM и затем пытаемся найти initialUri в загруженных папках
                vm.loadData(sortType, sortAscending, null, hiddenFolders)
                // Если VM уже содержит папки, попробуем найти элемент — иначе сработает LaunchedEffect на allFolders
                val mediaUri = if (initialUri.scheme == "file") {
                    val path = initialUri.path
                    context.contentResolver.query(MediaStore.Files.getContentUri("external"), arrayOf(MediaStore.Files.FileColumns._ID), "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(path), null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        } else null
                    }
                } else initialUri

                if (mediaUri != null) {
                    val loaded = vm.allFolders.value
                    var targetFolder: MediaFolder? = null
                    var targetItemIndex = -1
                    for (folder in loaded) {
                        val index = folder.items.indexOfFirst { item -> item.uri == mediaUri }
                        if (index != -1) {
                            targetFolder = folder
                            targetItemIndex = index
                            break
                        }
                    }
                    if (targetFolder != null) viewerState = MediaViewerState(targetFolder.items, targetItemIndex)
                    else {
                        val details = getMediaDetails(context, initialUri)
                        val name = details?.name ?: ""
                        val isVideo = context.contentResolver.getType(initialUri)?.startsWith("video/") == true
                        viewerState = MediaViewerState(listOf(MediaItem(initialUri, name, isVideo, 0, 0, 0)), 0, isExternal = true)
                    }
                }
            }
        }

        LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, hiddenFolders, refreshTrigger) {
            if (hasPermissions) {
                vm.loadData(sortType, sortAscending, selectedDate, hiddenFolders)
            }
        }

        LaunchedEffect(allFolders) {
             val screen = currentScreen
             if (screen is Screen.FolderContent) {
                 val updatedFolder = allFolders.find { it.id == screen.folder.id }
                 if (updatedFolder == null || updatedFolder.items.isEmpty()) {
                     currentScreen = Screen.Folders
                 } else {
                     if (screen.folder != updatedFolder) {
                         currentScreen = Screen.FolderContent(updatedFolder)
                     }
                 }
             }
         }

        LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, favorites.size, refreshTrigger) {
            if (hasPermissions) {
                vm.loadFavoriteItems(sortType, sortAscending, selectedDate)
            }
        }

        LaunchedEffect(favorites.toList()) {
            vm.saveFavoritesToRepo()
        }

        LaunchedEffect(selectedTheme) {
            SettingsRepository.setTheme(context, selectedTheme)
        }

        LaunchedEffect(selectedLanguage) {
            SettingsRepository.setLanguage(context, selectedLanguage)
        }

        LaunchedEffect(Unit) {
            if (!hasPermissions) {
                appLaunchers.requestPermissions(permissionsToRequest)
            }
            if (!hasManageStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${context.packageName}".toUri()
                    appLaunchers.launchManageStorage(intent)
                }
            }
        }

        val title = when (val screen = currentScreen) {
            is Screen.Folders -> stringResource(id = R.string.screen_title_folders)
            is Screen.FolderContent -> screen.folder.name
            is Screen.Favorites -> screen.openAlbumName ?: stringResource(id = R.string.screen_title_favorites)
            is Screen.Settings -> stringResource(id = R.string.screen_title_settings)
            is Screen.TagManagement -> stringResource(id = R.string.screen_title_manage_tags)
            is Screen.Trash -> stringResource(id = R.string.screen_title_trash)
            is Screen.AllMedia -> stringResource(id = R.string.screen_title_all_media)
        }

        val datePickerState = rememberDatePickerState()
        var showDatePicker by remember { mutableStateOf(false) }

        // Диалоги вынесены в хост DialogsHost
        DialogsHost(
            context = context,
            coroutineScope = coroutineScope,
            showUpdateDialog = showUpdateDialog,
            latestVersion = latestVersion,
            onDismissUpdate = { showUpdateDialog = false },
            onOpenReleasePage = { _ ->
                // открываем страницу релиза
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HosikoOuma/MyGalleryApp/releases/latest"))
                context.startActivity(intent)
                showUpdateDialog = false
            },
            onDoNotShowUpdateAgain = {
                SettingsRepository.setCheckForUpdatesOnStartup(context, false)
                showUpdateDialog = false
            },
            showTagDialog = showTagDialog,
            onDismissTagDialog = { showTagDialog = null },
            onSaveTagsForItem = { uri, tagSet -> TagsRepository.setTagsForItem(context, uri, tagSet); vm.refreshTags() },
            tagsMap = tags,
            showBulkTagDialog = showBulkTagDialog,
            onDismissBulkTagDialog = { showBulkTagDialog = false },
            onSaveBulkTags = { uris, newTags ->
                val commonTags = if (uris.isNotEmpty()) uris.map { TagsRepository.getTagsForItem(context, it) }.reduce { acc, set -> acc.intersect(set) } else emptySet()
                val tagsToAdd = newTags - commonTags
                val tagsToRemove = commonTags - newTags
                uris.forEach { uri ->
                    val currentTags = TagsRepository.getTagsForItem(context, uri).toMutableSet()
                    currentTags.addAll(tagsToAdd)
                    currentTags.removeAll(tagsToRemove)
                    TagsRepository.setTagsForItem(context, uri, currentTags)
                }
                vm.refreshTags()
            },
            selectedItemsForBulk = selectedItems.toList(),
            showDetailsDialog = showDetailsDialog,
            onDismissDetailsDialog = { showDetailsDialog = null },
            launchCropForWallpaper = { options -> appLaunchers.launchCropImage(options) },
            onCopyFromDetails = { uri -> filesToProcess = listOf(uri); currentFileOperation = FileOperation.COPY; showFolderSelectionDialog = true; showDetailsDialog = null },
            onMoveFromDetails = { uri -> filesToProcess = listOf(uri); currentFileOperation = FileOperation.MOVE; showFolderSelectionDialog = true; showDetailsDialog = null },
            onRenameFromDetails = { uri -> showRenameDialog = uri; showDetailsDialog = null },
            showRenameDialog = showRenameDialog,
            onDismissRenameDialog = { showRenameDialog = null },
            onRenameItem = { uri, newName -> renameMedia(context, uri, newName); refreshTrigger++ },
            showAlbumDetailsDialog = showAlbumDetailsDialog,
            albumDetailsProvider = {
                val screen = currentScreen
                if (screen is Screen.FolderContent) {
                    val folder = screen.folder
                    val path = getMediaDetails(context, folder.items.first().uri)?.path?.substringBeforeLast('/') ?: ""
                    AlbumDetails(path, folder.totalSize, folder.dateRange, folder.itemCount)
                } else if (screen is Screen.Favorites && screen.openAlbumName != null) {
                    val taggedAlbums = favoriteItems
                        .flatMap { item -> (tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                        .groupBy({ it.first }, { it.second })
                    val albumItems = if (screen.openAlbumName == allFavoritesAlbumName) favoriteItems else taggedAlbums[screen.openAlbumName] ?: emptySet()
                    if (albumItems.isNotEmpty()) {
                        val totalSize = albumItems.sumOf { it.size }
                        AlbumDetails(totalSize = totalSize, itemCount = albumItems.size)
                    } else null
                } else null
            },
            onDismissAlbumDetails = { showAlbumDetailsDialog = false },
            showEasterEggDialog = showEasterEggDialog,
            onDismissEasterEgg = { showEasterEggDialog = false },
            showHiddenFoldersDialog = showHiddenFoldersDialog,
            allFolders = allFolders,
            hiddenFolders = hiddenFolders,
            onDismissHiddenFolders = { showHiddenFoldersDialog = false },
            onFolderHiddenChange = { folderId, isHidden ->
                val newHiddenFolders = if (isHidden) hiddenFolders + folderId else hiddenFolders - folderId
                hiddenFolders = newHiddenFolders
                SettingsRepository.setHiddenFolders(context, newHiddenFolders)
            },
            showBackupAndRestoreDialog = showBackupAndRestoreDialog,
            onDismissBackupAndRestore = { showBackupAndRestoreDialog = false },
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
                         Toast.makeText(context, context.getString(R.string.favorites_exported_successfully), Toast.LENGTH_SHORT).show()
                     } catch (_: Exception) {
                         Toast.makeText(context, context.getString(R.string.failed_to_export_favorites), Toast.LENGTH_SHORT).show()
                     }
                 } else {
                     Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), Toast.LENGTH_SHORT).show()
                 }
             },
            onImportFavorites = { appLaunchers.launchImportFavorites() },
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
                         Toast.makeText(context, context.getString(R.string.tags_exported_successfully), Toast.LENGTH_SHORT).show()
                     } catch (_: Exception) {
                         Toast.makeText(context, context.getString(R.string.failed_to_export_tags), Toast.LENGTH_SHORT).show()
                     }
                 } else {
                     Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), Toast.LENGTH_SHORT).show()
                 }
             },
            onImportTags = { appLaunchers.launchImportTags() },
            showDatePicker = showDatePicker,
            datePickerStateProvider = { datePickerState },
            onDateSelected = { selectedDate = it },
            onDatePickerDismiss = { showDatePicker = false },
            showConfirmDeleteDialog = showConfirmDeleteDialog,
            onConfirmDelete = {
                if (isClearingTrash) {
                    vm.clearTrash()
                    isClearingTrash = false
                } else {
                    TrashRepository.removeFromTrash(context, itemsToDelete)
                }
                if (isVibrationEnabled) performVibration(context)
                refreshTrigger++
                selectedItems.clear()
                viewerState = null
                showConfirmDeleteDialog = false
            },
            onDismissConfirmDelete = { showConfirmDeleteDialog = false },
            showConfirmTrashDialog = showConfirmTrashDialog,
            onConfirmTrash = {
                vm.copyToTrashAndDelete(itemsToTrash)
                if (isVibrationEnabled) performVibration(context)
                selectedItems.clear()
                itemsToTrash = emptyList()
                showConfirmTrashDialog = false
                viewerState = null
            },
            onDismissConfirmTrash = { showConfirmTrashDialog = false },
            showConfirmRestoreDialog = showConfirmRestoreDialog,
            onConfirmRestore = {
                vm.restoreFromTrash(itemsToRestore)
                selectedItems.clear()
                refreshTrigger++
                showConfirmRestoreDialog = false
                if (isVibrationEnabled) performVibration(context)
            },
            onDismissConfirmRestore = { showConfirmRestoreDialog = false },
            showFolderSelectionDialog = showFolderSelectionDialog,
            onDismissFolderSelection = { showFolderSelectionDialog = false },
            onFolderSelected = { destinationFolder ->
                coroutineScope.launch {
                    val folderPath = destinationFolder.items.firstOrNull()?.let { getFolderPathFromUri(context, it.uri) } ?: destinationFolder.name
                    when (currentFileOperation) {
                        FileOperation.COPY -> filesToProcess.forEach { copyMediaToFolder(context, it, folderPath) }
                        FileOperation.MOVE -> filesToProcess.forEach { moveMediaToFolder(context, it, folderPath); viewerState = null }
                        null -> {}
                    }
                    refreshTrigger++
                    showFolderSelectionDialog = false
                    filesToProcess = emptyList()
                    currentFileOperation = null
                }
            },
            favorites = favorites,
            favoriteItems = favoriteItems,
            tags = tags
         )

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
            BackHandler(enabled = currentScreen is Screen.Settings) {
                currentScreen = Screen.Folders
            }
            BackHandler(enabled = currentScreen is Screen.TagManagement) {
                currentScreen = Screen.Settings
            }
            BackHandler(enabled = currentScreen is Screen.Trash) { currentScreen = Screen.Folders }
            BackHandler(enabled = currentScreen is Screen.AllMedia) {
                currentScreen = Screen.Folders
            }
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
                            if (isVibrationEnabled) {
                                performVibration(context)
                            }
                            if (currentScreen is Screen.Favorites) {
                                val urisToUnfavorite = selectedItems.toList()
                                favorites.removeAll(urisToUnfavorite.toSet())
                                vm.favoriteItems.value = vm.favoriteItems.value.filterNot { it.uri in urisToUnfavorite.toSet() }
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
                        onBackClick = { currentScreen = Screen.Folders },
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
                        onDetailsClick = { showAlbumDetailsDialog = true },
                        context = context,
                        isVibrationEnabled = isVibrationEnabled,
                        onCopy = {
                            filesToProcess = selectedItems.toList()
                            currentFileOperation = FileOperation.COPY
                            showFolderSelectionDialog = true
                            selectedItems.clear()
                        },
                        onMove = {
                            filesToProcess = selectedItems.toList()
                            currentFileOperation = FileOperation.MOVE
                                showFolderSelectionDialog = true
                                selectedItems.clear()
                            }
                        )
                    },
                bottomBar = {
                    BottomBar(
                        currentScreen = currentScreen,
                        onScreenChange = { screen ->
                            currentScreen = screen
                        },
                        context = context,
                        onSettingsClick = { currentScreen = Screen.Settings },
                        isVibrationEnabled = isVibrationEnabled
                    )
                },
                floatingActionButton = {
                    if (isShuffleButtonVisible && currentScreen !is Screen.Trash && currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement) {
                        val allFavoritesAlbumName = stringResource(id = R.string.album_name_all_favorites)
                        val onClick = {
                            if (isVibrationEnabled) performVibration(context)
                            val itemsToShuffle = when (val screen = currentScreen) {
                                is Screen.FolderContent -> screen.folder.items
                                is Screen.AllMedia -> allMedia
                                is Screen.Favorites -> {
                                    if (screen.openAlbumName != null) {
                                        val taggedAlbums = favoriteItems
                                            .flatMap { item -> (tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                                            .groupBy({ it.first }, { it.second })
                                        if (screen.openAlbumName == allFavoritesAlbumName) favoriteItems else taggedAlbums[screen.openAlbumName]
                                            ?: emptyList()
                                    } else {
                                        favoriteItems
                                    }
                                }
                                is Screen.Folders -> allMedia
                                else -> emptyList()
                            }

                            if (itemsToShuffle.isNotEmpty()) {
                                val shuffledItems = itemsToShuffle.shuffled()
                                viewerState = MediaViewerState(items = shuffledItems, startIndex = 0)
                            }
                        }
                        if (useLargeFab) {
                            LargeFloatingActionButton(
                                onClick = onClick
                            ) {
                                Icon(Icons.Filled.Photo, contentDescription = stringResource(id = R.string.content_description_shuffle_play), modifier = Modifier.size(40.dp))
                            }
                        } else {
                            FloatingActionButton(
                                onClick = onClick
                            ) {
                                Icon(Icons.Filled.Photo, contentDescription = stringResource(id = R.string.content_description_shuffle_play), modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val boxModifier = if (pullToRefreshEnabled) {
                    Modifier
                        .padding(innerPadding)
                        .nestedScroll(pullRefreshState.nestedScrollConnection)
                } else {
                    Modifier.padding(innerPadding)
                }
                Box(modifier = boxModifier) {
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
                            isBlurInFolderEnabled = isBlurInFolderEnabled,
                            onBlurInFolderEnabledChange = {
                                onBlurInFolderEnabledChange(it)
                                SettingsRepository.setBlurInFolderEnabled(context, it)
                            },
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
                                onTrashBlurEnabledChange(it)
                                SettingsRepository.setTrashBlurEnabled(context, it)
                            },
                            isMuteVideoByDefault = isMuteVideoByDefault,
                            onMuteVideoByDefaultChange = {
                                isMuteVideoByDefault = it
                                SettingsRepository.setMuteVideoByDefault(context, it)
                            },
                            onEasterEggClick = {
                                if (isVibrationEnabled) performVibration(context)
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
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { language ->
                                selectedLanguage = language
                            },
                            onManageHiddenFoldersClick = {
                                if (isVibrationEnabled) performVibration(context)
                                showHiddenFoldersDialog = true
                            },
                            selectedZoomType = selectedZoomType,
                            onZoomTypeChange = {
                                selectedZoomType = it
                                SettingsRepository.setZoomType(context, it)
                            },
                            onManageTagsClick = {
                                if (isVibrationEnabled) performVibration(context)
                                currentScreen = Screen.TagManagement
                            },
                            onBackupAndRestoreClick = {
                                if (isVibrationEnabled) performVibration(context)
                                showBackupAndRestoreDialog = true
                            },
                            onDeleteTag = {
                                if (isVibrationEnabled) performVibration(context)
                                TagsRepository.removeTagFromAllItems(context, it)
                                vm.refreshTags()
                            },
                            onEditTag = { oldTag, newTag ->
                                if (isVibrationEnabled) performVibration(context)
                                TagsRepository.renameTag(context, oldTag, newTag)
                                vm.refreshTags()
                            },
                            trashedItems = trashedItems,
                            onClearTrash = {
                                if (isVibrationEnabled) performVibration(context)
                                isClearingTrash = true
                                itemsToDelete = trashedItems.map { it.uri }
                                showConfirmDeleteDialog = true
                            },
                            onBlurEnabledChange = {
                                onBlurEnabledChange(it)
                                SettingsRepository.setBlurEnabled(context, it)
                            },
                            isBlurAllMediaEnabled = isBlurAllMediaEnabled,
                            onBlurAllMediaEnabledChange = {
                                onBlurAllMediaEnabledChange(it)
                                SettingsRepository.setBlurAllMediaEnabled(context, it)
                            },
                            isVibrationEnabled = isVibrationEnabled,
                            onVibrationEnabledChange = {
                                onVibrationEnabledChange(it)
                                SettingsRepository.setVibrationEnabled(context, it)
                            },
                            onOpenAlbum = { albumName ->
                                if (isVibrationEnabled) performVibration(context)
                                currentScreen = Screen.Favorites(openAlbumName = albumName)
                            },
                            isShowFileCountEnabled = isShowFileCountEnabled,
                            onShowFileCountChange = {
                                isShowFileCountEnabled = it
                                SettingsRepository.setShowFileCount(context, it)
                            },
                            isShuffleButtonVisible = isShuffleButtonVisible,
                            onShuffleButtonVisibleChange = {
                                isShuffleButtonVisible = it
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
                            selectedBlurType = selectedBlurType,
                            onBlurTypeChange = {
                                selectedBlurType = it
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
                            onCheckForUpdates = {
                                checkForUpdatesAction(true)
                            },
                            currentVersion = currentVersion
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(id = R.string.permission_required_message))
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    if (isVibrationEnabled) performVibration(context)
                                    appLaunchers.requestPermissions(permissionsToRequest)
                                }) {
                                    Text(stringResource(id = R.string.grant_permission_button))
                                }
                            }
                        }
                    }
                    if (pullToRefreshEnabled) {
                        PullToRefreshContainer(
                            modifier = Modifier.align(Alignment.TopCenter),
                            state = pullRefreshState,
                        )
                    }
                }
            }

            if (viewerState != null) {
                BackHandler { viewerState = null }
                val isTrashViewing = viewerState?.items?.map { it.uri }?.intersect(trashedItems.map { it.uri }.toSet())?.isNotEmpty() ?: false
                MediaViewer(
                    items = viewerState!!.items,
                    startIndex = viewerState!!.startIndex,
                    favorites = favorites,
                    onDismiss = { viewerState = null },
                    imageLoader = imageLoader,
                    isExternal = viewerState!!.isExternal,
                    isTrashMode = isTrashViewing,
                    onDelete = { uris ->
                        if (isTrashViewing) {
                            itemsToDelete = uris
                            showConfirmDeleteDialog = true
                        } else {
                            itemsToTrash = uris
                            showConfirmTrashDialog = true
                        }
                    },
                    onRestore = { uris ->
                        itemsToRestore = uris
                        showConfirmRestoreDialog = true
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
                    onCopy = {
                        filesToProcess = listOf(it)
                        currentFileOperation = FileOperation.COPY
                        showFolderSelectionDialog = true
                    },
                    onMove = {
                        filesToProcess = listOf(it)
                        currentFileOperation = FileOperation.MOVE
                        showFolderSelectionDialog = true
                    },
                    isMuteVideoByDefault = isMuteVideoByDefault,
                    zoomType = selectedZoomType,
                    isLoopVideoEnabled = isLoopVideoEnabled,
                    isSwipeToDismissEnabled = isSwipeToDismissEnabled
                )
            }
        }
    }
}
//
