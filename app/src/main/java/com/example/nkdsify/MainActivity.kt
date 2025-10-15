package com.android.nkdsify

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.mygalleryapp.ui.theme.MyPhotoAppTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom

data class MediaItem(val uri: Uri, val isVideo: Boolean)
data class MediaFolder(val id: Long, val name: String, val coverUri: Uri, val items: List<MediaItem>)
data class MediaViewerState(val items: List<MediaItem>, val startIndex: Int)
enum class SortType { DATE_MODIFIED, DATE_ADDED, NAME }

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPhotoAppTheme {
                MyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    val context = LocalContext.current

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

    var currentScreen by remember { mutableStateOf("folders") }
    var selectedFolder by remember { mutableStateOf<MediaFolder?>(null) }

    var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
    var sortAscending by remember { mutableStateOf(false) }

    val favorites = remember {
        val initialFavorites = FavoritesRepository.getFavorites(context).map { Uri.parse(it) }
        mutableStateListOf(*initialFavorites.toTypedArray())
    }

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

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    LaunchedEffect(hasPermissions, sortType, sortAscending) {
        if(hasPermissions) {
            folders = loadMediaFolders(context, sortType, sortAscending)
        }
    }

    LaunchedEffect(folders) {
        if (currentScreen == "images_in_folder" && selectedFolder != null) {
            val updatedFolder = folders.find { it.id == selectedFolder?.id }
            if (updatedFolder != null) {
                selectedFolder = updatedFolder
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        BackHandler(enabled = currentScreen == "images_in_folder") { currentScreen = "folders" }

        val title = when (currentScreen) {
            "folders" -> "В С Ё"
            "images_in_folder" -> selectedFolder?.name ?: ""
            "favorites" -> "NSFW"
            else -> ""
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    modifier = Modifier.statusBarsPadding(),
                    navigationIcon = {
                        if (currentScreen == "images_in_folder") {
                            IconButton(onClick = { currentScreen = "folders" }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (currentScreen == "images_in_folder" || currentScreen == "favorites") {
                            var menuExpanded by remember { mutableStateOf(false) }

                            IconButton(onClick = { sortAscending = !sortAscending }) {
                                Icon(
                                    if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = "Sort Direction"
                                )
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort By")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("По дате изменения") },
                                        onClick = { sortType = SortType.DATE_MODIFIED; menuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("По дате добавления") },
                                        onClick = { sortType = SortType.DATE_ADDED; menuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("По имени") },
                                        onClick = { sortType = SortType.NAME; menuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "Photos") },
                        label = { Text("В С Ё") },
                        selected = currentScreen == "folders" || currentScreen == "images_in_folder",
                        onClick = { currentScreen = "folders" }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                        label = { Text("NSFW") },
                        selected = currentScreen == "favorites",
                        onClick = { currentScreen = "favorites" }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (hasPermissions) {
                    when (currentScreen) {
                        "folders" -> FoldersGrid(folders = folders, imageLoader = imageLoader, onFolderClick = {
                            selectedFolder = it
                            currentScreen = "images_in_folder"
                        })
                        "images_in_folder" -> {
                            selectedFolder?.let { folder ->
                                MediaGrid(items = folder.items, favorites = favorites, imageLoader = imageLoader, onItemClick = { item ->
                                    viewerState = MediaViewerState(items = folder.items, startIndex = folder.items.indexOf(item))
                                })
                            }
                        }
                        "favorites" -> {
                            var favoriteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

                            LaunchedEffect(favorites.toList(), sortType, sortAscending) {
                                favoriteItems = loadFavoriteMediaItems(context, favorites.toSet(), sortType, sortAscending)
                            }

                            MediaGrid(
                                items = favoriteItems,
                                favorites = favorites,
                                imageLoader = imageLoader,
                                onItemClick = { item ->
                                    viewerState = MediaViewerState(items = favoriteItems, startIndex = favoriteItems.indexOf(item))
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Для доступа к медиафайлам необходимо разрешение.")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                                Text("Предоставить разрешение")
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
                onDismiss = { viewerState = null },
                imageLoader = imageLoader
            )
        }
    }
}

fun loadFavoriteMediaItems(context: Context, favoriteUris: Set<Uri>, sortType: SortType, sortAscending: Boolean): List<MediaItem> {
    if (favoriteUris.isEmpty()) {
        return emptyList()
    }

    val favoriteItems = mutableListOf<MediaItem>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val selection = "${MediaStore.Files.FileColumns._ID} IN (${favoriteUris.joinToString { "?" }})"
    val selectionArgs = favoriteUris.map { ContentUris.parseId(it).toString() }.toTypedArray()

    val sortColumn = when (sortType) {
        SortType.DATE_MODIFIED -> MediaStore.Files.FileColumns.DATE_MODIFIED
        SortType.DATE_ADDED -> MediaStore.Files.FileColumns.DATE_ADDED
        SortType.NAME -> MediaStore.Files.FileColumns.DISPLAY_NAME
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val mediaType = cursor.getInt(mediaTypeColumn)
            val uri = ContentUris.withAppendedId(collection, id)
            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            favoriteItems.add(MediaItem(uri, isVideo))
        }
    }
    return favoriteItems
}

fun loadMediaFolders(context: Context, sortType: SortType, sortAscending: Boolean): List<MediaFolder> {
    val foldersMap = mutableMapOf<Long, MutableList<MediaItem>>()
    val folderNames = mutableMapOf<Long, String>()

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val sortColumn = when (sortType) {
        SortType.DATE_MODIFIED -> MediaStore.Files.FileColumns.DATE_MODIFIED
        SortType.DATE_ADDED -> MediaStore.Files.FileColumns.DATE_ADDED
        SortType.NAME -> MediaStore.Files.FileColumns.DISPLAY_NAME
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val bucketId = cursor.getLong(bucketIdColumn)
            val bucketName = cursor.getString(bucketNameColumn)
            val mediaType = cursor.getInt(mediaTypeColumn)

            val uri = ContentUris.withAppendedId(collection, id)
            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val item = MediaItem(uri, isVideo)

            if (!foldersMap.containsKey(bucketId)) {
                foldersMap[bucketId] = mutableListOf()
                folderNames[bucketId] = bucketName
            }
            foldersMap[bucketId]?.add(item)
        }
    }

    return foldersMap.mapNotNull { entry ->
        val folderName = folderNames[entry.key]
        val itemsInFolder = entry.value
        val coverItem = itemsInFolder.firstOrNull { !it.isVideo } ?: itemsInFolder.firstOrNull()
        if (folderName != null && coverItem != null) {
            MediaFolder(id = entry.key, name = folderName, coverUri = coverItem.uri, items = itemsInFolder)
        } else {
            null
        }
    }.sortedBy { it.name }
}

@Composable
fun FoldersGrid(folders: List<MediaFolder>, imageLoader: ImageLoader, onFolderClick: (MediaFolder) -> Unit) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Папок не найдено")
        }
        return
    }
    val gridState = rememberLazyGridState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {        items(folders) { folder ->
            Card(modifier = Modifier.padding(8.dp).aspectRatio(1f).pointerInput(folder) { detectTapGestures(onTap = { _ -> onFolderClick(folder) }) }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column {
                    Image(painter = rememberAsyncImagePainter(model = folder.coverUri, imageLoader = imageLoader), contentDescription = "Folder cover", contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxWidth())
                    Text(text = folder.name, modifier = Modifier.padding(8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        }
        CustomVerticalScrollbar(gridState = gridState, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
    }
}

@Composable
fun MediaGrid(items: List<MediaItem>, favorites: MutableList<Uri>, imageLoader: ImageLoader, onItemClick: (MediaItem) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет медиафайлов")
        }
        return
    }
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                Box(modifier = Modifier.padding(4.dp)) {
                    Card(modifier = Modifier.fillMaxSize().aspectRatio(1f).pointerInput(item) { detectTapGestures(onTap = { _ -> onItemClick(item) }, onLongPress = { _ -> if (favorites.contains(item.uri)) favorites.remove(item.uri) else favorites.add(item.uri); haptics.performHapticFeedback(HapticFeedbackType.LongPress) }) }) {
                        Image(painter = rememberAsyncImagePainter(model = item.uri, imageLoader = imageLoader), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    if (item.isVideo) {
                        Icon(imageVector = Icons.Filled.PlayCircle, contentDescription = "Video", tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(24.dp))
                    }
                    if (favorites.contains(item.uri)) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Избранное", tint = Color.Red, modifier = Modifier.align(Alignment.TopEnd).size(24.dp))
                    }
                }
            }
        }
        CustomVerticalScrollbar(gridState = gridState, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
    }
}

@Composable
private fun CustomVerticalScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isScrollInProgress by remember { derivedStateOf { gridState.isScrollInProgress } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerInput(gridState) {
                detectDragGestures {
                    change, _ ->
                    change.consume()
                    val trackHeight = size.height.toFloat()
                    val dragProgress = (change.position.y / trackHeight).coerceIn(0f, 1f)

                    val totalItems = gridState.layoutInfo.totalItemsCount
                    if (totalItems > 0) {
                        coroutineScope.launch {
                            val targetIndex = (dragProgress * totalItems).toInt()
                            gridState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
    ) {
        AnimatedVisibility(
            visible = isScrollInProgress,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0 || layoutInfo.visibleItemsInfo.isEmpty()) return@AnimatedVisibility

            val visibleItemsCount = layoutInfo.visibleItemsInfo.size.toFloat()
            val scrollableItems = (totalItems - visibleItemsCount).coerceAtLeast(1f)

            val scrollProgress = gridState.firstVisibleItemIndex.toFloat() / scrollableItems

            val thumbHeight = (maxHeight * (visibleItemsCount / totalItems)).coerceAtLeast(20.dp)
            val thumbOffsetY = (maxHeight - thumbHeight) * scrollProgress

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(thumbHeight)
                    .offset(y = thumbOffsetY)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(items: List<MediaItem>, startIndex: Int, onDismiss: () -> Unit, imageLoader: ImageLoader) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) {
            page ->
            val item = items[page]
            if (item.isVideo) {
                VideoPlayerPage(uri = item.uri, isVisible = pagerState.currentPage == page)
            } else {
                ZoomableImage(uri = item.uri, imageLoader = imageLoader)
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

@Composable
fun ZoomableImage(uri: Uri, imageLoader: ImageLoader) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(key1 = uri) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()

                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            val newScale = (scale * zoom).coerceIn(1f, 5f)

                            if (newScale <= 1f) {
                                offsetX = 0f
                                offsetY = 0f
                                scale = 1f
                            } else {
                                val maxOffsetX = (size.width * (newScale - 1)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1)) / 2f

                                val newOffsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                val newOffsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)

                                if (zoom != 1f || pan != androidx.compose.ui.geometry.Offset.Zero) {
                                    event.changes.forEach { it.consume() }
                                }

                                scale = newScale
                                offsetX = newOffsetX
                                offsetY = newOffsetY
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current).data(uri).size(Size.ORIGINAL).build(),
                imageLoader = imageLoader
            ),
            contentDescription = "Full screen image",
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

@Composable
fun VideoPlayerPage(uri: Uri, isVisible: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(uri, isVisible) {
        if (isVisible) {
            exoPlayer.setMediaItem(Media3Item.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = true } },
            update = { playerView -> playerView.player = exoPlayer },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {}
        )
    }
}

object FavoritesRepository {
    private const val PREFS_NAME = "MyGalleryAppPrefs"
    private const val FAVORITES_KEY = "favorites"

    private fun getSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): Set<String> = getSharedPreferences(context).getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()

    fun saveFavorites(context: Context, favorites: Set<String>) {
        getSharedPreferences(context).edit().putStringSet(FAVORITES_KEY, favorites).apply()
    }
}
