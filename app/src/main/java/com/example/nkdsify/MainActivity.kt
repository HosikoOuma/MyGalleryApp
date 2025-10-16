package com.example.nkdsify

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
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
import com.example.nkdsify.ui.screens.FavoritesScreen
import com.example.nkdsify.ui.screens.FoldersGrid
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.ConfirmDeleteDialog
import com.example.nkdsify.ui.utils.FavoritesRepository
import com.example.nkdsify.ui.utils.deleteMediaPermanently
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant.now

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(initialUri: Uri? = null) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermissions by remember {
        mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

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
    val now = System.currentTimeMillis()
    var favoriteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

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

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshTrigger++
            selectedItems.clear()
            itemsToDelete = emptyList()
            viewerState = null // Dismiss viewer on successful deletion
        }
        showConfirmDeleteDialog = false
    }

    LaunchedEffect(initialUri, hasPermissions) {
        if (initialUri != null && hasPermissions) {
            withContext(Dispatchers.IO) {
                val allFolders = loadMediaFolders(context, sortType, sortAscending, null)
                var targetFolder: MediaFolder? = null
                var targetItemIndex = -1

                val mediaUri = if (initialUri.scheme == "file") {
                    val path = initialUri.path
                    context.contentResolver.query(
                        MediaStore.Files.getContentUri("external"),
                        arrayOf(MediaStore.Files.FileColumns._ID),
                        "${MediaStore.Files.FileColumns.DATA} = ?",
                        arrayOf(path),
                        null
                    )?.use { cursor ->
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

                if (targetFolder != null && targetItemIndex != -1) {
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

    LaunchedEffect(hasPermissions, sortType, sortAscending, selectedDate, favorites.size, refreshTrigger) {
        if (hasPermissions) {
            folders = withContext(Dispatchers.IO) {
                loadMediaFolders(context, sortType, sortAscending, selectedDate)
            }
            favoriteItems = withContext(Dispatchers.IO) {
                loadFavoriteMediaItems(context, favorites.toSet(), sortType, sortAscending, selectedDate)
            }
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis; showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showConfirmDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = {
                val intentSender = deleteMediaPermanently(context, itemsToDelete)
                if (intentSender != null) {
                    deleteRequestLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } else {
                    refreshTrigger++
                    selectedItems.clear()
                    showConfirmDeleteDialog = false
                }
            },
            onDismiss = { showConfirmDeleteDialog = false }
        )
    }

    Box(Modifier.fillMaxSize()) {
        BackHandler(enabled = isSelectionMode) {
            selectedItems.clear()
        }
        BackHandler(enabled = currentScreen is Screen.FolderContent && !isSelectionMode) { currentScreen = Screen.Folders }

        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedItems.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = { selectedItems.clear() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close selection")
                            }
                        },
                        actions = {
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
                                    favorites.removeAll(selectedItems.toList())
                                } else {
                                    selectedItems.forEach { uri ->
                                        if (!favorites.contains(uri)) {
                                            favorites.add(uri)
                                        }
                                    }
                                }
                                selectedItems.clear()
                            }) {
                                Icon(
                                    imageVector = if (isFavoritesScreen) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                                    contentDescription = if (isFavoritesScreen) "Remove from Favorites" else "Add to Favorites"
                                )
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(title) },
                        modifier = Modifier.statusBarsPadding(),
                        navigationIcon = {
                            if (currentScreen is Screen.FolderContent) {
                                IconButton(onClick = { currentScreen = Screen.Folders }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            if (currentScreen !is Screen.FolderContent) { // Simplified condition
                                var menuExpanded by remember { mutableStateOf(false) }

                                IconButton(onClick = { sortAscending = !sortAscending }) {
                                    Icon(
                                        if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = "Sort Direction"
                                    )
                                }

                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Filled.DateRange, contentDescription = "Filter by date")
                                }

                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort By")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("By Date Modified") },
                                            onClick = { sortType = SortType.DATE_MODIFIED; menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("By Date Added") },
                                            onClick = { sortType = SortType.DATE_ADDED; menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("By Name") },
                                            onClick = { sortType = SortType.NAME; menuExpanded = false }
                                        )
                                        if (selectedDate != null) {
                                            DropdownMenuItem(
                                                text = { Text("Reset Date Filter") },
                                                onClick = { selectedDate = null; menuExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "Folders") },
                        label = { Text("Folders") },
                        selected = currentScreen is Screen.Folders || currentScreen is Screen.FolderContent,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentScreen = Screen.Folders
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        selected = currentScreen is Screen.Favorites,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentScreen = Screen.Favorites
                            if (now - lastTap < 500) {
                                tapCount++
                            } else {
                                tapCount = 1
                            }
                            lastTap = now

                            if (tapCount == 5) {
                                tapCount = 0
                                val mediaPlayer = MediaPlayer.create(context, R.raw.uwu)
                                mediaPlayer.setOnCompletionListener { it.release() }
                                mediaPlayer.start()
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (hasPermissions) {
                    when (val screen = currentScreen) {
                        is Screen.Folders -> FoldersGrid(folders = folders, imageLoader = imageLoader, onFolderClick = {
                            currentScreen = Screen.FolderContent(it)
                        })
                        is Screen.FolderContent -> {
                            val folder = folders.find { it.id == screen.folder.id } ?: screen.folder
                            MediaGrid(
                                items = folder.items,
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
                                }
                            )
                        }
                        is Screen.Favorites -> {
                            FavoritesScreen(
                                items = favoriteItems,
                                favorites = favorites,
                                selectedItems = selectedItems,
                                imageLoader = imageLoader,
                                onItemClick = { item ->
                                    viewerState = MediaViewerState(items = favoriteItems, startIndex = favoriteItems.indexOf(item))
                                },
                                onToggleSelection = { item ->
                                    if (selectedItems.contains(item.uri)) {
                                        selectedItems.remove(item.uri)
                                    } else {
                                        selectedItems.add(item.uri)
                                    }
                                }
                            )
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
                onDeletePermanently = { uris ->
                    itemsToDelete = uris
                    showConfirmDeleteDialog = true
                }
            )
        }
    }
}
