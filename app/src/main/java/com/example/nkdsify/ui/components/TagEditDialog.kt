package com.example.nkdsify.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@Composable
fun TagEditDialog(
    initialTags: Set<String>,
    allTags: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    var tags by remember { mutableStateOf(initialTags.joinToString(", ")) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.edit_tags_dialog_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.edit_tags_dialog_info))
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(16.dp))
                Box {
                    TextButton(onClick = { expanded = true
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
                                    tags = if (tags.isEmpty()) tag else "$tags, $tag"
                                    expanded = false
                                    if (isVibrationEnabled(context)) performVibration(context)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val tagSet = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                onSave(tagSet)
                onDismiss()
                if (isVibrationEnabled(context)) performVibration(context)
            }) {
                Text(stringResource(id = R.string.save_button))
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
                if (isVibrationEnabled(context)) performVibration(context)
            }) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}
