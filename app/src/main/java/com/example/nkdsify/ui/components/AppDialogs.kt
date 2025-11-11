package com.example.nkdsify.ui.components

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.nkdsify.FileOperation
import com.example.nkdsify.R
import com.example.nkdsify.ui.screens.*
import com.example.nkdsify.data.AlbumDetails
import com.example.nkdsify.data.*
import com.example.nkdsify.ui.*
import com.example.nkdsify.ui.utils.*
import com.example.nkdsify.ui.components.*

import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.performVibration
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialogs(
    showDetailsDialog: Uri?,
    showRenameDialog: Uri?,
    showAlbumDetailsDialog: Boolean,
    showEasterEggDialog: Boolean,
    showHiddenFoldersDialog: Boolean,
    showBackupAndRestoreDialog: Boolean,
    showDatePicker: Boolean,
    showConfirmDeleteDialog: Boolean,
    showConfirmTrashDialog: Boolean,
    showConfirmRestoreDialog: Boolean,
    showFolderSelectionDialog: Boolean,
    isClearingTrash: Boolean,
    isVibrationEnabled: Boolean,
    currentScreen: Screen,
    favoriteItems: List<MediaItem>,
    tags: Map<String, Set<String>>,
    allFolders: List<MediaFolder>,
    hiddenFolders: Set<String>,
    favorites: List<Uri>,
    importFavoritesLauncher: ActivityResultLauncher<String>,
    importTagsLauncher: ActivityResultLauncher<String>,
    datePickerState: DatePickerState,
    currentFileOperation: FileOperation?,
    filesToProcess: List<Uri>,
    onDismiss: (DialogType) -> Unit,
    onConfirm: (DialogType, Any?) -> Unit,
    onSetWallpaper: (Uri) -> Unit,
    onRename: (Uri, String) -> Unit,
    onFolderHiddenChange: (String, Boolean) -> Unit,
    coroutineScope: CoroutineScope,
    itemsToTrash: List<Uri>,
    itemsToRestore: List<Uri>,
    itemsToDelete: List<Uri>,
    screenWidth: Int,
    screenHeight: Int,
    cropImageLauncher: ActivityResultLauncher<CropImageContractOptions>
) {
    val context = LocalContext.current

    if (showDetailsDialog != null) {
        val uri = showDetailsDialog
        val details = getMediaDetails(context, uri)
        if (details != null) {
            MediaDetailsDialog(
                details = details,
                onDismiss = { onDismiss(DialogType.DETAILS) },
                onSetAsWallpaper = {
                    val cropOptions = CropImageContractOptions(uri, CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        fixAspectRatio = true,
                        aspectRatioX = screenWidth,
                        aspectRatioY = screenHeight,
                        outputRequestWidth = screenWidth,
                        outputRequestHeight = screenHeight,
                        outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT
                    ))
                    cropImageLauncher.launch(cropOptions)
                    onSetWallpaper(uri)
                },
                onCopy = {
                    onConfirm(DialogType.DETAILS, FileOperation.COPY)
                },
                onMove = {
                    onConfirm(DialogType.DETAILS, FileOperation.MOVE)
                },
                onRename = {
                    onConfirm(DialogType.DETAILS, "rename")
                }
            )
        }
    }

    if (showRenameDialog != null) {
        val uri = showRenameDialog
        val currentName = getMediaDetails(context, uri)?.name ?: ""
        RenameDialog(
            currentName = currentName,
            onDismiss = { onDismiss(DialogType.RENAME) },
            onRename = { newName ->
                onRename(uri, newName)
            }
        )
    }

    if (showAlbumDetailsDialog) {
        val screen = currentScreen
        if (screen is Screen.FolderContent) {
            val folder = screen.folder
            val path = getMediaDetails(context, folder.items.first().uri)?.path?.substringBeforeLast('/') ?: ""
            AlbumDetailsDialog(
                details = AlbumDetails(path, folder.totalSize, folder.dateRange, folder.itemCount),
                onDismiss = { onDismiss(DialogType.ALBUM_DETAILS) })
        } else if (screen is Screen.Favorites && screen.openAlbumName != null) {
            val taggedAlbums = favoriteItems
                .flatMap { item -> (tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                .groupBy({ it.first }, { it.second })
            val albumItems = if (screen.openAlbumName == context.getString(R.string.album_name_all_favorites)) favoriteItems else taggedAlbums[screen.openAlbumName] ?: emptySet()

            if (albumItems.isNotEmpty()) {
                val totalSize = albumItems.sumOf { it.size }
                AlbumDetailsDialog(
                    details = AlbumDetails(totalSize = totalSize, itemCount = albumItems.size),
                    onDismiss = { onDismiss(DialogType.ALBUM_DETAILS) })
            }
        }
    }

    if (showEasterEggDialog) {
        EasterEggDialog(onDismiss = { onDismiss(DialogType.EASTER_EGG) })
    }

    if (showHiddenFoldersDialog) {
        HiddenFoldersDialog(
            allFolders = allFolders,
            hiddenFolders = hiddenFolders,
            onDismiss = { onDismiss(DialogType.HIDDEN_FOLDERS) },
            onFolderHiddenChange = onFolderHiddenChange
        )
    }

    if (showBackupAndRestoreDialog) {
        BackupAndRestoreDialog(
            onDismiss = { onDismiss(DialogType.BACKUP_AND_RESTORE) },
            onExportFavorites = {
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
                        Toast.makeText(context, context.getString(R.string.favorites_exported_successfully), Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, context.getString(R.string.failed_to_export_favorites), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), Toast.LENGTH_SHORT).show()
                }
            },
            onImportFavorites = { importFavoritesLauncher.launch("application/json") },
            onExportTags = {
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
                        Toast.makeText(context, context.getString(R.string.tags_exported_successfully), Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, context.getString(R.string.failed_to_export_tags), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), Toast.LENGTH_SHORT).show()
                }
            },
            onImportTags = { importTagsLauncher.launch("application/json") }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { onDismiss(DialogType.DATE_PICKER) }, confirmButton = {
            TextButton(onClick = {
                onConfirm(DialogType.DATE_PICKER, datePickerState.selectedDateMillis)
            }) {
                Text(stringResource(id = R.string.dialog_ok))
            }
        }, dismissButton = {
            TextButton(onClick = { onDismiss(DialogType.DATE_PICKER) }) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }) {
            DatePicker(state = datePickerState)
        }
    }

    if (showConfirmDeleteDialog) {
        ConfirmDeleteDialog(onConfirm = {
            if (isClearingTrash) {
                TrashRepository.clearTrash(context)
            } else {
                TrashRepository.removeFromTrash(context, itemsToDelete)
            }
            if (isVibrationEnabled) performVibration(context)
            onConfirm(DialogType.CONFIRM_DELETE, null)
        }, onDismiss = { onDismiss(DialogType.CONFIRM_DELETE)
            if (isVibrationEnabled) performVibration(context)
        })
    }

    if (showConfirmTrashDialog) {
        ConfirmTrashDialog(
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    val copiedUris = TrashRepository.copyToTrash(context, itemsToTrash)
                    if (copiedUris.isNotEmpty()) {
                        var itemsDeleted = false
                        copiedUris.forEach { uri ->
                            try {
                                if (context.contentResolver.delete(uri, null, null) > 0) {
                                    itemsDeleted = true
                                }
                            } catch (e: Exception) {
                            }
                        }
                        if (itemsDeleted) {
                            withContext(Dispatchers.Main) {
                                onConfirm(DialogType.CONFIRM_TRASH, null)
                            }
                        }
                    }
                }
                if (isVibrationEnabled) performVibration(context)
            },
            onDismiss = { onDismiss(DialogType.CONFIRM_TRASH)
                if (isVibrationEnabled) performVibration(context)
            }
        )
    }

    if (showConfirmRestoreDialog) {
        ConfirmRestoreDialog(
            onConfirm = {
                TrashRepository.restoreFromTrash(context, itemsToRestore)
                if (isVibrationEnabled) performVibration(context)
                onConfirm(DialogType.CONFIRM_RESTORE, null)
            },
            onDismiss = { onDismiss(DialogType.CONFIRM_RESTORE)
                if (isVibrationEnabled) performVibration(context)
            }
        )
    }

    if (showFolderSelectionDialog) {
        FolderSelectionDialog(
            folders = allFolders,
            onDismiss = { onDismiss(DialogType.FOLDER_SELECTION) },
            onFolderSelected = { destinationFolder: MediaFolder ->
                onConfirm(DialogType.FOLDER_SELECTION, destinationFolder)
            }
        )
    }
}

enum class DialogType {
    DETAILS,
    RENAME,
    ALBUM_DETAILS,
    EASTER_EGG,
    HIDDEN_FOLDERS,
    BACKUP_AND_RESTORE,
    DATE_PICKER,
    CONFIRM_DELETE,
    CONFIRM_TRASH,
    CONFIRM_RESTORE,
    FOLDER_SELECTION
}
