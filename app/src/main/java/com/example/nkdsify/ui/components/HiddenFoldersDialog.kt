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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.ui.utils.SettingsRepository.isVibrationEnabled
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun HiddenFoldersDialog(
    allFolders: List<MediaFolder>,
    hiddenFolders: Set<String>,
    onDismiss: () -> Unit,
    onFolderHiddenChange: (String, Boolean) -> Unit

) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.manage_hidden_folders_title)) },
        text = {
            Column {
                if (allFolders.isEmpty()) {
                    Text(stringResource(id = R.string.no_folders_to_hide))
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
                                        if (isVibrationEnabled(context)) performVibration(context)
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
                Text(stringResource(id = R.string.close_button))
            }
        }
    )
}
