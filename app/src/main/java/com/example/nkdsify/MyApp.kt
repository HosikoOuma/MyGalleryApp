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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.loadAllMedia
import com.example.nkdsify.data.loadFavoriteMediaItems
import com.example.nkdsify.data.loadMediaFolders
import com.example.nkdsify.data.loadTrashedMediaItems
import com.example.nkdsify.ui.MyAppFAB
import com.example.nkdsify.ui.components.MediaViewer
import com.example.nkdsify.ui.components.utils.rememberCoilImageLoader
import com.example.nkdsify.ui.dialogs.DeletionDialogs
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.FavoritesRepository
import com.example.nkdsify.ui.utils.MigrationUtils
import com.example.nkdsify.ui.utils.SecretRepository
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.TrashRepository
import com.example.nkdsify.ui.utils.ViewHistoryRepository
import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.performVibration
import com.example.nkdsify.ui.utils.sanitizeFolders
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MyApp(myAppState: MyAppState, initialUri: Uri? = null, screenWidth: Int, screenHeight: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        myAppState.isProcessing = true
        MigrationUtils.runMigrationIfNeeded(context)
        myAppState.isProcessing = false
    }
    LaunchedEffect(Unit) {
        myAppState.checkForUpdates(false)
    }
    LaunchedEffect(myAppState.selectedLanguage) {
        if (myAppState.selectedLanguage.code != SettingsRepository.getLanguage(context).code) {
            SettingsRepository.setLanguage(context, myAppState.selectedLanguage)
            (context as? Activity)?.recreate()
        }
    }
    NkdsifyAppTheme(
        theme = myAppState.selectedTheme,
        appFontFamily = myAppState.selectedFontFamily
    ) {
        val foldersGridState = rememberLazyGridState()
        val folderContentGridState = rememberLazyGridState()
        val favoritesGridState = rememberLazyGridState()
        val favoritesContentGridState = rememberLazyGridState()
        val trashGridState = rememberLazyGridState()
        val allMediaGridState = rememberLazyGridState()
        val secretGridState = rememberLazyGridState()
        val viewHistoryGridState = rememberLazyGridState()
        val hiddenFoldersListState = rememberLazyListState()
        
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        val isNavBarVisible by remember {
            derivedStateOf {
                val currentGridState = when (val screen = myAppState.currentScreen) {
                    is Screen.Folders -> foldersGridState
                    is Screen.FolderContent -> folderContentGridState
                    is Screen.Favorites -> if (screen.openAlbumName != null) favoritesContentGridState else favoritesGridState
                    is Screen.Trash -> trashGridState
                    is Screen.AllMedia -> allMediaGridState
                    is Screen.SecretStorage -> secretGridState
                    is Screen.ViewHistory -> viewHistoryGridState
                    is Screen.HiddenFolders -> hiddenFoldersListState
                    else -> null
                }
                val isScrolling = currentGridState?.isScrollInProgress ?: false
                !isScrolling || myAppState.isSelectionMode
            }
        }

        val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                myAppState.hasManageStoragePermission = Environment.isExternalStorageManager()
            }
        }

        val favorites = remember {
            val initialFavorites = FavoritesRepository.getFavorites(context)
            mutableStateListOf(*initialFavorites.toTypedArray())
        }
        myAppState.favoritesList.clear()
        myAppState.favoritesList.addAll(favorites)

        myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
        val onAddNewTag: (String) -> Unit = { newTag ->
            if (myAppState.isVibrationEnabled) performVibration(context)
            TagsRepository.addNewTag(context, newTag)
            myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
        }
        val onMoveTag: (Int, Int) -> Unit = { from, to ->
            if (from in myAppState.allTags.indices && to in myAppState.allTags.indices) {
                val mutableTags = myAppState.allTags.toMutableList()
                val movedTag = mutableTags.removeAt(from)
                mutableTags.add(to, movedTag)
                myAppState.allTags = mutableTags.toImmutableList()
                TagsRepository.saveAllTags(context, mutableTags)
            }
        }
        val imageLoader = rememberCoilImageLoader(context)

        myAppState.imageLoader = imageLoader
        myAppState.coroutineScope = coroutineScope
        myAppState.keyboardController = keyboardController
        myAppState.foldersGridState = foldersGridState
        myAppState.folderContentGridState = folderContentGridState
        myAppState.favoritesGridState = favoritesGridState
        myAppState.favoritesContentGridState = favoritesContentGridState
        myAppState.trashGridState = trashGridState
        myAppState.allMediaGridState = allMediaGridState
        myAppState.secretGridState = secretGridState
        myAppState.viewHistoryGridState = viewHistoryGridState
        myAppState.hiddenFoldersListState = hiddenFoldersListState

        myAppState.onAddNewTag = onAddNewTag
        myAppState.onMoveTag = onMoveTag
        myAppState.onFontFamilyChange = {
            myAppState.selectedFontFamily = it
            SettingsRepository.setFontFamily(context, it)
        }
        myAppState.onFabActionChange = {
            myAppState.selectedFabAction = it
            SettingsRepository.setFabAction(context, it)
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            myAppState.hasPermissions = permissions.values.all { it }
        }

        val pullToRefreshEnabled = myAppState.currentScreen !is Screen.Settings && myAppState.currentScreen !is Screen.TagManagement
        
        val pullRefreshState = key(myAppState.currentScreen) {
            rememberPullToRefreshState()
        }
        
        LaunchedEffect(pullRefreshState.isRefreshing) {
            if (pullRefreshState.isRefreshing) {
                if (pullToRefreshEnabled) {
                    delay(1000)
                    myAppState.refreshTrigger++
                }
                pullRefreshState.endRefresh()
            }
        }

        LaunchedEffect(myAppState.allFolders) {
            myAppState.sanitizedFoldersState.value = sanitizeFolders(myAppState.allFolders, context).toImmutableList()
        }
        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                if (myAppState.autoDeleteTrashEnabled) {
                    val days = myAppState.autoDeleteTrashDays
                    TrashRepository.deleteExpired(context, days)
                }
            }
        }

        LaunchedEffect(myAppState.currentScreen, myAppState.refreshTrigger, myAppState.allMedia) {
            if (myAppState.currentScreen is Screen.SecretStorage) {
                myAppState.secretItems = SecretRepository.getSecretMediaItems(context).toImmutableList()
            }
            if (myAppState.currentScreen is Screen.ViewHistory) {
                val historyWithTimestamps = ViewHistoryRepository.getHistory(context)
                val allMediaMap by lazy { myAppState.allMedia.associateBy { it.uri.toString() } }
                myAppState.viewHistory = historyWithTimestamps.mapNotNull { historyItem ->
                    allMediaMap[historyItem.uri]
                }.toImmutableList()
            }
        }
        val filteredViewHistory by remember(myAppState.viewHistory, myAppState.searchQuery, myAppState.isSearchActive) {
            derivedStateOf {
                if (myAppState.isSearchActive && myAppState.searchQuery.isNotEmpty() && myAppState.currentScreen is Screen.ViewHistory) {
                    myAppState.viewHistory.filter { it.name.contains(myAppState.searchQuery, ignoreCase = true) }
                } else {
                    myAppState.viewHistory
                }
            }
        }
        LaunchedEffect(filteredViewHistory) {
            myAppState.filteredViewHistory = filteredViewHistory.toImmutableList()
        }
        LaunchedEffect(initialUri, myAppState.hasPermissions) {
            if (initialUri != null && myAppState.hasPermissions) {
                snapshotFlow { myAppState.allMedia }
                    .filter { it.isNotEmpty() }
                    .first()

                withContext(Dispatchers.IO) {
                    val details = getMediaDetails(context, initialUri)
                    val externalPath = details?.path
                    val foundItem = myAppState.allMedia.find { it.absolutePath == externalPath }

                    if (foundItem != null) {
                        val folder = myAppState.allFolders.find { it.items.contains(foundItem) }
                        if (folder != null) {
                            val indexInFolder = folder.items.indexOf(foundItem)
                            myAppState.viewerState = MediaViewerState(folder.items, indexInFolder)
                        } else {
                            myAppState.viewerState = MediaViewerState(persistentListOf(foundItem), 0)
                        }
                    } else {
                        val name = details?.name ?: ""
                        val path = details?.path ?: ""
                        val isVideo = context.contentResolver.getType(initialUri)?.startsWith("video/") == true
                        myAppState.viewerState = MediaViewerState(
                            persistentListOf(MediaItem(initialUri, name, path, isVideo, 0, 0, 0)),
                            0,
                            isExternal = true
                        )
                    }
                }
            }
        }
        LaunchedEffect(favorites.toList()) {
            FavoritesRepository.saveFavorites(context, favorites.toSet())
        }
        LaunchedEffect(myAppState.currentScreen) {
            val screen = myAppState.currentScreen as? Screen.FolderContent
            if (screen?.scrollToItemUri != null) {
                val index = screen.folder.items.indexOfFirst { it.uri == screen.scrollToItemUri }
                if (index != -1) {
                    coroutineScope.launch {
                        folderContentGridState.scrollToItem(index)
                    }
                }
            } else if (myAppState.currentScreen is Screen.FolderContent) {
                coroutineScope.launch {
                    folderContentGridState.scrollToItem(0)
                }
            }
            if (myAppState.currentScreen is Screen.ViewHistory) {
                coroutineScope.launch {
                    viewHistoryGridState.scrollToItem(0)
                }
            }
            if (myAppState.currentScreen is Screen.Favorites && (myAppState.currentScreen as Screen.Favorites).openAlbumName != null) {
                coroutineScope.launch {
                    favoritesContentGridState.scrollToItem(0)
                }
            }
            if (myAppState.currentScreen !is Screen.FolderContent && myAppState.currentScreen !is Screen.Favorites && myAppState.currentScreen !is Screen.ViewHistory) {
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
        LaunchedEffect(myAppState.hasPermissions, myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate, myAppState.hiddenFolders, myAppState.refreshTrigger, myAppState.revelationModeEnabled) {
            if (myAppState.hasPermissions) {
                val hiddenFolders = if (myAppState.revelationModeEnabled) emptySet() else myAppState.hiddenFolders
                myAppState.allFolders = withContext(Dispatchers.IO) { loadMediaFolders(context, myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate) }
                myAppState.trashedItems = withContext(Dispatchers.IO) { loadTrashedMediaItems(context, myAppState.sortType, myAppState.sortAscending) }
                myAppState.allMedia = withContext(Dispatchers.IO) { loadAllMedia(context, myAppState.sortType, myAppState.sortAscending, hiddenFolders, myAppState.selectedDate) }
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
        LaunchedEffect(myAppState.hasPermissions, myAppState.sortType, myAppState.sortAscending, myAppState.selectedDate, favorites.size, myAppState.refreshTrigger, myAppState.hiddenFolders, myAppState.revelationModeEnabled) {
            if (myAppState.hasPermissions) {
                val hiddenFolders = if (myAppState.revelationModeEnabled) emptySet() else myAppState.hiddenFolders
                myAppState.favoriteItems = withContext(Dispatchers.IO) { loadFavoriteMediaItems(context, favorites.toSet(), myAppState.sortType, myAppState.sortAscending, hiddenFolders, myAppState.selectedDate) }
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
            ContextCompat.registerReceiver(context, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

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
            is Screen.ViewHistory -> stringResource(id = R.string.view_history_title)
            is Screen.About -> stringResource(id = R.string.about_button)
            is Screen.Help -> stringResource(id = R.string.help_button)
            is Screen.HiddenFolders -> stringResource(id = R.string.manage_hidden_folders_button)
        }

        OthersDialogs(myAppState = myAppState)
        if (myAppState.showClearHistoryDialog) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { myAppState.showClearHistoryDialog = false },
                sheetState = sheetState,
                windowInsets = WindowInsets(0) // Убираем отступ, чтобы BS был во весь экран
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding() // Паддинг для системного бара
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.clear_history_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.clear_history_confirmation),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { myAppState.showClearHistoryDialog = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(id = R.string.dialog_cancel))
                        }
                        Button(
                            onClick = {
                                ViewHistoryRepository.clearHistory(context)
                                myAppState.viewHistory = persistentListOf()
                                myAppState.showClearHistoryDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(id = R.string.dialog_clear))
                        }
                    }
                }
            }
        }
        InfoDialogs(    myAppState = myAppState,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            onFind = {
                myAppState.showDetailsDialog?.let { uri ->
                    val folder = myAppState.allFolders.find { it.items.any { item -> item.uri == uri } }
                    if (folder != null) {
                        myAppState.previousViewerState = myAppState.viewerState
                        myAppState.viewerState = null
                        myAppState.currentScreen = Screen.FolderContent(folder, scrollToItemUri = uri)
                        myAppState.showDetailsDialog = null
                    }
                }
            }
        )
        FolderDialogs(myAppState = myAppState)
        TagDialogs(
            myAppState = myAppState,
            onAddNewTag = onAddNewTag,
            favorites = favorites,
            isVibrationEnabled = myAppState.isVibrationEnabled
        )
        DeletionDialogs(myAppState = myAppState, isVibrationEnabled = myAppState.isVibrationEnabled)
        RestorationDialogs(myAppState = myAppState, isVibrationEnabled = myAppState.isVibrationEnabled)


        Box(Modifier.fillMaxSize()) {

            MyAppBackHandler(myAppState = myAppState)
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MyAppTopBar(
                        myAppState = myAppState,
                        title = title,
                        favorites = favorites,
                        context = context,
                        isVibrationEnabled = myAppState.isVibrationEnabled,
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = { },
                floatingActionButton = { },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                            isNavBarVisible = isNavBarVisible 
                        )

                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(id = R.string.permission_required_message))
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    if (myAppState.isVibrationEnabled) performVibration(context)
                                    permissionLauncher.launch(myAppState.permissionsToRequest)
                                }) {
                                    Text(stringResource(id = R.string.grant_permission_button))
                                }
                            }
                        }
                    }
                    
                    if (pullToRefreshEnabled) {
                        PullToRefreshContainer(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    val isCollapsed = scrollBehavior.state.collapsedFraction > 0.1f
                                    val isActivelyRefreshing = pullRefreshState.isRefreshing || pullRefreshState.verticalOffset > 0.5f
                                    this.alpha = if (isCollapsed || !isActivelyRefreshing) 0f else 1f
                                },
                            state = pullRefreshState,
                        )
                    }
                }
            }
            
            MyAppBottomBar(
                myAppState = myAppState,
                context = context,
                isVibrationEnabled = myAppState.isVibrationEnabled,
                isVisible = isNavBarVisible,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            val fabBottomPadding by animateDpAsState(
                targetValue = if (isNavBarVisible) 104.dp else 16.dp, 
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "FabBottomPadding"
            )
            val safePadding = if (fabBottomPadding < 0.dp) 0.dp else fabBottomPadding

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = safePadding)
            ) {
                MyAppFAB(
                    myAppState = myAppState
                )
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
                    myAppState = myAppState,
                    items = myAppState.viewerState!!.items,
                    startIndex = myAppState.viewerState!!.startIndex,
                    favorites = favorites,
                    imageLoader = imageLoader,
                    isExternal = myAppState.viewerState!!.isExternal,
                    isTrashMode = isTrashViewing
                )
             }
             if (myAppState.secretViewerState != null) {
                 BackHandler { myAppState.secretViewerState = null }
                MediaViewer(
                    myAppState = myAppState,
                    items = myAppState.secretViewerState!!.items,
                    startIndex = myAppState.secretViewerState!!.startIndex,
                    favorites = mutableListOf(), 
                    imageLoader = imageLoader,
                    isSecretMode = true
                )
             }
         }
     }
 }
