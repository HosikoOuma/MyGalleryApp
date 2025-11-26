package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.window.DialogProperties
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
    var expanded by remember { mutableStateOf(false) }

    // This is now the single source of truth for the selected tags
    val selectedTags = remember(tagsText) {
        tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Text(text = stringResource(id = R.string.edit_tags_dialog_title))
        },
        text = {
            Column {
                Text(stringResource(id = R.string.edit_tags_dialog_info))
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = tagsText,
                    onValueChange = { tagsText = it }, // Allow free text editing
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
                            val isSelected = tag in selectedTags
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null // Click handled by DropdownMenuItem
                                        )
                                        Text(
                                            text = tag,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                },
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
                                    // Keep the menu open by not setting expanded = false
                                }
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
