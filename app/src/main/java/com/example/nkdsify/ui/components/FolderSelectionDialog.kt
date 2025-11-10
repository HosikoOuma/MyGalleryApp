package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaFolder

@Composable
fun FolderSelectionDialog(
    folders: List<MediaFolder>,
    onDismiss: () -> Unit,
    onFolderSelected: (MediaFolder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_a_folder_title)) },
        text = {
            LazyColumn {
                items(folders) { folder ->
                    TextButton(
                        onClick = { onFolderSelected(folder) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = folder.name,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}
