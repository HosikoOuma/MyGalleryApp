package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun BackupAndRestoreDialog(
    onDismiss: () -> Unit,
    onExportFavorites: () -> Unit,
    onImportFavorites: () -> Unit,
    onExportTags: () -> Unit,
    onImportTags: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup and Restore") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    onExportFavorites()
                    onDismiss()
                }) {
                    Text("Export Favorites")
                }
                Button(onClick = {
                    onImportFavorites()
                    onDismiss()
                }) {
                    Text("Import Favorites")
                }
                Button(onClick = {
                    onExportTags()
                    onDismiss()
                }) {
                    Text("Export Tags")
                }
                Button(onClick = {
                    onImportTags()
                    onDismiss()
                }) {
                    Text("Import Tags")
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
