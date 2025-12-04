package com.example.nkdsify.ui.dialogs

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.ConfirmDeleteDialog
import com.example.nkdsify.ui.utils.ConfirmDeleteFromSecretDialog
import com.example.nkdsify.ui.utils.ConfirmTrashDialog
import com.example.nkdsify.ui.utils.SecretRepository
import com.example.nkdsify.ui.utils.TrashRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeletionDialogs(
    myAppState: MyAppState,
    isVibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (myAppState.showConfirmDeleteDialog) {
        ConfirmDeleteDialog(onConfirm = {
            val urisToDelete = myAppState.itemsToDelete
            if (myAppState.isClearingTrash) {
                TrashRepository.clearTrash(context)
                myAppState.isClearingTrash = false
            } else {
                TrashRepository.removeFromTrash(context, urisToDelete)
            }

            if (isVibrationEnabled) com.example.nkdsify.ui.utils.performVibration(context)
            myAppState.refreshTrigger++
            myAppState.selectedItems.clear()
            myAppState.showConfirmDeleteDialog = false

            updateViewerStateAfterDeletion(
                viewerState = myAppState.viewerState,
                urisToDelete = urisToDelete,
                setViewerState = { myAppState.viewerState = it }
            )
            myAppState.itemsToDelete = persistentListOf()
        }, onDismiss = { myAppState.showConfirmDeleteDialog = false })
    }

    if (myAppState.showConfirmDeleteFromSecretDialog) {
        ConfirmDeleteFromSecretDialog(
            onConfirm = {
                BiometricUtils.authenticate(
                    activity = context as AppCompatActivity,
                    onSuccess = {
                        coroutineScope.launch {
                            val urisToDelete = myAppState.itemsToDeleteFromSecret
                            val currentViewerState = myAppState.secretViewerState

                            SecretRepository.deleteFromSecret(context, urisToDelete)
                            myAppState.secretItems = withContext(Dispatchers.IO) { SecretRepository.getSecretMediaItems(context).toImmutableList() }

                            withContext(Dispatchers.Main) {
                                myAppState.showConfirmDeleteFromSecretDialog = false
                                myAppState.itemsToDeleteFromSecret = persistentListOf()
                                myAppState.selectedItems.clear()

                                updateViewerStateAfterDeletion(
                                    viewerState = currentViewerState,
                                    urisToDelete = urisToDelete,
                                    setViewerState = { myAppState.secretViewerState = it }
                                )
                            }
                        }
                    },
                    onError = { _, _ -> /* Do nothing on error */ },
                    onFailed = { /* Do nothing on failure */ }
                )
            },
            onDismiss = { myAppState.showConfirmDeleteFromSecretDialog = false }
        )
    }

    if (myAppState.showConfirmTrashDialog) {
        ConfirmTrashDialog(
            onConfirm = {
                val urisToTrash = myAppState.itemsToTrash
                coroutineScope.launch(Dispatchers.IO) {
                    val copiedUris = TrashRepository.copyToTrash(context, urisToTrash)
                    if (copiedUris.isNotEmpty()) {
                        var itemsDeleted = false
                        copiedUris.forEach { uri ->
                            try {
                                if (context.contentResolver.delete(uri, null, null) > 0) {
                                    itemsDeleted = true
                                }
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                        if (itemsDeleted) {
                            withContext(Dispatchers.Main) {
                                myAppState.refreshTrigger++
                            }
                        }
                    }
                }
                if (isVibrationEnabled) com.example.nkdsify.ui.utils.performVibration(context)
                myAppState.selectedItems.clear()
                myAppState.itemsToTrash = persistentListOf()
                myAppState.showConfirmTrashDialog = false

                updateViewerStateAfterDeletion(
                    viewerState = myAppState.viewerState,
                    urisToDelete = urisToTrash,
                    setViewerState = { myAppState.viewerState = it }
                )
            },
            onDismiss = { myAppState.showConfirmTrashDialog = false }
        )
    }
}

private fun updateViewerStateAfterDeletion(
    viewerState: MediaViewerState?,
    urisToDelete: List<Uri>,
    setViewerState: (MediaViewerState?) -> Unit
) {
    if (viewerState == null || viewerState.items.none { it.uri in urisToDelete }) {
        return
    }

    val originalIndex = viewerState.items.indexOfFirst { it.uri in urisToDelete }
    val newItems = viewerState.items.filterNot { it.uri in urisToDelete }

    if (newItems.isEmpty()) {
        setViewerState(null)
    } else {
        val newIndex = originalIndex.coerceAtMost(newItems.size - 1)
        setViewerState(viewerState.copy(items = newItems.toImmutableList(), startIndex = newIndex))
    }
}
