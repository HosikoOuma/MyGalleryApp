package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        title = {
            Text(
                text = "Backup and Restore",
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Favorites")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onImportFavorites(); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text("Import")
                    }
                    Button(
                        onClick = { onExportFavorites(); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text("Export")
                    }
                }
                Text("Tags")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onImportTags(); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text("Import")
                    }
                    Button(
                        onClick = { onExportTags(); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text("Export")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
