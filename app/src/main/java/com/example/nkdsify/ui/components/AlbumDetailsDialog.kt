package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nkdsify.data.AlbumDetails
import com.example.nkdsify.ui.utils.formatDateRange
import com.example.nkdsify.ui.utils.formatFileSize

@Composable
fun AlbumDetailsDialog(
    details: AlbumDetails?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Album Details") },
        text = {
            if (details == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (details.path != null) {
                        Text("Path: ${details.path}")
                    }
                    Text("Total Size: ${formatFileSize(details.totalSize)}")
                    if (details.dateRange != null) {
                        Text("Date Range: ${formatDateRange(details.dateRange.first, details.dateRange.second)}")
                    }
                    Text("Items: ${details.itemCount}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
