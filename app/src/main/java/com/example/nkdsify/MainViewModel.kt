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

    fun clearTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            TrashRepository.clearTrash(getApplication())
            // refresh
            trashedItems.value = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
        }
    }

    fun restoreFromTrash(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            TrashRepository.restoreFromTrash(getApplication(), uris)
            trashedItems.value = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
        }
    }

    fun copyToTrashAndDelete(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val copied = TrashRepository.copyToTrash(getApplication(), uris)
            // try to delete originals
            var itemsDeleted = false
            copied.forEach { uri ->
                try {
                    if (getApplication< Application>().contentResolver.delete(uri, null, null) > 0) itemsDeleted = true
                } catch (_: Exception) {}
            }
            if (itemsDeleted) trashedItems.value = loadTrashedMediaItems(getApplication(), SortType.DATE_MODIFIED, false)
        }
    }
}
