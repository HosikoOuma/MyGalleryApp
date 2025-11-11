package com.example.nkdsify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.nkdsify.R
import com.example.nkdsify.ui.utils.SettingsRepository.isVibrationEnabled
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun TagManagementScreen(
    allTags: Set<String>,
    onDeleteTag: (String) -> Unit,
    onEditTag: (oldTag: String, newTag: String) -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf<String?>(null) }

    if (showEditDialog != null) {
        val oldTag = showEditDialog!!
        var newTag by remember { mutableStateOf(oldTag) }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(stringResource(id = R.string.edit_tag_dialog_title)) },
            text = {
                TextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text(stringResource(id = R.string.new_tag_name_label)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onEditTag(oldTag, newTag)
                    showEditDialog = null
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.save_button))
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = null
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (allTags.isEmpty()) {
            Text(stringResource(id = R.string.no_tags_found))
        } else {
            LazyColumn {
                items(allTags.toList()) { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag)
                        Row {
                            IconButton(onClick = { showEditDialog = tag
                                if (isVibrationEnabled(context)) performVibration(context)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit_tag_content_description))
                            }
                            IconButton(onClick = { 
                                onDeleteTag(tag)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_tag_content_description))
                            }
                        }
                    }
                }
            }
        }
    }
}
