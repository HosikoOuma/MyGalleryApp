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
import androidx.compose.ui.unit.dp

@Composable
fun TagManagementScreen(
    allTags: Set<String>,
    onDeleteTag: (String) -> Unit,
    onEditTag: (oldTag: String, newTag: String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<String?>(null) }

    if (showEditDialog != null) {
        val oldTag = showEditDialog!!
        var newTag by remember { mutableStateOf(oldTag) }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Tag") },
            text = {
                TextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("New tag name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onEditTag(oldTag, newTag)
                    showEditDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (allTags.isEmpty()) {
            Text("No tags found.")
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
                            IconButton(onClick = { showEditDialog = tag }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Tag")
                            }
                            IconButton(onClick = { onDeleteTag(tag) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Tag")
                            }
                        }
                    }
                }
            }
        }
    }
}
