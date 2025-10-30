package com.example.nkdsify.data

import android.net.Uri

sealed class Screen {
    data object Folders : Screen()
    data class FolderContent(val folder: MediaFolder) : Screen()
    data object Favorites : Screen()
    data object Settings : Screen()
    data object TagManagement : Screen()
    data object Trash : Screen()
    data object AllMedia : Screen()
    data class Edit(val uri: Uri) : Screen()
}


data class MediaFolder(
    val id: Long,
    val name: String,
    val items: List<MediaItem>,
    val coverUri: Uri? = null
)

data class MediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean = false
)

data class MediaDetails(
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val path: String,
    val resolution: String
)

data class MediaViewerState(
    val items: List<MediaItem>,
    val startIndex: Int,
    val isExternal: Boolean = false
)

enum class SortType {
    NAME,
    DATE_MODIFIED,
    DATE_ADDED,
    SIZE
}

enum class Theme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ZoomType {
    PINCH,
    DOUBLE_TAP
}


