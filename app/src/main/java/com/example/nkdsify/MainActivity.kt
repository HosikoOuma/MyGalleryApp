package com.example.nkdsify

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.SortType
import com.example.nkdsify.data.loadFavoriteMediaItems
import com.example.nkdsify.data.loadMediaFolders
import com.example.nkdsify.ui.components.MediaGrid
import com.example.nkdsify.ui.components.MediaViewer
import com.example.nkdsify.ui.components.TagEditDialog
import com.example.nkdsify.ui.screens.FavoritesScreen
import com.example.nkdsify.ui.screens.FoldersGrid
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.ConfirmDeleteDialog
import com.example.nkdsify.ui.utils.FavoritesRepository
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.deleteMediaPermanently
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.ContentUris

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
        setContent {
            NkdsifyAppTheme {
                MyApp(initialUri = initialUri)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MyApp(initialUri: Uri? = null) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermissions by remember { mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) }

    var folders by remember { mutableStateOf<List<MediaFolder>>(emptyList()) }
    var viewerState by remember { mutableStateOf<MediaViewerState?>(null) }

    var tapCount by remember { mutableStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Folders) }

    var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
    var sortAscending by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val favorites = remember {
        val initialFavorites = FavoritesRepository.getFavorites(context).map { it.toUri() }
        mutableStateListOf(*initialFavorites.toTypedArray())
    }
    var favoriteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var tags by remember { mutableStateOf(TagsRepository.getTags(context)) }
    var showTagDialog by remember { mutableStateOf<Uri?>(null) }

    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var itemsToDelete by remember { mutableStateOf<List<Uri>>(emptyList()) }

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
                    favorites.addAll(importedFavorites.map { it.toUri() })
                    refreshTrigger++
                    Toast.makeText(context, "Favorites imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import tags!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { coroutineScope.launch { 
        isRefreshing = true
        delay(1000) // For presentation purposes
        refreshTrigger++ 
        isRefreshing = false
    } })


    LaunchedEffect(initialUri, hasPermissions) {
        if (initialUri != null && hasPermissions) {
            withContext(Dispatchers.IO) {
                val allFolders = loadMediaFolders(context, sortType, sortAscending, null)
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
                    for (folder in allFolders) {
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
                    val isVideo = context.contentResolver.getType(initialUri)?.startsWith("video/") == true
                    viewerState = MediaViewerState(listOf(MediaItem(initialUri, isVideo)), 0, isExternal = true)
                }
            }
        }
    }

    LaunchedEffect(favorites.toList()) {
        val favoriteStrings = favorites.map { it.toString() }.toSet()
        FavoritesRepository.saveFavorites(context, favoriteStrings)
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, refreshTrigger) {
        if (hasPermissions) {
            folders = withContext(Dispatchers.IO) { loadMediaFolders(context, sortType, sortAscending, selectedDate) }
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
        is Screen.Favorites -> "Favorites"
    }
    val isFavoritesScreen = currentScreen is Screen.Favorites

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showTagDialog != null) {
        val uri = showTagDialog!!
        TagEditDialog(initialTags = tags[uri.toString()] ?: emptySet(), onDismiss = { showTagDialog = null }, onSave = { tagSet ->
            TagsRepository.setTagsForItem(context, uri, tagSet)
            tags = TagsRepository.getTags(context)
            showTagDialog = null
        })
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
                refreshTrigger++
                selectedItems.clear()
                showConfirmDeleteDialog = false
            }
        }, onDismiss = { showConfirmDeleteDialog = false })
    }

    Box(Modifier.fillMaxSize()) {
        BackHandler(enabled = isSelectionMode) {
            selectedItems.clear()
        }
        BackHandler(enabled = currentScreen is Screen.FolderContent && !isSelectionMode) { currentScreen = Screen.Folders }

        Scaffold(topBar = {
            if (isSelectionMode) {
                TopAppBar(title = { Text("${selectedItems.size} selected") }, navigationIcon = {
                    IconButton(onClick = { selectedItems.clear() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close selection")
                    }
                }, actions = {
                    IconButton(onClick = {
                        val currentSelected = selectedItems.toList()
                        selectedItems.clear()
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND_MULTIPLE
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(currentSelected))
                            type = "*/*"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        itemsToDelete = selectedItems.toList()
                        showConfirmDeleteDialog = true
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isFavoritesScreen) {
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
                    }) {
                        Icon(imageVector = if (isFavoritesScreen) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                            contentDescription = if (isFavoritesScreen) "Remove from Favorites" else "Add to Favorites")
                    }
                })
            } else {
                TopAppBar(title = { Text(title) }, modifier = Modifier.statusBarsPadding(), navigationIcon = {
                    if (currentScreen is Screen.FolderContent) {
                        IconButton(onClick = { currentScreen = Screen.Folders }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }, actions = {
                    if (isFavoritesScreen) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Export Favorites") }, onClick = { 
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
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to export favorites!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to create backup file!", Toast.LENGTH_SHORT).show()
                                    }
                                    menuExpanded = false
                                })
                                DropdownMenuItem(text = { Text("Import Favorites") }, onClick = { 
                                    importFavoritesLauncher.launch("application/json") 
                                    menuExpanded = false
                                })
                                DropdownMenuItem(text = { Text("Export Tags") }, onClick = { 
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
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to export tags!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to create backup file!", Toast.LENGTH_SHORT).show()
                                    }
                                    menuExpanded = false
                                })
                                DropdownMenuItem(text = { Text("Import Tags") }, onClick = { 
                                    importTagsLauncher.launch("application/json") 
                                    menuExpanded = false
                                })
                            }
                        }
                    }
                    if (currentScreen is Screen.Folders || currentScreen is Screen.Favorites || currentScreen is Screen.FolderContent) {
                        var menuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { sortAscending = !sortAscending }) {
                            Icon(if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Sort Direction")
                        }

                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Filter by date")
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("By Date Modified") },
                                    onClick = { sortType = SortType.DATE_MODIFIED; menuExpanded = false })
                                DropdownMenuItem(text = { Text("By Date Added") },
                                    onClick = { sortType = SortType.DATE_ADDED; menuExpanded = false })
                                DropdownMenuItem(text = { Text("By Name") },
                                    onClick = { sortType = SortType.NAME; menuExpanded = false })
                                if (selectedDate != null) {
                                    DropdownMenuItem(text = { Text("Reset Date Filter") },
                                        onClick = { selectedDate = null; menuExpanded = false })
                                }
                            }
                        }
                    }
                })
            }
        }, bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "Folders") },
                    label = { Text("Folders") },
                    selected = currentScreen is Screen.Folders || currentScreen is Screen.FolderContent,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); currentScreen = Screen.Folders })
                NavigationBarItem(icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = currentScreen is Screen.Favorites,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentScreen = Screen.Favorites
                        val now = System.currentTimeMillis()
                        if (now - lastTap < 500) {
                            tapCount++
                        } else {
                            tapCount = 1
                        }
                        lastTap = now

                        if (tapCount == 10) {
                            tapCount = 0
                            Toast.makeText(context, "UwU", Toast.LENGTH_SHORT).show()
                            val mediaPlayer = MediaPlayer.create(context, R.raw.uwu)
                            mediaPlayer.setOnCompletionListener { it.release() }
                            mediaPlayer.start()
                        }
                    })
            }
        }) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).pullRefresh(pullRefreshState)) {
                if (hasPermissions) {
                    when (val screen = currentScreen) {
                        is Screen.Folders -> FoldersGrid(folders = folders, imageLoader = imageLoader, onFolderClick = {
                            currentScreen = Screen.FolderContent(it)
                        })
                        is Screen.FolderContent -> {
                            val folder = folders.find { it.id == screen.folder.id } ?: screen.folder
                            MediaGrid(items = folder.items,
                                favorites = favorites,
                                selectedItems = selectedItems,
                                imageLoader = imageLoader,
                                onItemClick = { item ->
                                    viewerState = MediaViewerState(items = folder.items, startIndex = folder.items.indexOf(item))
                                },
                                onToggleSelection = { item ->
                                    if (selectedItems.contains(item.uri)) {
                                        selectedItems.remove(item.uri)
                                    } else {
                                        selectedItems.add(item.uri)
                                    }
                                })
                        }
                        is Screen.Favorites -> {
                            FavoritesScreen(items = favoriteItems,
                                favorites = favorites,
                                selectedItems = selectedItems,
                                imageLoader = imageLoader,
                                tags = tags,
                                onItemClick = { item ->
                                    viewerState = MediaViewerState(items = favoriteItems, startIndex = favoriteItems.indexOf(item))
                                },
                                onToggleSelection = { item ->
                                    if (selectedItems.contains(item.uri)) {
                                        selectedItems.remove(item.uri)
                                    } else {
                                        selectedItems.add(item.uri)
                                    }
                                })
                        }
                    }
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
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        if (viewerState != null) {
            BackHandler { viewerState = null }
            MediaViewer(items = viewerState!!.items,
                startIndex = viewerState!!.startIndex,
                favorites = favorites,
                onDismiss = { viewerState = null },
                imageLoader = imageLoader,
                isExternal = viewerState!!.isExternal, // Pass the flag
                onDeletePermanently = { uris ->
                    itemsToDelete = uris
                    showConfirmDeleteDialog = true
                },
                onShowTagDialog = { uri -> showTagDialog = uri },
                onToggleFavorite = { uri ->
                    if (favorites.contains(uri)) {
                        favorites.remove(uri)
                    } else {
                        favorites.add(uri)
                    }
                })
        }
    }
}