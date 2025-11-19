@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.nkdsify

import com.example.nkdsify.ui.MyAppTopBar
import com.example.nkdsify.ui.MyAppBottomBar
import com.example.nkdsify.ui.MyAppBackHandler
import com.example.nkdsify.ui.dialogs.RestorationDialogs
import com.example.nkdsify.ui.dialogs.TagDialogs
import com.example.nkdsify.ui.dialogs.FolderDialogs
import com.example.nkdsify.ui.dialogs.InfoDialogs
import com.example.nkdsify.ui.dialogs.OthersDialogs
import com.example.nkdsify.ui.MyAppNavigation
import android.app.Activity
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.canhub.cropper.CropImageContract
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.loadAllMedia
import com.example.nkdsify.data.loadFavoriteMediaItems
import com.example.nkdsify.data.loadMediaFolders
import com.example.nkdsify.data.loadTrashedMediaItems
import com.example.nkdsify.ui.MyAppFAB
import com.example.nkdsify.ui.components.EasterEggDialog
import com.example.nkdsify.ui.components.MediaViewer
import com.example.nkdsify.ui.dialogs.DeletionDialogs
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.EncryptedImageDecoder
import com.example.nkdsify.ui.utils.FavoritesRepository
import com.example.nkdsify.ui.utils.SecretRepository
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.TrashRepository
import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.performVibration
import com.example.nkdsify.ui.utils.sanitizeFolders
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.collections.isNotEmpty
import kotlin.collections.map

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
          autoDeleteTrashDays: Int, onAutoDeleteTrashDaysChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val myAppState = rememberMyAppState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        myAppState.checkForUpdates(false)
    }
    LaunchedEffect(myAppState.selectedLanguage) {
        if (myAppState.selectedLanguage.code != SettingsRepository.getLanguage(context).code) {
            SettingsRepository.setLanguage(context, myAppState.selectedLanguage)
            (context as? Activity)?.recreate()
        }
    }
    NkdsifyAppTheme(theme = myAppState.selectedTheme) {
        LaunchedEffect(myAppState.viewerState) {
            onViewerOpenChange(myAppState.viewerState != null)
        }
        val foldersGridState = rememberLazyGridState()
        val folderContentGridState = rememberLazyGridState()
        val favoritesGridState = rememberLazyGridState()
        val favoritesContentGridState = rememberLazyGridState()
        val trashGridState = rememberLazyGridState()
        val allMediaGridState = rememberLazyGridState()
        val secretGridState = rememberLazyGridState()
        val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                myAppState.hasManageStoragePermission = Environment.isExternalStorageManager()
            }
        }

        val favorites = remember {
            val initialFavorites = FavoritesRepository.getFavorites(context).map { it.toUri() }
            mutableStateListOf(*initialFavorites.toTypedArray())
        }
        myAppState.allTags = TagsRepository.getAllTags(context)
        val onAddNewTag: (String) -> Unit = { newTag ->
            if (isVibrationEnabled) performVibration(context)
            TagsRepository.addNewTag(context, newTag)
            myAppState.allTags = TagsRepository.getAllTags(context)
        }
        val onMoveTag: (Int, Int) -> Unit = { from, to ->
            if (from in myAppState.allTags.indices && to in myAppState.allTags.indices) {
                val mutableTags = myAppState.allTags.toMutableList()
                val movedTag = mutableTags.removeAt(from)
                mutableTags.add(to, movedTag)
                myAppState.allTags = mutableTags
                TagsRepository.saveAllTags(context, mutableTags)
            }
        }
        val imageLoader = remember(context) {
            ImageLoader.Builder(context)
                .components {
                    add(EncryptedImageDecoder.Factory(context))
                    add(ImageDecoderDecoder.Factory())
                    add(GifDecoder.Factory())
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            myAppState.hasPermissions = permissions.values.all { it }
        }

        val pullToRefreshEnabled = myAppState.currentScreen !is Screen.Settings && myAppState.currentScreen !is Screen.TagManagement
        val pullRefreshState = rememberPullToRefreshState()
        if (pullToRefreshEnabled && pullRefreshState.isRefreshing) {
            LaunchedEffect(true) {
                delay(1000)
                myAppState.refreshTrigger++
                pullRefreshState.endRefresh()
            }
        }
        LaunchedEffect(myAppState.allFolders) {
            myAppState.sanitizedFoldersState.value = sanitizeFolders(myAppState.allFolders, context)
        }
        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                if (SettingsRepository.isAutoDeleteTrashEnabled(context)) {
                    val days = SettingsRepository.getAutoDeleteTrashDays(context)
                    TrashRepository.deleteExpired(context, days)
                }
            }
        }
        LaunchedEffect(myAppState.currentScreen, myAppState.refreshTrigger) {
            if (myAppState.currentScreen is Screen.SecretStorage) {
                myAppState.secretItems = SecretRepository.getSecretMediaItems(context)
            }
        }
        LaunchedEffect(initialUri, myAppState.hasPermissions) {
            if (initialUri != null && myAppState.hasPermissions) {
                withContext(Dispatchers.IO) {
                    val loadedFolders = loadMediaFolders(context, myAppState.sortType, myAppState.sortAscending, null)
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
                        myAppState.viewerState = MediaViewerState(targetFolder.items, targetItemIndex)
                    } else {
                        val details = getMediaDetails(context, initialUri)
                        val name = details?.name ?: ""
                        val isVideo = context.contentResolver.getType(initialUri)?.startsWith("video/") == true
                        myAppState.viewerState = MediaViewerState(listOf(MediaItem(initialUri, name, isVideo, 0, 0, 0)), 0, isExternal = true)
                    }
                }
            }
        }
        LaunchedEffect(favorites.toList()) {
            val favoriteStrings = favorites.map { it.toString() }.toSet()
            FavoritesRepository.saveFavorites(context, favoriteStrings)
        }
        LaunchedEffect(myAppState.currentScreen) {
            if (myAppState.currentScreen is Screen.FolderContent) {
                coroutineScope.launch {
                    folderContentGridState.scrollToItem(0)
                }
            }
            if (myAppState.currentScreen is Screen.Favorites && (myAppState.currentScreen as Screen.Favorites).openAlbumName != null) {
                coroutineScope.launch {
                    favoritesContentGridState.scrollToItem(0)
                }
            }
            if (myAppState.currentScreen !is Screen.FolderContent && myAppState.currentScreen !is Screen.Favorites) {
                myAppState.isSearchActive = false
                myAppState.searchQuery = ""
            }
            if (myAppState.currentScreen !is Screen.Trash) {
                myAppState.selectedItems.clear()
            }
        }
        LaunchedEffect(Unit) {
            if (!myAppState.hasPermissions) {
                permissionLauncher.launch(myAppState.permissionsToRequest)
            }
            if (!myAppState.hasManageStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${context.packageName}".toUri()
                    manageStorageLauncher.launch(intent)
                }
            }
        }
        LaunchedEffect(myAppState.hasPermissions, myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate, myAppState.hiddenFolders, myAppState.refreshTrigger) {
            if (myAppState.hasPermissions) {
                myAppState.allFolders = withContext(Dispatchers.IO) { loadMediaFolders(context, myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate) }
                myAppState.trashedItems = withContext(Dispatchers.IO) { loadTrashedMediaItems(context, myAppState.sortType, myAppState.sortAscending) }
                myAppState.allMedia = withContext(Dispatchers.IO) { loadAllMedia(context, myAppState.sortType, myAppState.sortAscending, myAppState.hiddenFolders, myAppState.selectedDate) }
            }
        }
        LaunchedEffect(myAppState.allFolders) {
            val screen = myAppState.currentScreen
            if (screen is Screen.FolderContent) {
                val updatedFolder = myAppState.allFolders.find { it.id == screen.folder.id }
                if (updatedFolder == null || updatedFolder.items.isEmpty()) {
                    myAppState.currentScreen = Screen.Folders
                } else {
                    if (screen.folder != updatedFolder) {
                        myAppState.currentScreen = Screen.FolderContent(updatedFolder)
                    }
                }
            }
        }
        LaunchedEffect(myAppState.hasPermissions, myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate, favorites.size, myAppState.refreshTrigger) {
            if (myAppState.hasPermissions) {
                myAppState.favoriteItems = withContext(Dispatchers.IO) { loadFavoriteMediaItems(context, favorites.toSet(), myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate) }
            }
        }
        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != -1L) {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val uri = downloadManager.getUriForDownloadedFile(id)
                        if (uri != null) {
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(installIntent)
                        }
                    }
                }
            }
            val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(receiver, intentFilter)
            }

            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        val title = when (val screen = myAppState.currentScreen) {
            is Screen.Folders -> stringResource(id = R.string.screen_title_folders)
            is Screen.FolderContent -> screen.folder.name
            is Screen.Favorites -> screen.openAlbumName ?: stringResource(id = R.string.screen_title_favorites)
            is Screen.Settings -> stringResource(id = R.string.screen_title_settings)
            is Screen.TagManagement -> stringResource(id = R.string.screen_title_manage_tags)
            is Screen.Trash -> stringResource(id = R.string.screen_title_trash)
            is Screen.AllMedia -> stringResource(id = R.string.screen_title_all_media)
            is Screen.MediaByTag -> screen.tag
            is Screen.SecretStorage -> stringResource(id = R.string.secret_storage)
        }

        OthersDialogs(myAppState = myAppState)
        InfoDialogs(myAppState = myAppState, screenWidth = screenWidth, screenHeight = screenHeight)
        FolderDialogs(myAppState = myAppState)
        TagDialogs(
            myAppState = myAppState,
            onAddNewTag = onAddNewTag,
            isVibrationEnabled = isVibrationEnabled,
            favorites = favorites
        )
        DeletionDialogs(myAppState = myAppState, isVibrationEnabled = isVibrationEnabled)
        RestorationDialogs(myAppState = myAppState, isVibrationEnabled = isVibrationEnabled)


        Box(Modifier.fillMaxSize()) {
            MyAppBackHandler(myAppState = myAppState)
            Scaffold(
                topBar = {
                    MyAppTopBar(
                        myAppState = myAppState,
                        isVibrationEnabled = isVibrationEnabled,
                        title = title,
                        favorites = favorites,
                        context = context
                    )
                },
                bottomBar = {
                    MyAppBottomBar(
                        myAppState = myAppState,
                        context = context,
                        isVibrationEnabled = isVibrationEnabled
                    )
                },
                floatingActionButton = {
                    MyAppFAB(
                        myAppState = myAppState,
                        isVibrationEnabled = isVibrationEnabled,
                        useLargeFab = useLargeFab,
                        coroutineScope = coroutineScope,
                        context = context
                    )
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
                    if (myAppState.hasPermissions) {
                        MyAppNavigation(
                            myAppState = myAppState,
                            imageLoader = imageLoader,
                            foldersGridState = foldersGridState,
                            folderContentGridState = folderContentGridState,
                            favoritesGridState = favoritesGridState,
                            favoritesContentGridState = favoritesContentGridState,
                            trashGridState = trashGridState,
                            allMediaGridState = allMediaGridState,
                            secretGridState = secretGridState,
                            favorites = favorites,
                            keyboardController = keyboardController,
                            onMoveTag = onMoveTag,
                            onAddNewTag = onAddNewTag,
                            isVibrationEnabled = isVibrationEnabled,
                            isBlurEnabled = isBlurEnabled,
                            onBlurEnabledChange = onBlurEnabledChange,
                            isBlurInFolderEnabled = isBlurInFolderEnabled,
                            onBlurInFolderEnabledChange = onBlurInFolderEnabledChange,
                            isTrashBlurEnabled = isTrashBlurEnabled,
                            onTrashBlurEnabledChange = onTrashBlurEnabledChange,
                            isBlurAllMediaEnabled = isBlurAllMediaEnabled,
                            onBlurAllMediaEnabledChange = onBlurAllMediaEnabledChange,
                            onVibrationEnabledChange = onVibrationEnabledChange,
                            isShakeToBlurEnabled = isShakeToBlurEnabled,
                            onShakeToBlurEnabledChange = onShakeToBlurEnabledChange,
                            isLoopVideoEnabled = isLoopVideoEnabled,
                            onLoopVideoEnabledChange = onLoopVideoEnabledChange,
                            isSwipeToDismissEnabled = isSwipeToDismissEnabled,
                            onSwipeToDismissEnabledChange = onSwipeToDismissEnabledChange,
                            useLargeFab = useLargeFab,
                            onUseLargeFabChange = onUseLargeFabChange,
                            autoDeleteTrashEnabled = autoDeleteTrashEnabled,
                            onAutoDeleteTrashEnabledChange = onAutoDeleteTrashEnabledChange,
                            autoDeleteTrashDays = autoDeleteTrashDays,
                            onAutoDeleteTrashDaysChange = onAutoDeleteTrashDaysChange
                        )

                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(id = R.string.permission_required_message))
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    if (isVibrationEnabled) performVibration(context)
                                    permissionLauncher.launch(myAppState.permissionsToRequest)
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

            if (myAppState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (myAppState.viewerState != null) {
                BackHandler { myAppState.viewerState = null }
                val isTrashViewing = myAppState.viewerState?.items?.map { it.uri }?.intersect(myAppState.trashedItems.map { it.uri }.toSet())?.isNotEmpty() ?: false
                MediaViewer(
                    items = myAppState.viewerState!!.items,
                    startIndex = myAppState.viewerState!!.startIndex,
                    favorites = favorites,
                    onDismiss = { myAppState.viewerState = null },
                    imageLoader = imageLoader,
                    isExternal = myAppState.viewerState!!.isExternal,
                    isTrashMode = isTrashViewing,
                    onDelete = { uris ->
                        if (isTrashViewing) {
                            myAppState.itemsToDelete = uris
                            myAppState.showConfirmDeleteDialog = true
                        } else {
                            myAppState.itemsToTrash = uris
                            myAppState.showConfirmTrashDialog = true
                        }
                    },
                    onRestore = { uris ->
                        myAppState.itemsToRestore = uris
                        myAppState.showConfirmRestoreDialog = true
                    },
                    onShowTagDialog = { uri -> myAppState.showTagDialog = uri },
                    onShowDetails = { uri -> myAppState.showDetailsDialog = uri },
                    onToggleFavorite = { uri ->
                        if (favorites.contains(uri)) {
                            favorites.remove(uri)
                        } else {
                            favorites.add(uri)
                        }
                    },
                    isMuteVideoByDefault = myAppState.isMuteVideoByDefault,
                    zoomType = myAppState.selectedZoomType,
                    isLoopVideoEnabled = isLoopVideoEnabled,
                    isSwipeToDismissEnabled = isSwipeToDismissEnabled
                )
            }
            if (myAppState.secretViewerState != null) {
                BackHandler { myAppState.secretViewerState = null }
                MediaViewer(
                    items = myAppState.secretViewerState!!.items,
                    startIndex = myAppState.secretViewerState!!.startIndex,
                    favorites = emptyList(), // No favorites in secret mode
                    onDismiss = { myAppState.secretViewerState = null },
                    imageLoader = imageLoader,
                    isSecretMode = true,
                    onRestore = { uris ->
                        myAppState.itemsToRestoreFromSecret = uris
                        myAppState.showConfirmRestoreFromSecretDialog = true
                    },
                    onDelete = { uris ->
                        myAppState.itemsToDeleteFromSecret = uris
                        myAppState.showConfirmDeleteFromSecretDialog = true
                    },

                    // Dummy parameters not used in secret mode
                    onShowTagDialog = {},
                    onToggleFavorite = {},
                    onShowDetails = {},
                    isTrashMode = false,
                    isMuteVideoByDefault = myAppState.isMuteVideoByDefault,
                    zoomType = myAppState.selectedZoomType,
                    isLoopVideoEnabled = isLoopVideoEnabled,
                    isSwipeToDismissEnabled = isSwipeToDismissEnabled
                )
            }
        }
    }
}
