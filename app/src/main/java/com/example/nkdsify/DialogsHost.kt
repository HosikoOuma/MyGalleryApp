package com.example.nkdsify

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.data.AlbumDetails
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.ui.components.AlbumDetailsDialog
import com.example.nkdsify.ui.components.BackupAndRestoreDialog
import com.example.nkdsify.ui.components.EasterEggDialog
import com.example.nkdsify.ui.components.FolderSelectionDialog
import com.example.nkdsify.ui.components.HiddenFoldersDialog
import com.example.nkdsify.ui.components.TagEditDialog
import com.example.nkdsify.ui.components.UpdateDialog
import com.example.nkdsify.ui.utils.ConfirmDeleteDialog
import com.example.nkdsify.ui.utils.ConfirmRestoreDialog
import com.example.nkdsify.ui.utils.ConfirmTrashDialog
import com.example.nkdsify.ui.utils.MediaDetailsDialog
import com.example.nkdsify.ui.utils.RenameDialog
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.getMediaDetails
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogsHost(
    context: Context,
    coroutineScope: CoroutineScope,
    // Update
    showUpdateDialog: Boolean,
    latestVersion: String?,
    onDismissUpdate: () -> Unit,
    onOpenReleasePage: (String) -> Unit,
    onDoNotShowUpdateAgain: () -> Unit,
    // Tag dialogs
    showTagDialog: Uri?,
    onDismissTagDialog: () -> Unit,
    onSaveTagsForItem: (Uri, Set<String>) -> Unit,
    tagsMap: Map<String, Set<String>>,
    // Bulk tag
    showBulkTagDialog: Boolean,
    onDismissBulkTagDialog: () -> Unit,
    onSaveBulkTags: (List<Uri>, Set<String>) -> Unit,
    selectedItemsForBulk: List<Uri>,
    // Details / MediaDetails
    showDetailsDialog: Uri?,
    onDismissDetailsDialog: () -> Unit,
    launchCropForWallpaper: (com.canhub.cropper.CropImageContractOptions) -> Unit,
    onCopyFromDetails: (Uri) -> Unit,
    onMoveFromDetails: (Uri) -> Unit,
    onRenameFromDetails: (Uri) -> Unit,
    // Rename
    showRenameDialog: Uri?,
    onDismissRenameDialog: () -> Unit,
    onRenameItem: (Uri, String) -> Unit,
    // Album details
    showAlbumDetailsDialog: Boolean,
    albumDetailsProvider: () -> AlbumDetails?,
    onDismissAlbumDetails: () -> Unit,
    // Easter egg
    showEasterEggDialog: Boolean,
    onDismissEasterEgg: () -> Unit,
    // Hidden folders
    showHiddenFoldersDialog: Boolean,
    allFolders: List<MediaFolder>,
    hiddenFolders: Set<String>,
    onDismissHiddenFolders: () -> Unit,
    onFolderHiddenChange: (String, Boolean) -> Unit,
    // Backup and restore
    showBackupAndRestoreDialog: Boolean,
    onDismissBackupAndRestore: () -> Unit,
    onExportFavorites: () -> Unit,
    onImportFavorites: () -> Unit,
    onExportTags: () -> Unit,
    onImportTags: () -> Unit,
    // Date picker
    showDatePicker: Boolean,
    datePickerStateProvider: () -> androidx.compose.material3.DatePickerState,
    onDateSelected: (Long?) -> Unit,
    onDatePickerDismiss: () -> Unit,
    // Confirmations
    showConfirmDeleteDialog: Boolean,
    onConfirmDelete: () -> Unit,
    onDismissConfirmDelete: () -> Unit,
    showConfirmTrashDialog: Boolean,
    onConfirmTrash: () -> Unit,
    onDismissConfirmTrash: () -> Unit,
    showConfirmRestoreDialog: Boolean,
    onConfirmRestore: () -> Unit,
    onDismissConfirmRestore: () -> Unit,
    // Folder selection
    showFolderSelectionDialog: Boolean,
    onDismissFolderSelection: () -> Unit,
    onFolderSelected: (MediaFolder) -> Unit,
    // Extras: provide favorites/tags where needed
    favorites: MutableList<Uri>,
    favoriteItems: List<MediaItem>,
    tags: Map<String, Set<String>>
) {
    // Update dialog
    if (showUpdateDialog && latestVersion != null) {
        UpdateDialog(
            onDismiss = onDismissUpdate,
            onConfirm = { onOpenReleasePage(latestVersion) },
            onDoNotShowAgain = onDoNotShowUpdateAgain,
            latestVersion = latestVersion
        )
    }

    // Tag edit dialog
    if (showTagDialog != null) {
        val uri = showTagDialog
        TagEditDialog(
            initialTags = TagsRepository.getTagsForItem(context, uri!!),
            allTags = tags.values.flatten().toSet(),
            onDismiss = onDismissTagDialog,
            onSave = { tagSet ->
                onSaveTagsForItem(uri, tagSet)
            }
        )
    }

    // Bulk tag dialog
    if (showBulkTagDialog) {
        val uris = selectedItemsForBulk
        val commonTags = if (uris.isNotEmpty()) uris.map { TagsRepository.getTagsForItem(context, it) }.reduce { acc, set -> acc.intersect(set) } else emptySet()
        TagEditDialog(
            initialTags = commonTags,
            allTags = tags.values.flatten().toSet(),
            onDismiss = onDismissBulkTagDialog,
            onSave = { newTags -> onSaveBulkTags(uris, newTags) }
        )
    }

    // Details dialog
    if (showDetailsDialog != null) {
        val uri = showDetailsDialog!!
        val details = getMediaDetails(context, uri)
        if (details != null) {
            MediaDetailsDialog(
                details = details,
                onDismiss = onDismissDetailsDialog,
                onSetAsWallpaper = {
                    val cropOptions = com.canhub.cropper.CropImageContractOptions(uri, com.canhub.cropper.CropImageOptions(
                        guidelines = com.canhub.cropper.CropImageView.Guidelines.ON
                    ))
                    launchCropForWallpaper(cropOptions)
                },
                onCopy = { onCopyFromDetails(uri) },
                onMove = { onMoveFromDetails(uri) },
                onRename = { onRenameFromDetails(uri) }
            )
        }
    }

    // Rename dialog
    if (showRenameDialog != null) {
        val uri = showRenameDialog!!
        val currentName = getMediaDetails(context, uri)?.name ?: ""
        RenameDialog(
            currentName = currentName,
            onDismiss = onDismissRenameDialog,
            onRename = { newName -> onRenameItem(uri, newName); onDismissRenameDialog() }
        )
    }

    // Album details
    if (showAlbumDetailsDialog) {
        val details = albumDetailsProvider()
        if (details != null) {
            AlbumDetailsDialog(details = details, onDismiss = onDismissAlbumDetails)
        }
    }

    // Easter egg
    if (showEasterEggDialog) {
        EasterEggDialog(onDismiss = onDismissEasterEgg)
    }

    // Hidden folders
    if (showHiddenFoldersDialog) {
        HiddenFoldersDialog(
            allFolders = allFolders,
            hiddenFolders = hiddenFolders,
            onDismiss = onDismissHiddenFolders,
            onFolderHiddenChange = onFolderHiddenChange
        )
    }

    // Backup and restore
    if (showBackupAndRestoreDialog) {
        BackupAndRestoreDialog(
            onDismiss = onDismissBackupAndRestore,
            onExportFavorites = onExportFavorites,
            onImportFavorites = onImportFavorites,
            onExportTags = onExportTags,
            onImportTags = onImportTags
        )
    }

    // Date picker
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = onDatePickerDismiss, confirmButton = {
            TextButton(onClick = {
                val selected = datePickerStateProvider().selectedDateMillis
                onDateSelected(selected)
                onDatePickerDismiss()
            }) { Text(stringResource(id = R.string.dialog_ok)) }
        }, dismissButton = {
            TextButton(onClick = onDatePickerDismiss) { Text(stringResource(id = R.string.dialog_cancel)) }
        }) {
            DatePicker(state = datePickerStateProvider())
        }
    }

    // Confirm delete
    if (showConfirmDeleteDialog) {
        ConfirmDeleteDialog(onConfirm = {
            onConfirmDelete()
        }, onDismiss = {
            onDismissConfirmDelete()
        })
    }

    // Confirm trash
    if (showConfirmTrashDialog) {
        ConfirmTrashDialog(onConfirm = { onConfirmTrash() }, onDismiss = { onDismissConfirmTrash() })
    }

    // Confirm restore
    if (showConfirmRestoreDialog) {
        ConfirmRestoreDialog(onConfirm = { onConfirmRestore() }, onDismiss = { onDismissConfirmRestore() })
    }

    // Folder selection dialog
    if (showFolderSelectionDialog) {
        FolderSelectionDialog(folders = allFolders, onDismiss = onDismissFolderSelection, onFolderSelected = { onFolderSelected(it) })
    }
}
