package com.example.nkdsify.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(val uri: Uri, val isVideo: Boolean)

@Immutable
data class MediaDetails(
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val path: String,
    val resolution: String
)

@Immutable
data class MediaFolder(val id: Long, val name: String, val coverUri: Uri, val items: List<MediaItem>)

@Immutable
data class MediaViewerState(val items: List<MediaItem>, val startIndex: Int, val isExternal: Boolean = false)

enum class SortType { DATE_MODIFIED, DATE_ADDED, NAME }

enum class Theme { SYSTEM, LIGHT, DARK }

sealed class Screen {
    object Folders : Screen()
    data class FolderContent(val folder: MediaFolder) : Screen()
    object Favorites : Screen()
    object Settings : Screen()
}
