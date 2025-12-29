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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.nkdsify.ui.utils.performVibration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreDialog(
    onDismiss: () -> Unit,
    onExportFavorites: () -> Unit,
    onImportFavorites: () -> Unit,
    onExportTags: () -> Unit,
    onImportTags: () -> Unit,
    isVibrationEnabled: Boolean
) {
    val context = LocalContext.current
    val vibrate = {
        if (isVibrationEnabled) {
            performVibration(context)
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.backup_and_restore_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(id = R.string.favorites_section_title))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            vibrate()
                            onImportFavorites()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text(stringResource(id = R.string.import_button))
                    }
                    Button(
                        onClick = {
                            vibrate()
                            onExportFavorites()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text(stringResource(id = R.string.export_button))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.tags_section_title))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            vibrate()
                            onImportTags()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text(stringResource(id = R.string.import_button))
                    }
                    Button(
                        onClick = {
                            vibrate()
                            onExportTags()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Text(stringResource(id = R.string.export_button))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                vibrate()
                onDismiss()
            }) {
                Text(stringResource(id = R.string.close_button))
            }
        }
    }
}
