package com.example.nkdsify.data

import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.exifinterface.media.ExifInterface
import com.example.nkdsify.R
import kotlinx.collections.immutable.ImmutableList

sealed class Screen {
    data object Folders : Screen()
    data class FolderContent(val folder: MediaFolder, val scrollToItemUri: Uri? = null) : Screen()
    data class Favorites(val openAlbumName: String? = null) : Screen()
    data object Settings : Screen()
    data object TagManagement : Screen()
    data object Trash : Screen()
    data object AllMedia : Screen()
    data class MediaByTag(val tag: String) : Screen()
    data object SecretStorage : Screen()
    data object ViewHistory : Screen()
    data object About : Screen()
    data object Help : Screen()
}

enum class AppFontFamily {
    SYSTEM,
    JETBRAINS_MONO,
    GOOGLE_SANS
}

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_extrabold, FontWeight.ExtraBold),
)
val GoogleSansFontFamily = FontFamily(
    Font(R.font.gsr, FontWeight.Normal),
    Font(R.font.gsb, FontWeight.Bold),
    Font(R.font.gsm, FontWeight.Medium),
)
enum class MediaTypeFilter {
    ALL,
    PHOTOS,
    VIDEOS
}


data class MediaFolder(
    val id: Long,
    val name: String,
    val items: ImmutableList<MediaItem>,
    val coverUri: Uri? = null,
    val totalSize: Long,
    val dateRange: Pair<Long, Long>,
    val itemCount: Int
)


data class MediaItem(
    val uri: Uri,
    val name: String,
    val absolutePath: String,
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
    val items: ImmutableList<MediaItem>,
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
    DARK,
    AMOLED
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

enum class BlurType {
    BLUR, PLACEHOLDER
}

enum class FabAction {
    SHUFFLE,
    CAMERA
}
