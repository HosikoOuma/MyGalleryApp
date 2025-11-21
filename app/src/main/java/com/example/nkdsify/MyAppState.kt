package com.example.nkdsify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.nkdsify.data.AppFontFamily
import com.example.nkdsify.data.FabAction
import com.example.nkdsify.data.MediaTypeFilter
import com.example.nkdsify.data.Language
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.data.SortType
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.GithubUpdateChecker
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.TagsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberMyAppState(
    context: Context = LocalContext.current
): MyAppState {
    return remember(context) {
        MyAppState(context)
    }
}

class MyAppState(
    private val context: Context
) {
    var selectedFontFamily by mutableStateOf(SettingsRepository.getFontFamily(context))
    var selectedZoomType by mutableStateOf(SettingsRepository.getZoomType(context))
    var selectedBlurType by mutableStateOf(SettingsRepository.getBlurType(context))
    var isShowFileCountEnabled by mutableStateOf(SettingsRepository.isShowFileCountEnabled(context))
    var isShuffleButtonVisible by mutableStateOf(SettingsRepository.isShuffleButtonVisible(context))
    var selectedLanguage by mutableStateOf(SettingsRepository.getLanguage(context))
    var selectedFabAction by mutableStateOf(SettingsRepository.getFabAction(context))
    var mediaTypeFilter by mutableStateOf(MediaTypeFilter.ALL)
    var selectedTheme by mutableStateOf(SettingsRepository.getTheme(context))
    var searchQuery by mutableStateOf("")
    var isSearchActive by mutableStateOf(false)
    var isProcessing by mutableStateOf(false)
    var showSelectionDetailsDialog by mutableStateOf(false)
    var selectionDetails by mutableStateOf("")
    var showClearHistoryDialog by mutableStateOf(false)
    var viewHistory by mutableStateOf<List<MediaItem>>(emptyList())

    var showUpdateDialog by mutableStateOf(false)
    var latestVersion by mutableStateOf<String?>(null)
    val currentVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    var hasPermissions by mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    var hasManageStoragePermission by mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true)
    var allFolders by mutableStateOf<List<MediaFolder>>(emptyList())
    var allMedia by mutableStateOf<List<MediaItem>>(emptyList())
    var viewerState by mutableStateOf<MediaViewerState?>(null)
    var currentScreen by mutableStateOf<Screen>(Screen.Folders)
    val sanitizedFoldersState = mutableStateOf<List<MediaFolder>>(allFolders)
    var sortType by mutableStateOf(SortType.DATE_MODIFIED)
    var sortAscending by mutableStateOf(false)
    var selectedDate by mutableStateOf<Long?>(null)
    var refreshTrigger by mutableIntStateOf(0)
    var isMuteVideoByDefault by mutableStateOf(SettingsRepository.isMuteVideoByDefault(context))
    var hiddenFolders by mutableStateOf(SettingsRepository.getHiddenFolders(context))
    var showTagDialog by mutableStateOf<Uri?>(null)
    var showBulkTagDialog by mutableStateOf(false)
    var showDetailsDialog by mutableStateOf<Uri?>(null)
    var showAlbumDetailsDialog by mutableStateOf(false)
    var showConfirmDeleteDialog by mutableStateOf(false)
    var itemsToDeleteFromSecret by mutableStateOf<List<Uri>>(emptyList())
    var showConfirmDeleteFromSecretDialog by mutableStateOf(false)
    var showConfirmTrashDialog by mutableStateOf(false)
    var showConfirmRestoreDialog by mutableStateOf(false)
    var showEasterEggDialog by mutableStateOf(false)
    var showHiddenFoldersDialog by mutableStateOf(false)
    var showBackupAndRestoreDialog by mutableStateOf(false)
    var showFolderSelectionDialog by mutableStateOf(false)
    var showRenameDialog by mutableStateOf<Uri?>(null)
    var filesToProcess by mutableStateOf<List<Uri>>(emptyList())
    var currentFileOperation by mutableStateOf<FileOperation?>(null)
    var showDatePicker by mutableStateOf(false)
    var easterEggTapCount by mutableIntStateOf(0)
    var itemsToDelete by mutableStateOf<List<Uri>>(emptyList())
    var isClearingTrash by mutableStateOf(false)
    var itemsToTrash by mutableStateOf<List<Uri>>(emptyList())
    var secretItems by mutableStateOf<List<MediaItem>>(emptyList())
    var secretViewerState by mutableStateOf<MediaViewerState?>(null)
    var itemsToRestore by mutableStateOf<List<Uri>>(emptyList())
    var itemsToRestoreFromSecret by mutableStateOf<List<Uri>>(emptyList())
    var showConfirmRestoreFromSecretDialog by mutableStateOf(false)
    var showConfirmMoveToSecretDialog by mutableStateOf(false)
    var isSettingWallpaper by mutableStateOf(false)
    var showAddDialog by mutableStateOf(false)
    var favoriteItems by mutableStateOf<List<MediaItem>>(emptyList())
    var trashedItems by mutableStateOf<List<MediaItem>>(emptyList())
    var tags by mutableStateOf(TagsRepository.getTags(context))
    var allTags by mutableStateOf<List<String>>(emptyList())
    val selectedItems = mutableStateListOf<Uri>()
    val isSelectionMode get() = selectedItems.isNotEmpty()

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

    var downloadUrl by mutableStateOf<String?>(null)

    suspend fun checkForUpdates(isTriggeredByUser: Boolean) {
        if (!isTriggeredByUser && !SettingsRepository.isCheckForUpdatesOnStartupEnabled(context)) {
            return
        }
        withContext(Dispatchers.IO) {
            val release = GithubUpdateChecker.getLatestRelease("HosikoOuma", "MyGalleryApp")
            release?.let {
                if (compareVersionNames(it.tag_name, currentVersion) > 0) {
                    val apkAsset = it.assets.find { asset -> asset.name.endsWith(".apk") }
                    if (apkAsset != null) {
                        withContext(Dispatchers.Main) {
                            latestVersion = it.tag_name
                            downloadUrl = apkAsset.browser_download_url
                            showUpdateDialog = true
                           // showUpdateNotification(context, it.tag_name)
                        }
                    }
                } else {
                    if (isTriggeredByUser) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.no_updates_available),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}