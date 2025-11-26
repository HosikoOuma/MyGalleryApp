package com.example.nkdsify.ui.dialogs

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.nkdsify.MyAppState
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.ConfirmRestoreDialog
import com.example.nkdsify.ui.utils.ConfirmRestoreFromSecretDialog
import com.example.nkdsify.ui.utils.SecretRepository
import com.example.nkdsify.ui.utils.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RestorationDialogs(myAppState: MyAppState, isVibrationEnabled: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (myAppState.showConfirmRestoreFromSecretDialog) {
        ConfirmRestoreFromSecretDialog(
            onConfirm = {
                myAppState.showConfirmRestoreFromSecretDialog = false
                BiometricUtils.authenticate(
                    activity = context as AppCompatActivity,
                    onSuccess = {
                        coroutineScope.launch {
                            myAppState.isProcessing = true
                            SecretRepository.restoreFromSecret(context, myAppState.itemsToRestoreFromSecret)
                            // Обновляем список секретных файлов напрямую
                            myAppState.secretItems = withContext(Dispatchers.IO) { SecretRepository.getSecretMediaItems(context) }

                            // Сбрасываем состояния
                            myAppState.itemsToRestoreFromSecret = emptyList()
                            myAppState.secretViewerState = null
                            myAppState.selectedItems.clear()
                            myAppState.isProcessing = false
                        }
                    },
                    onError = { _, _ -> /* Do nothing on error */ },
                    onFailed = { /* Do nothing on failure */ }
                )
            },
            onDismiss = { myAppState.showConfirmRestoreFromSecretDialog = false }
        )
    }

    if (myAppState.showConfirmRestoreDialog) {
        ConfirmRestoreDialog(
            onConfirm = {
                val urisToRestore = myAppState.itemsToRestore
                val currentViewerState = myAppState.viewerState
                val isRestoringFromViewer = currentViewerState != null && currentViewerState.items.any { it.uri in urisToRestore }

                TrashRepository.restoreFromTrash(context, urisToRestore)
                myAppState.selectedItems.clear()
                myAppState.refreshTrigger++
                myAppState.showConfirmRestoreDialog = false
                if (isVibrationEnabled) com.example.nkdsify.ui.utils.performVibration(context)

                if (isRestoringFromViewer) {
                    val originalIndex = currentViewerState.items.indexOfFirst { it.uri in urisToRestore }
                    val newItems = currentViewerState.items.filterNot { it.uri in urisToRestore }
                    if (newItems.isEmpty()) {
                        myAppState.viewerState = null
                    } else {
                        val newIndex = originalIndex.coerceAtMost(newItems.size - 1)
                        myAppState.viewerState = currentViewerState.copy(items = newItems, startIndex = newIndex)
                    }
                } else {
                    myAppState.viewerState = null
                }
                myAppState.itemsToRestore = emptyList()
            },
            onDismiss = { myAppState.showConfirmRestoreDialog = false
                if (isVibrationEnabled) com.example.nkdsify.ui.utils.performVibration(context)
            }
        )
    }
}
