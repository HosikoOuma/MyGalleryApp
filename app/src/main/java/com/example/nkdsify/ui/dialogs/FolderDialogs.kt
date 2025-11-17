package com.example.nkdsify.ui.dialogs

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.nkdsify.FileOperation
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.ui.components.FolderSelectionDialog
import com.example.nkdsify.ui.components.HiddenFoldersDialog
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.ConfirmMoveToSecretDialog
import com.example.nkdsify.ui.utils.SecretRepository
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.copyMediaToFolder
import com.example.nkdsify.ui.utils.getFolderPathFromUri
import com.example.nkdsify.ui.utils.moveMediaToFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FolderDialogs(myAppState: MyAppState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (myAppState.showFolderSelectionDialog) {
        FolderSelectionDialog(
            folders = myAppState.allFolders,
            onDismiss = { myAppState.showFolderSelectionDialog = false },
            onFolderSelected = { destinationFolder: MediaFolder ->
                coroutineScope.launch {
                    myAppState.isProcessing = true
                    val folderPath = destinationFolder.items.firstOrNull()?.let {
                        getFolderPathFromUri(context, it.uri)
                    } ?: destinationFolder.name

                    when (myAppState.currentFileOperation) {
                        FileOperation.COPY -> {
                            myAppState.filesToProcess.forEach { uri ->
                                copyMediaToFolder(context, uri, folderPath)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.copied_to_folder, destinationFolder.name), Toast.LENGTH_SHORT).show()
                            }
                        }
                        FileOperation.MOVE -> {
                            myAppState.filesToProcess.forEach { uri ->
                                moveMediaToFolder(context, uri, folderPath)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.moved_to_folder, destinationFolder.name), Toast.LENGTH_SHORT).show()
                            }
                            myAppState.viewerState = null
                        }
                        null -> {}
                    }
                    myAppState.refreshTrigger++
                    myAppState.showFolderSelectionDialog = false
                    myAppState.filesToProcess = emptyList()
                    myAppState.currentFileOperation = null
                    myAppState.isProcessing = false
                }
            }
        )
    }

    if (myAppState.showHiddenFoldersDialog) {
        HiddenFoldersDialog(
            allFolders = myAppState.allFolders,
            hiddenFolders = myAppState.hiddenFolders,
            onDismiss = { myAppState.showHiddenFoldersDialog = false },
            onFolderHiddenChange = { folderId, isHidden ->
                val newHiddenFolders = if (isHidden) {
                    myAppState.hiddenFolders + folderId
                } else {
                    myAppState.hiddenFolders - folderId
                }
                myAppState.hiddenFolders = newHiddenFolders
                SettingsRepository.setHiddenFolders(context, newHiddenFolders)
            }
        )
    }

    if (myAppState.showConfirmMoveToSecretDialog) {
        ConfirmMoveToSecretDialog(
            onConfirm = {
                myAppState.showConfirmMoveToSecretDialog = false
                BiometricUtils.authenticate(
                    activity = context as AppCompatActivity,
                    onSuccess = {
                        coroutineScope.launch {
                            myAppState.isProcessing = true
                            val itemsToMove = if (myAppState.isSelectionMode) myAppState.selectedItems.toList() else listOfNotNull(myAppState.showDetailsDialog)
                            SecretRepository.moveToSecret(context, itemsToMove)
                            myAppState.showDetailsDialog = null
                            myAppState.selectedItems.clear()
                            myAppState.viewerState = null
                            myAppState.refreshTrigger++
                            myAppState.isProcessing = false
                        }
                    },
                    onError = { _, _ -> /* Do nothing on error */ },
                    onFailed = { /* Do nothing on failure */ }
                )
            },
            onDismiss = { myAppState.showConfirmMoveToSecretDialog = false }
        )
    }
}
