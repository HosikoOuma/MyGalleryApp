package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.nkdsify.data.MediaFolder

@Composable
fun HiddenFoldersDialog(
    allFolders: List<MediaFolder>,
    hiddenFolders: Set<String>,
    onDismiss: () -> Unit,
    onFolderHiddenChange: (String, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Hidden Folders") },
        text = {
            Column {
                if (allFolders.isEmpty()) {
                    Text("No folders to hide.")
                } else {
                    LazyColumn {
                        items(allFolders) { folder ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(folder.name)
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = hiddenFolders.contains(folder.id.toString()),
                                    onCheckedChange = { isChecked ->
                                        onFolderHiddenChange(folder.id.toString(), isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
