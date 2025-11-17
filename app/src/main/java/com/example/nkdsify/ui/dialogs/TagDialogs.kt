package com.example.nkdsify.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.ui.components.BackupAndRestoreDialog
import com.example.nkdsify.ui.components.TagEditDialog
import com.example.nkdsify.ui.utils.TagsRepository
import com.google.gson.Gson

@Composable
fun TagDialogs(
    myAppState: MyAppState,
    onAddNewTag: (String) -> Unit,
    isVibrationEnabled: Boolean,
    favorites: MutableList<Uri>
) {
    val context = LocalContext.current
    val importFavoritesLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val json = java.io.BufferedReader(java.io.InputStreamReader(inputStream)).readText()
                    val type = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
                    val importedFavorites: Set<String> = Gson().fromJson(json, type)
                    favorites.clear()
                    favorites.addAll(importedFavorites.map { uriString -> uriString.toUri() })
                    myAppState.refreshTrigger++
                    android.widget.Toast.makeText(context, context.getString(R.string.favorites_imported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_import_favorites), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importTagsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val json = java.io.BufferedReader(java.io.InputStreamReader(inputStream)).readText()
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Set<String>>>() {}.type
                    val importedTags: Map<String, Set<String>> = Gson().fromJson(json, type)
                    myAppState.tags = importedTags
                    TagsRepository.saveTags(context, myAppState.tags)
                    myAppState.refreshTrigger++
                    android.widget.Toast.makeText(context, context.getString(R.string.tags_imported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_import_tags), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (myAppState.showTagDialog != null) {
        val uri = myAppState.showTagDialog!!
        TagEditDialog(
            initialTags = TagsRepository.getTagsForItem(context, uri),
            allTags = myAppState.allTags.toList(),
            onDismiss = { myAppState.showTagDialog = null },
            onSave = { tagSet ->
                TagsRepository.setTagsForItem(context, uri, tagSet)
                myAppState.tags = TagsRepository.getTags(context)
                myAppState.allTags = TagsRepository.getAllTags(context) .toList()
                myAppState.showTagDialog = null
            })
    }

    if (myAppState.showBulkTagDialog) {
        val uris = myAppState.selectedItems.toList()
        val commonTags = if (uris.isNotEmpty()) {
            uris.map { TagsRepository.getTagsForItem(context, it) }.reduce { acc, set -> acc.intersect(set) }
        } else emptySet()

        TagEditDialog(
            initialTags = commonTags,
            allTags = myAppState.allTags.toList(),
            onDismiss = { myAppState.showBulkTagDialog = false },
            onSave = { newTags ->
                val tagsToAdd = newTags - commonTags
                val tagsToRemove = commonTags - newTags
                uris.forEach { uri ->
                    val currentTags = TagsRepository.getTagsForItem(context, uri).toMutableSet()
                    currentTags.addAll(tagsToAdd)
                    currentTags.removeAll(tagsToRemove)
                    TagsRepository.setTagsForItem(context, uri, currentTags)
                }
                myAppState.tags = TagsRepository.getTags(context)
                myAppState.allTags = TagsRepository.getAllTags(context).toList()
                myAppState.showBulkTagDialog = false
                myAppState.selectedItems.clear()
            }
        )
    }

    if (myAppState.showBackupAndRestoreDialog) {
        BackupAndRestoreDialog(
            onDismiss = { myAppState.showBackupAndRestoreDialog = false },
            onExportFavorites = {
                val json = Gson().toJson(favorites.map { it.toString() })
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "favorites_backup.json")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(json.toByteArray())
                        }
                        android.widget.Toast.makeText(context, context.getString(R.string.favorites_exported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, context.getString(R.string.failed_to_export_favorites), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onImportFavorites = { importFavoritesLauncher.launch("application/json") },
            onExportTags = {
                val json = Gson().toJson(myAppState.tags)
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "tags_backup.json")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(json.toByteArray())
                        }
                        android.widget.Toast.makeText(context, context.getString(R.string.tags_exported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, context.getString(R.string.failed_to_export_tags), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onImportTags = { importTagsLauncher.launch("application/json") },
            isVibrationEnabled = isVibrationEnabled,
        )
    }
}
