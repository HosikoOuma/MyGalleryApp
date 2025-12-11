package com.example.nkdsify.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.R

@Composable
fun PermissionPermanentlyDeniedDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.permission_permanently_denied_title)) },
        text = { Text(stringResource(id = R.string.permission_permanently_denied_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(id = R.string.go_to_settings_button))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}