package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var tagsText by remember { mutableStateOf(initialTags.joinToString(", ")) }

    // This is now the single source of truth for the selected tags
    val selectedTags = remember(tagsText) {
        tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.edit_tags_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp) // Ограничиваем максимальную высоту, чтобы окно было компактнее
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.edit_tags_dialog_info))
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = tagsText,
                    onValueChange = { tagsText = it }, // Allow free text editing
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.existing_tags_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allTags.forEach { tag ->
                            val isSelected = tag in selectedTags
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newTags = selectedTags.toMutableSet()
                                    if (isSelected) {
                                        newTags.remove(tag)
                                    } else {
                                        newTags.add(tag)
                                    }
                                    // Reconstruct the string from the set to update the text field
                                    tagsText = newTags.joinToString(", ")
                                    if (isVibrationEnabled(context)) performVibration(context)
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(selectedTags) // selectedTags is derived from tagsText
                if (isVibrationEnabled(context)) performVibration(context)
            }) {
                Text(stringResource(id = R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    if (isVibrationEnabled(context)) performVibration(context)
                }
            ) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}
