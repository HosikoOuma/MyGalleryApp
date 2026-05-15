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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val importFavoritesLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    myAppState.isProcessing = true
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val json = java.io.BufferedReader(java.io.InputStreamReader(inputStream)).readText()
                        val type = object : TypeToken<Set<String>>() {}.type
                        val importedFavorites: Set<String> = Gson().fromJson(json, type)
                        withContext(Dispatchers.Main) {
                            favorites.clear()
                            favorites.addAll(importedFavorites)
                            myAppState.refreshMedia()
                            android.widget.Toast.makeText(context, context.getString(R.string.favorites_imported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, context.getString(R.string.failed_to_import_favorites), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(NonCancellable + Dispatchers.Main) {
                        myAppState.isProcessing = false
                    }
                }
            }
        }
    }

    val importTagsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    myAppState.isProcessing = true
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
                        withContext(Dispatchers.Main) {
                            myAppState.tags = currentTagsMap
                            myAppState.allTags = currentAllTags.toImmutableList()
                            myAppState.refreshMedia()
                            android.widget.Toast.makeText(context, context.getString(R.string.tags_imported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, context.getString(R.string.failed_to_import_tags), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(NonCancellable + Dispatchers.Main) {
                        myAppState.isProcessing = false
                    }
                }
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
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            myAppState.isProcessing = true
                            TagsRepository.setTagsForItem(context, path, tagSet)
                            val updatedTags = TagsRepository.getTags(context)
                            val updatedAllTags = TagsRepository.getAllTags(context).toImmutableList()
                            withContext(Dispatchers.Main) {
                                myAppState.tags = updatedTags
                                myAppState.allTags = updatedAllTags
                                myAppState.showTagDialog = null
                            }
                        } finally {
                            withContext(NonCancellable + Dispatchers.Main) {
                                myAppState.isProcessing = false
                            }
                        }
                    }
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
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        myAppState.isProcessing = true
                        val tagsToAdd = newTags - commonTags
                        val tagsToRemove = commonTags - newTags
                        paths.forEach { path ->
                            val currentTags = TagsRepository.getTagsForItem(context, path).toMutableSet()
                            currentTags.addAll(tagsToAdd)
                            currentTags.removeAll(tagsToRemove)
                            TagsRepository.setTagsForItem(context, path, currentTags)
                        }
                        val updatedTags = TagsRepository.getTags(context)
                        val updatedAllTags = TagsRepository.getAllTags(context).toImmutableList()
                        withContext(Dispatchers.Main) {
                            myAppState.tags = updatedTags
                            myAppState.allTags = updatedAllTags
                            myAppState.showBulkTagDialog = false
                            myAppState.selectedItems.clear()
                        }
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main) {
                            myAppState.isProcessing = false
                        }
                    }
                }
            }
        )
    }

    if (myAppState.showBackupAndRestoreDialog) {
        BackupAndRestoreDialog(
            onDismiss = { myAppState.showBackupAndRestoreDialog = false },
            onExportFavorites = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        myAppState.isProcessing = true
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
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.favorites_exported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.failed_to_export_favorites), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main) {
                            myAppState.isProcessing = false
                        }
                    }
                }
            },
            onImportFavorites = { importFavoritesLauncher.launch("application/json") },
            onExportTags = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        myAppState.isProcessing = true
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
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.tags_exported_successfully), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.failed_to_export_tags), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_create_backup_file), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main) {
                            myAppState.isProcessing = false
                        }
                    }
                }
            },
            onImportTags = { importTagsLauncher.launch("application/json") },
            isVibrationEnabled = isVibrationEnabled,
        )
    }
}
