package com.example.nkdsify.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TagsRepository {
    private const val PREFS_NAME = "media_tags"
    private const val TAGS_KEY = "tags_map"
    private const val ALL_TAGS_KEY = "all_tags" // New key for the set of all tags
    private val gson = Gson()

    fun saveTags(context: Context, tags: Map<String, Set<String>>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(tags)
        prefs.edit {
            putString(TAGS_KEY, json)
        }
    }

    fun getTags(context: Context): Map<String, Set<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(TAGS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, Set<String>>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    // New function to get all tags as a single set
    fun getAllTags(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ALL_TAGS_KEY, null)
        val allTags = if (json != null) {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson<Set<String>>(json, type)
        } else {
            emptySet()
        }
        // For migration purposes, let's also include tags from media
        val tagsFromMedia = getTags(context).values.flatten().toSet()
        return allTags + tagsFromMedia
    }

    // New function to save the set of all tags
    private fun saveAllTags(context: Context, allTags: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(allTags)
        prefs.edit {
            putString(ALL_TAGS_KEY, json)
        }
    }

    // New function to add a new tag
    fun addNewTag(context: Context, newTag: String) {
        if (newTag.isBlank()) return
        val allTags = getAllTags(context).toMutableSet()
        allTags.add(newTag)
        saveAllTags(context, allTags)
    }

    fun getTagsForItem(context: Context, uri: Uri): Set<String> {
        val allTags = getTags(context)
        return allTags[uri.toString()] ?: emptySet()
    }

    fun setTagsForItem(context: Context, uri: Uri, tags: Set<String>) {
        val allMediaTags = getTags(context).toMutableMap()
        if (tags.isEmpty()) {
            allMediaTags.remove(uri.toString())
        } else {
            allMediaTags[uri.toString()] = tags
        }
        saveTags(context, allMediaTags)

        // Also update the master list of all tags
        val allTagsSet = getAllTags(context).toMutableSet()
        allTagsSet.addAll(tags)
        saveAllTags(context, allTagsSet)
    }

    fun removeTagFromAllItems(context: Context, tag: String) {
        val allMediaTags = getTags(context).toMutableMap()
        val updatedMediaTags = allMediaTags.mapValues {
            it.value.toMutableSet().apply { remove(tag) }
        }.filterValues { it.isNotEmpty() }
        saveTags(context, updatedMediaTags)

        // Also remove from the master list
        val allTags = getAllTags(context).toMutableSet()
        allTags.remove(tag)
        saveAllTags(context, allTags)
    }

    fun renameTag(context: Context, oldTag: String, newTag: String) {
        if (oldTag == newTag || newTag.isBlank()) return
        val allMediaTags = getTags(context)
        val updatedMediaTags = allMediaTags.mapValues { (_, tags) ->
            if (tags.contains(oldTag)) {
                tags.toMutableSet().apply {
                    remove(oldTag)
                    add(newTag)
                }
            } else {
                tags
            }
        }
        saveTags(context, updatedMediaTags)

        // Also rename in the master list
        val allTags = getAllTags(context).toMutableSet()
        if (allTags.contains(oldTag)) {
            allTags.remove(oldTag)
            allTags.add(newTag)
            saveAllTags(context, allTags)
        }
    }
}