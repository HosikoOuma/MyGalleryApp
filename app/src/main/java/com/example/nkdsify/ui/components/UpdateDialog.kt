package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDoNotShowAgain: () -> Unit,
    onDownload: () -> Unit,
    latestVersion: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.update_available_title)) },
        text = { Text(stringResource(id = R.string.update_available_text, latestVersion)) },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.download_button))
                }
                TextButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.update_button))
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onDoNotShowAgain) {
                        Text(stringResource(id = R.string.dont_show_again_button))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.dialog_cancel))
                    }
                }
            }
        },
        dismissButton = null
    )
}
