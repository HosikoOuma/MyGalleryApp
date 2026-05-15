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
import kotlinx.coroutines.NonCancellable
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
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    myAppState.isProcessing = true
                    val urisToDelete = myAppState.itemsToDelete
                    if (myAppState.isClearingTrash) {
                        TrashRepository.clearTrash(context)
                        withContext(Dispatchers.Main) {
                            myAppState.isClearingTrash = false
                        }
                    } else {
                        TrashRepository.removeFromTrash(context, urisToDelete)
                    }

                    withContext(Dispatchers.Main) {
                        if (isVibrationEnabled) com.example.nkdsify.ui.utils.performVibration(context)
                        myAppState.refreshMedia()
                        myAppState.selectedItems.clear()
                        myAppState.showConfirmDeleteDialog = false

                        updateViewerStateAfterDeletion(
                            viewerState = myAppState.viewerState,
                            urisToDelete = urisToDelete,
                            setViewerState = { myAppState.viewerState = it }
                        )
                        myAppState.itemsToDelete = persistentListOf()
                    }
                } finally {
                    withContext(NonCancellable + Dispatchers.Main) {
                        myAppState.isProcessing = false
                    }
                }
            }
        }, onDismiss = { myAppState.showConfirmDeleteDialog = false })
    }

    if (myAppState.showConfirmDeleteFromSecretDialog) {
        ConfirmDeleteFromSecretDialog(
            onConfirm = {
                BiometricUtils.authenticate(
                    activity = context as AppCompatActivity,
                    onSuccess = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                myAppState.isProcessing = true
                                val urisToDelete = myAppState.itemsToDeleteFromSecret
                                val currentViewerState = myAppState.secretViewerState

                                SecretRepository.deleteFromSecret(context, urisToDelete)
                                val updatedSecretItems = SecretRepository.getSecretMediaItems(context).toImmutableList()

                                withContext(Dispatchers.Main) {
                                    myAppState.secretItems = updatedSecretItems
                                    myAppState.showConfirmDeleteFromSecretDialog = false
                                    myAppState.itemsToDeleteFromSecret = persistentListOf()
                                    myAppState.selectedItems.clear()

                                    updateViewerStateAfterDeletion(
                                        viewerState = currentViewerState,
                                        urisToDelete = urisToDelete,
                                        setViewerState = { myAppState.secretViewerState = it }
                                    )
                                }
                            } finally {
                                withContext(NonCancellable + Dispatchers.Main) {
                                    myAppState.isProcessing = false
                                }
                            }
                        }
                    },
                    onError = { _, _ -> /* Do nothing */ },
                    onFailed = { /* Do nothing */ }
                )
            },
            onDismiss = { myAppState.showConfirmDeleteFromSecretDialog = false }
        )
    }

    if (myAppState.showConfirmTrashDialog) {
        ConfirmTrashDialog(
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        myAppState.isProcessing = true
                        val urisToTrash = myAppState.itemsToTrash
                        val copiedUris = TrashRepository.copyToTrash(context, urisToTrash)
                        if (copiedUris.isNotEmpty()) {
                            var itemsDeleted = false
                            copiedUris.forEach { uri ->
                                try {
                                    if (context.contentResolver.delete(uri, null, null) > 0) {
                                        itemsDeleted = true
                                    }
                                } catch (e: Exception) {}
                            }
                            if (itemsDeleted) {
                                withContext(Dispatchers.Main) {
                                    myAppState.refreshMedia()
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (isVibrationEnabled) com.example.nkdsify.ui.utils.performVibration(context)
                            myAppState.selectedItems.clear()
                            myAppState.itemsToTrash = persistentListOf()
                            myAppState.showConfirmTrashDialog = false

                            updateViewerStateAfterDeletion(
                                viewerState = myAppState.viewerState,
                                urisToDelete = urisToTrash,
                                setViewerState = { myAppState.viewerState = it }
                            )
                        }
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main) {
                            myAppState.isProcessing = false
                        }
                    }
                }
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
    if (viewerState == null || viewerState.items.none { it.uri in urisToDelete }) return
    val originalIndex = viewerState.items.indexOfFirst { it.uri in urisToDelete }
    val newItems = viewerState.items.filterNot { it.uri in urisToDelete }
    if (newItems.isEmpty()) {
        setViewerState(null)
    } else {
        val newIndex = originalIndex.coerceAtMost(newItems.size - 1)
        setViewerState(viewerState.copy(items = newItems.toImmutableList(), startIndex = newIndex))
    }
}
