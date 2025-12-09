package com.example.nkdsify.ui.utils

import android.content.Context
import android.util.Log
import com.example.nkdsify.data.loadAllMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MigrationUtils {

    private const val PREFS_NAME = "migration_prefs"
    private const val KEY_FAVORITES_TAGS_MIGRATED = "favorites_tags_migrated_to_paths"

    private fun isMigrationNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // We need to migrate if the flag is not set OR if the old favorites/tags are still URI-based
        val oldFavorites = FavoritesRepository.getFavorites(context)
        return !prefs.getBoolean(KEY_FAVORITES_TAGS_MIGRATED, false) || oldFavorites.any { it.startsWith("content://") }
    }

    private fun markMigrationAsDone(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FAVORITES_TAGS_MIGRATED, true).apply()
    }

    suspend fun runMigrationIfNeeded(context: Context) {
        if (!isMigrationNeeded(context)) {
            Log.d("MigrationUtils", "Migration not needed.")
            return
        }

        Log.d("MigrationUtils", "Starting migration from URI to absolute paths.")

        withContext(Dispatchers.IO) {
            try {
                // 1. Load all media to create a URI -> Path map
                val allMedia = loadAllMedia(context, com.example.nkdsify.data.SortType.DATE_ADDED, false, emptySet(), null)
                val uriToPathMap = allMedia.associate { it.uri.toString() to it.absolutePath }

                // 2. Migrate Favorites
                val oldFavorites = FavoritesRepository.getFavorites(context)
                if (oldFavorites.any { it.startsWith("content://") }) {
                    val newFavorites = oldFavorites.mapNotNull { uriString -> uriToPathMap[uriString] }.toSet()
                    FavoritesRepository.saveFavorites(context, newFavorites)
                    Log.d("MigrationUtils", "Migrated ${newFavorites.size} favorites.")
                }

                // 3. Migrate Tags
                val oldTags = TagsRepository.getTags(context)
                if (oldTags.keys.any { it.startsWith("content://") }) {
                    val newTags = mutableMapOf<String, Set<String>>()
                    oldTags.forEach { (uriString, tagSet) ->
                        uriToPathMap[uriString]?.let { path ->
                            newTags[path] = tagSet
                        }
                    }
                    TagsRepository.saveTags(context, newTags)
                    Log.d("MigrationUtils", "Migrated tags for ${newTags.size} items.")
                }

                // 4. Mark migration as done
                markMigrationAsDone(context)
                Log.d("MigrationUtils", "Migration completed successfully.")

            } catch (e: Exception) {
                Log.e("MigrationUtils", "Migration failed", e)
                // If migration fails, we don't mark it as done, so it can be retried.
            }
        }
    }
}