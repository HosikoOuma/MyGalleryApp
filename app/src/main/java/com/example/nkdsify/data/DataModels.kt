package com.example.nkdsify.data

import android.net.Uri
import androidx.exifinterface.media.ExifInterface

sealed class Screen {
    data object Folders : Screen()
    data class FolderContent(val folder: MediaFolder) : Screen()
    data class Favorites(val openAlbumName: String? = null) : Screen()
    data object Settings : Screen()
    data object TagManagement : Screen()
    data object Trash : Screen()
    data object AllMedia : Screen()
    data class MediaByTag(val tag: String) : Screen()
    data object SecretStorage : Screen()
    data object ViewHistory : Screen()
}
enum class MediaTypeFilter {
    ALL,
    PHOTOS,
    VIDEOS
}


data class MediaFolder(
    val id: Long,
    val name: String,
    val items: List<MediaItem>,
    val coverUri: Uri? = null,
    val totalSize: Long,
    val dateRange: Pair<Long, Long>,
    val itemCount: Int
)


data class MediaItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean = false,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long
)


data class AlbumDetails(
    val path: String? = null,
    val totalSize: Long,
    val dateRange: Pair<Long, Long>? = null,
    val itemCount: Int
)

data class MediaDetails(
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val path: String,
    val resolution: String,
    val isVideo: Boolean,
    val exif: ExifInterface? = null
)

data class MediaViewerState(
    val items: List<MediaItem>,
    val startIndex: Int,
    val isExternal: Boolean = false
)

enum class SortType {
    ALPHABET,
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
    DOUBLE_TAP,
    PINCH
}

enum class Language(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    RUSSIAN("ru"),
    SPECIAL("xx")
}
