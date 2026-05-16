package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
import com.example.nkdsify.data.AlbumDetails
import com.example.nkdsify.ui.utils.performVibration
import com.example.nkdsify.ui.utils.SettingsRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsDialog(details: AlbumDetails, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.album_details_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!details.path.isNullOrEmpty()) {
                DetailItem(stringResource(id = R.string.details_path_label), details.path)
            }
            DetailItem(stringResource(id = R.string.details_total_size), android.text.format.Formatter.formatShortFileSize(context, details.totalSize))
            if (details.dateRange != null) {
                val dateRangeString = formatDateRangeToString(details.dateRange)
                if (dateRangeString.isNotEmpty()) {
                    DetailItem(stringResource(id = R.string.details_date_range), dateRangeString)
                }
            }
            DetailItem(stringResource(id = R.string.details_item_count), details.itemCount.toString())

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    if (SettingsRepository.isVibrationEnabled(context)) performVibration(context)
                    onDismiss()
                }) {
                    Text(stringResource(id = R.string.dialog_close))
                }
            }
        }
    }
}

private fun formatDateRangeToString(dateRange: Pair<Long, Long>): String {
    val (startMillis, endMillis) = dateRange
    if (startMillis == 0L || endMillis == 0L) return ""
    val startDate = Instant.ofEpochSecond(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val endDate = Instant.ofEpochSecond(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return if (startDate == endDate) {
        formatter.format(startDate)
    } else {
        "${formatter.format(startDate)} - ${formatter.format(endDate)}"
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
