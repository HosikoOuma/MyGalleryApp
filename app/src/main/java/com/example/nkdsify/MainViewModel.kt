package com.example.nkdsify

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nkdsify.data.*
import com.example.nkdsify.ui.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel для `MainContent` — содержит основную бизнес-логику и состояние, связанное с данными.
 * Это минимальная, безопасная миграция логики из `MainContent` в отдельный слой.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    // Данные и состояния
    val allFolders = mutableStateOf<List<MediaFolder>>(emptyList())
    val allMedia = mutableStateOf<List<MediaItem>>(emptyList())
    val favoriteItems = mutableStateOf<List<MediaItem>>(emptyList())
    val trashedItems = mutableStateOf<List<MediaItem>>(emptyList())
    val tags = mutableStateOf<Map<String, Set<String>>>(emptyMap())

    // Favorites как изменяемый список (Snapshot state list) — UI может наблюдать и модифицировать
    val favorites = mutableStateListOf<Uri>()

    init {
        // инициализация тегов
        viewModelScope.launch(Dispatchers.IO) {
            tags.value = TagsRepository.getTags(getApplication())
            val favs = FavoritesRepository.getFavorites(getApplication()).map { it.toUri() }
            favorites.addAll(favs)
        }
    }

    fun loadData(sortType: SortType, sortAscending: Boolean, selectedDate: Long?, hiddenFolders: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val folders = loadMediaFolders(getApplication(), sortType, sortAscending, selectedDate)
                val trash = loadTrashedMediaItems(getApplication(), sortType, sortAscending)
                val all = loadAllMedia(getApplication(), sortType, sortAscending, hiddenFolders, selectedDate)
                withContext(Dispatchers.Main) {
                    allFolders.value = folders
                    trashedItems.value = trash
                    allMedia.value = all
                }
            } catch (_: Exception) {
                // swallow - MainContent handles UI feedback
            }
        }
    }

    fun loadFavoriteItems(sortType: SortType, sortAscending: Boolean, selectedDate: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loaded = loadFavoriteMediaItems(getApplication(), favorites.toSet(), sortType, sortAscending, selectedDate)
                withContext(Dispatchers.Main) {
                    favoriteItems.value = loaded
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteExpiredTrashIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            if (SettingsRepository.isAutoDeleteTrashEnabled(getApplication())) {
                val days = SettingsRepository.getAutoDeleteTrashDays(getApplication())
                TrashRepository.deleteExpired(getApplication(), days)
            }
        }
    }

    fun saveFavoritesToRepo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favoriteStrings = favorites.map { it.toString() }.toSet()
                FavoritesRepository.saveFavorites(getApplication(), favoriteStrings)
            } catch (_: Exception) {}
        }
    }

    fun refreshTags() {
        viewModelScope.launch(Dispatchers.IO) {
            tags.value = TagsRepository.getTags(getApplication())
        }
    }

    fun importFavoritesFromUri(uri: Uri?, resolverProvider: () -> android.content.ContentResolver?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = resolverProvider() ?: return@launch
                resolver.openInputStream(uri)?.use { input ->
                    val json = input.bufferedReader().readText()
                    val type = com.google.gson.reflect.TypeToken.getParameterized(Set::class.java, String::class.java).type
                    val imported: Set<String> = com.google.gson.Gson().fromJson(json, type)
                    withContext(Dispatchers.Main) {
                        favorites.clear()
                        favorites.addAll(imported.map { it.toUri() })
                        saveFavoritesToRepo()
                    }
                }
            } catch (_: Exception) {
                // ignore - caller shows toast
            }
        }
    }

    fun importTagsFromUri(uri: Uri?, resolverProvider: () -> android.content.ContentResolver?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = resolverProvider() ?: return@launch
                resolver.openInputStream(uri)?.use { input ->
                    val json = input.bufferedReader().readText()
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Set<String>>>() {}.type
                    val imported: Map<String, Set<String>> = com.google.gson.Gson().fromJson(json, type)
                    TagsRepository.saveTags(getApplication(), imported)
                    withContext(Dispatchers.Main) {
                        tags.value = imported
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Export favorites JSON to Downloads — callback on main: (success, message)
    fun exportFavorites(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = com.google.gson.Gson().toJson(favorites.map { it.toString() })
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "favorites_backup.json")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = getApplication<Application>().contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    withContext(Dispatchers.Main) { onResult(true, null) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "failed_to_create") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "error") }
            }
        }
    }

    fun exportTags(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = com.google.gson.Gson().toJson(tags.value)
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "tags_backup.json")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = getApplication<Application>().contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    withContext(Dispatchers.Main) { onResult(true, null) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "failed_to_create") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "error") }
            }
        }
    }

    fun renameItem(uri: Uri, newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // reuse utility
                com.example.nkdsify.ui.utils.renameMedia(getApplication(), uri, newName)
                withContext(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun removeFromTrash(uris: List<Uri>, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                TrashRepository.removeFromTrash(getApplication(), uris)
                // refresh trashedItems
                val updated = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
                withContext(Dispatchers.Main) {
                    trashedItems.value = updated
                    onResult(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun clearTrash(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                TrashRepository.clearTrash(getApplication())
                val updated = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
                withContext(Dispatchers.Main) {
                    trashedItems.value = updated
                    onResult(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun copyToTrashAndDelete(uris: List<Uri>, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val copiedUris = TrashRepository.copyToTrash(getApplication(), uris)
                if (copiedUris.isNotEmpty()) {
                    deleteMedia(getApplication(), copiedUris)
                }
                // refresh data
                val folders = loadMediaFolders(getApplication(), SortType.DATE_MODIFIED, false, null)
                val all = loadAllMedia(getApplication(), SortType.DATE_MODIFIED, false, SettingsRepository.getHiddenFolders(getApplication()), null)
                val trash = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
                withContext(Dispatchers.Main) {
                    allFolders.value = folders
                    allMedia.value = all
                    trashedItems.value = trash
                    onResult(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun restoreFromTrash(uris: List<Uri>, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                TrashRepository.restoreFromTrash(getApplication(), uris)
                // refresh data
                val folders = loadMediaFolders(getApplication(), SortType.DATE_MODIFIED, false, null)
                val all = loadAllMedia(getApplication(), SortType.DATE_MODIFIED, false, SettingsRepository.getHiddenFolders(getApplication()), null)
                val trash = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
                withContext(Dispatchers.Main) {
                    allFolders.value = folders
                    allMedia.value = all
                    trashedItems.value = trash
                    onResult(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun performFileOperation(uris: List<Uri>, folderPath: String, operation: FileOperation?, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (operation == FileOperation.COPY) {
                    uris.forEach { com.example.nkdsify.ui.utils.copyMediaToFolder(getApplication(), it, folderPath) }
                } else if (operation == FileOperation.MOVE) {
                    uris.forEach { com.example.nkdsify.ui.utils.moveMediaToFolder(getApplication(), it, folderPath) }
                }
                // refresh data
                val folders = loadMediaFolders(getApplication(), SortType.DATE_MODIFIED, false, null)
                val all = loadAllMedia(getApplication(), SortType.DATE_MODIFIED, false, SettingsRepository.getHiddenFolders(getApplication()), null)
                withContext(Dispatchers.Main) {
                    allFolders.value = folders
                    allMedia.value = all
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun removeTagFromAllItems(tag: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            TagsRepository.removeTagFromAllItems(getApplication(), tag)
            withContext(Dispatchers.Main) {
                tags.value = TagsRepository.getTags(getApplication())
                onComplete()
            }
        }
    }

    fun renameTag(oldTag: String, newTag: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            TagsRepository.renameTag(getApplication(), oldTag, newTag)
            withContext(Dispatchers.Main) {
                tags.value = TagsRepository.getTags(getApplication())
                onComplete()
            }
        }
    }
}
