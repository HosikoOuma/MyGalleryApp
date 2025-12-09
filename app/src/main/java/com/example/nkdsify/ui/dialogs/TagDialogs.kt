package com.example.nkdsify.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.ui.components.BackupAndRestoreDialog
import com.example.nkdsify.ui.components.TagEditDialog
import com.example.nkdsify.ui.utils.TagsRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.collections.immutable.toImmutableList

// Data class to hold both the ordered list of all tags and the tag-to-file assignments
data class TagsBackup(val allTags: List<String>, val tagsMap: Map<String, Set<String>>)

@Composable
fun TagDialogs(
    myAppState: MyAppState,
    onAddNewTag: (String) -> Unit,
    isVibrationEnabled: Boolean,
    favorites: MutableList<String>
) {
    val context = LocalContext.current
    val importFavoritesLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val json = java.io.BufferedReader(java.io.InputStreamReader(inputStream)).readText()
                    val type = object : TypeToken<Set<String>>() {}.type
                    val importedFavorites: Set<String> = Gson().fromJson(json, type)
                    favorites.clear()
                    favorites.addAll(importedFavorites)
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
                    val type = object : TypeToken<TagsBackup>() {}.type
                    val importedBackup: TagsBackup = Gson().fromJson(json, type)

                    // --- MERGE LOGIC ---

                    // 1. Load existing data
                    val currentTagsMap = TagsRepository.getTags(context).toMutableMap()
                    val currentAllTags = TagsRepository.getAllTags(context).toMutableList()

                    // 2. Merge tagsMap (file-to-tag assignments)
                    importedBackup.tagsMap.forEach { (path, importedTagSet) ->
                        val currentTagSet = currentTagsMap.getOrPut(path) { emptySet() }
                        currentTagsMap[path] = currentTagSet + importedTagSet // Union of the two sets
                    }

                    // 3. Merge allTags (the ordered list of unique tags)
                    val existingTagsSet = currentAllTags.toSet()
                    importedBackup.allTags.forEach { importedTag ->
                        if (!existingTagsSet.contains(importedTag)) {
                            currentAllTags.add(importedTag)
                        }
                    }

                    // 4. Save merged data
                    TagsRepository.saveTags(context, currentTagsMap)
                    TagsRepository.saveAllTags(context, currentAllTags)

                    // 5. Reload state in the app
                    myAppState.tags = currentTagsMap
                    myAppState.allTags = currentAllTags.toImmutableList()
                    myAppState.refreshTrigger++
                    android.widget.Toast.makeText(context, context.getString(R.string.tags_imported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_import_tags), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (myAppState.showTagDialog != null) {
        val path = myAppState.allMedia.find { it.uri == myAppState.showTagDialog }?.absolutePath
        if (path != null) {
            TagEditDialog(
                initialTags = TagsRepository.getTagsForItem(context, path),
                allTags = myAppState.allTags.toList(),
                onDismiss = { myAppState.showTagDialog = null },
                onSave = { tagSet ->
                    TagsRepository.setTagsForItem(context, path, tagSet)
                    myAppState.tags = TagsRepository.getTags(context)
                    myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
                    myAppState.showTagDialog = null
                })
        }
    }

    if (myAppState.showBulkTagDialog) {
        val paths = myAppState.selectedItems.mapNotNull { uri -> myAppState.allMedia.find { it.uri == uri }?.absolutePath }
        val commonTags = if (paths.isNotEmpty()) {
            paths.map { TagsRepository.getTagsForItem(context, it) }.reduce { acc, set -> acc.intersect(set) }
        } else emptySet()

        TagEditDialog(
            initialTags = commonTags,
            allTags = myAppState.allTags.toList(),
            onDismiss = { myAppState.showBulkTagDialog = false },
            onSave = { newTags ->
                val tagsToAdd = newTags - commonTags
                val tagsToRemove = commonTags - newTags
                paths.forEach { path ->
                    val currentTags = TagsRepository.getTagsForItem(context, path).toMutableSet()
                    currentTags.addAll(tagsToAdd)
                    currentTags.removeAll(tagsToRemove)
                    TagsRepository.setTagsForItem(context, path, currentTags)
                }
                myAppState.tags = TagsRepository.getTags(context)
                myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
                myAppState.showBulkTagDialog = false
                myAppState.selectedItems.clear()
            }
        )
    }

    if (myAppState.showBackupAndRestoreDialog) {
        BackupAndRestoreDialog(
            onDismiss = { myAppState.showBackupAndRestoreDialog = false },
            onExportFavorites = {
                val json = Gson().toJson(favorites)
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
                // Create a single backup object with both pieces of data
                val backup = TagsBackup(
                    allTags = myAppState.allTags,
                    tagsMap = TagsRepository.getTags(context)
                )
                val json = Gson().toJson(backup)
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
