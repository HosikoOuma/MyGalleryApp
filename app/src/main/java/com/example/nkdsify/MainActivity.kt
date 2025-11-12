//GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
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
import com.example.nkdsify.showUpdateNotification
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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

enum class FileOperation {
    COPY, MOVE
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null
    // Внутри класса MainActivity, но вне onCreate
    override fun attachBaseContext(newBase: Context) {
        // Здесь мы подменяем контекст на тот, в котором уже есть нужный язык
        super.attachBaseContext(ContextUtils.updateLocale(newBase))
    }


    //GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
//        val language = SettingsRepository.getLanguage(this)
//        val locale = if (language == Language.SYSTEM) {
//            LocaleListCompat.getEmptyLocaleList()
//        } else {
//            LocaleListCompat.forLanguageTags(language.code)
//        }
//        AppCompatDelegate.setApplicationLocales(locale)
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector()

        setContent {
            val initialUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            var isShakeToBlurEnabled by remember { mutableStateOf(SettingsRepository.isShakeToBlurEnabled(this@MainActivity)) }
            var isBlurEnabled by remember { mutableStateOf(SettingsRepository.isBlurEnabled(this@MainActivity)) }
            var isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(this@MainActivity)) }
            var isBlurInFolderEnabled by remember { mutableStateOf(SettingsRepository.isBlurInFolderEnabled(this@MainActivity)) }
            var isTrashBlurEnabled by remember { mutableStateOf(SettingsRepository.isTrashBlurEnabled(context = this@MainActivity)) }
            var isBlurAllMediaEnabled by remember { mutableStateOf(SettingsRepository.isBlurAllMediaEnabled(context = this@MainActivity)) }
            var isViewerOpen by remember { mutableStateOf(false) }
            var isLoopVideoEnabled by remember { mutableStateOf(SettingsRepository.isLoopVideoEnabled(this@MainActivity)) }
            var isSwipeToDismissEnabled by remember { mutableStateOf(SettingsRepository.isSwipeToDismissEnabled(this@MainActivity)) }
            var useLargeFab by remember { mutableStateOf(SettingsRepository.isUseLargeFab(this@MainActivity)) }
            var autoDeleteTrashEnabled by remember { mutableStateOf(SettingsRepository.isAutoDeleteTrashEnabled(this@MainActivity)) }
            var autoDeleteTrashDays by remember { mutableIntStateOf(SettingsRepository.getAutoDeleteTrashDays(this@MainActivity)) }


            MyApp(initialUri = initialUri, screenWidth = screenWidth, screenHeight = screenHeight,
                isShakeToBlurEnabled = isShakeToBlurEnabled, onShakeToBlurEnabledChange = { isShakeToBlurEnabled = it },
                isBlurEnabled = isBlurEnabled, onBlurEnabledChange = { isBlurEnabled = it },
                isVibrationEnabled = isVibrationEnabled, onVibrationEnabledChange = { isVibrationEnabled = it },
                isBlurInFolderEnabled = isBlurInFolderEnabled, onBlurInFolderEnabledChange = { isBlurInFolderEnabled = it },
                onViewerOpenChange = { isViewerOpen = it },
                isLoopVideoEnabled = isLoopVideoEnabled, onLoopVideoEnabledChange = {isLoopVideoEnabled = it},
                isSwipeToDismissEnabled = isSwipeToDismissEnabled, onSwipeToDismissEnabledChange = {isSwipeToDismissEnabled = it},
                useLargeFab = useLargeFab, onUseLargeFabChange = { useLargeFab = it }, isBlurAllMediaEnabled = isBlurAllMediaEnabled, isTrashBlurEnabled = isTrashBlurEnabled,
                onBlurAllMediaEnabledChange = { isBlurAllMediaEnabled = it },
                onTrashBlurEnabledChange = { isTrashBlurEnabled = it },
                autoDeleteTrashEnabled = autoDeleteTrashEnabled, onAutoDeleteTrashEnabledChange = { autoDeleteTrashEnabled = it },
                autoDeleteTrashDays = autoDeleteTrashDays, onAutoDeleteTrashDaysChange = { autoDeleteTrashDays = it }
            )

            shakeDetector?.setOnShakeListener {
                if (isShakeToBlurEnabled && !isViewerOpen) {
                    if (isVibrationEnabled) {
                        performVibration(this@MainActivity)
                    }
                    val newBlurState = !isBlurEnabled
                    isBlurEnabled = newBlurState
                    isBlurInFolderEnabled = newBlurState
                    isTrashBlurEnabled = newBlurState
                    isBlurAllMediaEnabled = newBlurState
                    SettingsRepository.setBlurEnabled(this@MainActivity, newBlurState)
                    SettingsRepository.setBlurInFolderEnabled(this@MainActivity, newBlurState)
                    SettingsRepository.setTrashBlurEnabled(this@MainActivity, newBlurState)
                    SettingsRepository.setBlurAllMediaEnabled(this@MainActivity, newBlurState)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }
}

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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

    fun compareVersionNames(v1: String, v2: String): Int {
        val parts1 = v1.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(parts1.size, parts2.size)
        for (i in 0 until size) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 < p2) return -1
            if (p1 > p2) return 1
        }
        return 0
    }

    val checkForUpdates: (Boolean) -> Unit = { isTriggeredByUser ->
        coroutineScope.launch(Dispatchers.IO) {
            if (!isTriggeredByUser && !SettingsRepository.isCheckForUpdatesOnStartupEnabled(context)) {
                return@launch
            }

            val release = GithubUpdateChecker.getLatestRelease("HosikoOuma", "MyGalleryApp")
            release?.let {
                if (compareVersionNames(it.tag_name, currentVersion) > 0) {
                    withContext(Dispatchers.Main) {
                        latestVersion = it.tag_name
                        showUpdateDialog = true
                        showUpdateNotification(context, it.tag_name)
                    }
                } else {
                    if (isTriggeredByUser) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.no_updates_available), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdates(false)
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

        var allFolders by remember { mutableStateOf<List<MediaFolder>>(emptyList()) }
        var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
        var viewerState by remember { mutableStateOf<MediaViewerState?>(null) }

        LaunchedEffect(viewerState) {
            onViewerOpenChange(viewerState != null)
        }

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Folders) }
        val foldersGridState = rememberLazyGridState()
        val favoritesGridState = rememberLazyGridState()
        val sanitizedFoldersState = remember { mutableStateOf<List<MediaFolder>>(allFolders) }

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

        val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hasManageStoragePermission = Environment.isExternalStorageManager()
            }
        }

        val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
            if (result.isSuccessful) {
                val croppedImageUri = result.uriContent

                if (croppedImageUri != null) {
                    if (isSettingWallpaper) {
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
                    add(ImageDecoderDecoder.Factory())
                    add(GifDecoder.Factory())
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            hasPermissions = permissions.values.all { it }
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
                        Toast.makeText(context, context.getString(R.string.favorites_imported_successfully), Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.failed_to_import_favorites), Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, context.getString(R.string.tags_imported_successfully), Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.failed_to_import_tags), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val pullToRefreshEnabled = currentScreen !is Screen.Settings && currentScreen !is Screen.TagManagement
        val pullRefreshState = rememberPullToRefreshState()
        if (pullToRefreshEnabled && pullRefreshState.isRefreshing) {
            LaunchedEffect(true) {
                delay(1000)
                refreshTrigger++
                pullRefreshState.endRefresh()
            }
        }
        LaunchedEffect(allFolders) {
            sanitizedFoldersState.value = sanitizeFolders(allFolders, context)
        }

        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                if (SettingsRepository.isAutoDeleteTrashEnabled(context)) {
                    val days = SettingsRepository.getAutoDeleteTrashDays(context)
                    TrashRepository.deleteExpired(context, days)
                }
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
            if (!hasManageStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${context.packageName}".toUri()
                    manageStorageLauncher.launch(intent)
                }
            }
        }

        LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, hiddenFolders, refreshTrigger) {
            if (hasPermissions) {
                allFolders = withContext(Dispatchers.IO) { loadMediaFolders(context, sortType, sortAscending, selectedDate) }
                trashedItems = withContext(Dispatchers.IO) { loadTrashedMediaItems(context, sortType, sortAscending) }
                allMedia = withContext(Dispatchers.IO) { loadAllMedia(context, sortType, sortAscending, hiddenFolders, selectedDate) }
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
                favoriteItems = withContext(Dispatchers.IO) { loadFavoriteMediaItems(context, favorites.toSet(), sortType, sortAscending, selectedDate) }
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

        if (showUpdateDialog && latestVersion != null) {
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                onConfirm = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HosikoOuma/MyGalleryApp/releases/latest"))
                    context.startActivity(intent)
                    showUpdateDialog = false
                },
                onDoNotShowAgain = {
                    SettingsRepository.setCheckForUpdatesOnStartup(context, false)
                    showUpdateDialog = false
                },
                latestVersion = latestVersion!!
            )
        }

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
                    },
                    onCopy = {
                        filesToProcess = listOf(uri)
                        currentFileOperation = FileOperation.COPY
                        showFolderSelectionDialog = true
                        showDetailsDialog = null
                    },
                    onMove = {
                        filesToProcess = listOf(uri)
                        currentFileOperation = FileOperation.MOVE
                        showFolderSelectionDialog = true
                        showDetailsDialog = null
                    },
                    onRename = {
                        showRenameDialog = uri
                        showDetailsDialog = null
                    }
                )
            }
        }

        if (showRenameDialog != null) {
            val uri = showRenameDialog!!
            val currentName = getMediaDetails(context, uri)?.name ?: ""
            RenameDialog(
                currentName = currentName,
                onDismiss = { showRenameDialog = null },
                onRename = { newName ->
                    renameMedia(context, uri, newName)
                    showRenameDialog = null
                    refreshTrigger++
                }
            )
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
                val albumItems = if (screen.openAlbumName == stringResource(id = R.string.album_name_all_favorites)) favoriteItems else taggedAlbums[screen.openAlbumName] ?: emptySet()

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
                            Toast.makeText(context, context.getString(R.string.favorites_exported_successfully), Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, context.getString(R.string.failed_to_export_favorites), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, context.getString(R.string.tags_exported_successfully), Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, context.getString(R.string.failed_to_export_tags), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), Toast.LENGTH_SHORT).show()
                    }
                },
                onImportTags = { importTagsLauncher.launch("application/json") }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
                TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis; showDatePicker = false }) {
                    Text(stringResource(id = R.string.dialog_ok))
                }
            }, dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }) {
                DatePicker(state = datePickerState)
            }
        }

        if (showConfirmDeleteDialog) {
            ConfirmDeleteDialog(onConfirm = {
                if (isClearingTrash) {
                    TrashRepository.clearTrash(context)
                    isClearingTrash = false
                } else {
                    TrashRepository.removeFromTrash(context, itemsToDelete)
                }
                if (isVibrationEnabled) performVibration(context)
                refreshTrigger++
                selectedItems.clear()
                viewerState = null
                showConfirmDeleteDialog = false
            }, onDismiss = { showConfirmDeleteDialog = false
                if (isVibrationEnabled) performVibration(context)
            })
        }

        if (showConfirmTrashDialog) {
            ConfirmTrashDialog(
                onConfirm = {
                    val urisToTrash = itemsToTrash
                    coroutineScope.launch(Dispatchers.IO) {
                        val copiedUris = TrashRepository.copyToTrash(context, urisToTrash)
                        if (copiedUris.isNotEmpty()) {
                            var itemsDeleted = false
                            copiedUris.forEach { uri ->
                                try {
                                    // Удаляем каждый файл индивидуально
                                    if (context.contentResolver.delete(uri, null, null) > 0) {
                                        itemsDeleted = true
                                    }
                                } catch (e: Exception) {
                                    // Можно добавить обработку ошибок для каждого файла
                                }
                            }
                            if (itemsDeleted) {
                                withContext(Dispatchers.Main) {
                                    refreshTrigger++
                                }
                            }
                        }
                    }
                    if (isVibrationEnabled) performVibration(context)
                    selectedItems.clear()
                    itemsToTrash = emptyList()
                    showConfirmTrashDialog = false
                    viewerState = null
                },
                onDismiss = { showConfirmTrashDialog = false
                    if (isVibrationEnabled) performVibration(context)
                }
            )
        }

        if (showConfirmRestoreDialog) {
            ConfirmRestoreDialog(
                onConfirm = {
                    TrashRepository.restoreFromTrash(context, itemsToRestore)
                    selectedItems.clear()
                    refreshTrigger++
                    showConfirmRestoreDialog = false
                    if (isVibrationEnabled) performVibration(context)
                },
                onDismiss = { showConfirmRestoreDialog = false
                    if (isVibrationEnabled) performVibration(context)
                }
            )
        }

        if (showFolderSelectionDialog) {
            FolderSelectionDialog(
                folders = allFolders,
                onDismiss = { showFolderSelectionDialog = false },
                onFolderSelected = { destinationFolder: MediaFolder ->
                    coroutineScope.launch {
                        val folderPath = destinationFolder.items.firstOrNull()?.let {
                            getFolderPathFromUri(context, it.uri)
                        } ?: destinationFolder.name

                        when (currentFileOperation) {
                            FileOperation.COPY -> {
                                filesToProcess.forEach { uri ->
                                    copyMediaToFolder(context, uri, folderPath)
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.copied_to_folder, destinationFolder.name), Toast.LENGTH_SHORT).show()
                                }
                            }
                            FileOperation.MOVE -> {
                                filesToProcess.forEach { uri ->
                                    moveMediaToFolder(context, uri, folderPath)
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.moved_to_folder, destinationFolder.name), Toast.LENGTH_SHORT).show()
                                }
                                viewerState = null
                            }
                            null -> {}
                        }
                        refreshTrigger++
                        showFolderSelectionDialog = false
                        filesToProcess = emptyList()
                        currentFileOperation = null
                    }
                }
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
                            allFolders = sanitizedFoldersState.value,
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
                                tags = TagsRepository.getTags(context)
                            },
                            onEditTag = { oldTag, newTag ->
                                if (isVibrationEnabled) performVibration(context)
                                TagsRepository.renameTag(context, oldTag, newTag)
                                tags = TagsRepository.getTags(context)
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
                                checkForUpdates(true)
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
                                    permissionLauncher.launch(permissionsToRequest)
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