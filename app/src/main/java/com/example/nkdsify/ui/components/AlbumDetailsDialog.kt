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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
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
        title = { Text(stringResource(id = R.string.album_details_title)) },
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
                        Text(stringResource(id = R.string.album_details_path, details.path))
                    }
                    Text(stringResource(id = R.string.album_details_total_size, formatFileSize(details.totalSize)))
                    if (details.dateRange != null) {
                        Text(stringResource(id = R.string.album_details_date_range, formatDateRange(details.dateRange.first, details.dateRange.second)))
                    }
                    Text(stringResource(id = R.string.album_details_items, details.itemCount.toString()))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_ok))
            }
        }
    )
}
