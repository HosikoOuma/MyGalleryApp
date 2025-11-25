package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
import com.example.nkdsify.ui.utils.SettingsRepository.isVibrationEnabled
import com.example.nkdsify.ui.utils.performVibration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditDialog(
    initialTags: Set<String>,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    var tags by remember { mutableStateOf(initialTags.joinToString(", ")) }
    var expanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // To handle system navigation bar
        ) {
            Text(
                text = stringResource(id = R.string.edit_tags_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(stringResource(id = R.string.edit_tags_dialog_info))
            Spacer(Modifier.height(8.dp))
            TextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.align(Alignment.Start)) {
                TextButton(onClick = {
                    expanded = true
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.existing_tags_button))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    allTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                val currentTags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                                if (currentTags.add(tag)) {
                                    tags = currentTags.joinToString(", ")
                                }
                                expanded = false
                                if (isVibrationEnabled(context)) performVibration(context)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        if (isVibrationEnabled(context)) performVibration(context)
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
                Button(onClick = {
                    val tagSet = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    onSave(tagSet)
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.save_button))
                }
            }
        }
    }
}
