package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.ui.utils.SettingsRepository.isVibrationEnabled
import com.example.nkdsify.ui.utils.performVibration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenFoldersDialog(
    allFolders: List<MediaFolder>,
    hiddenFolders: Set<String>,
    onDismiss: () -> Unit,
    onFolderHiddenChange: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(id = R.string.manage_hidden_folders_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (allFolders.isEmpty()) {
                Text(stringResource(id = R.string.no_folders_to_hide))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(allFolders) { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(folder.name, modifier = Modifier.weight(1f))
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

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(id = R.string.close_button))
                }
            }
        }
    }
}
