package com.example.nkdsify.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TagsRepository {
    private const val PREFS_NAME = "media_tags"
    private const val TAGS_KEY = "tags_map"
    private const val ALL_TAGS_KEY = "all_tags_list" // Changed to list
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

    fun getAllTags(context: Context): List<String> { // Return List
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ALL_TAGS_KEY, null)
        val allTags = if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type // Use List
            gson.fromJson<List<String>>(json, type)
        } else {
            emptyList()
        }
        val tagsFromMedia = getTags(context).values.flatten().toSet()
        return (allTags + tagsFromMedia).distinct() // Keep order but ensure uniqueness
    }

    fun saveAllTags(context: Context, allTags: List<String>) { // Save List
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(allTags)
        prefs.edit {
            putString(ALL_TAGS_KEY, json)
        }
    }

    fun addNewTag(context: Context, newTag: String) {
        if (newTag.isBlank()) return
        val allTags = getAllTags(context).toMutableList()
        if (!allTags.contains(newTag)) {
            allTags.add(newTag)
            saveAllTags(context, allTags)
        }
    }

    fun getTagsForItem(context: Context, path: String): Set<String> {
        val allTags = getTags(context)
        return allTags[path] ?: emptySet()
    }

    fun setTagsForItem(context: Context, path: String, tags: Set<String>) {
        val allMediaTags = getTags(context).toMutableMap()
        if (tags.isEmpty()) {
            allMediaTags.remove(path)
        } else {
            allMediaTags[path] = tags
        }
        saveTags(context, allMediaTags)

        val allTagsList = getAllTags(context).toMutableList()
        tags.forEach { tag ->
            if (!allTagsList.contains(tag)) {
                allTagsList.add(tag)
            }
        }
        saveAllTags(context, allTagsList)
    }

    fun removeTagFromAllItems(context: Context, tag: String) {
        val allMediaTags = getTags(context).toMutableMap()
        val updatedMediaTags = allMediaTags.mapValues {
            it.value.toMutableSet().apply { remove(tag) }
        }.filterValues { it.isNotEmpty() }
        saveTags(context, updatedMediaTags)

        val allTags = getAllTags(context).toMutableList()
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

        val allTags = getAllTags(context).toMutableList()
        val index = allTags.indexOf(oldTag)
        if (index != -1) {
            allTags[index] = newTag
            saveAllTags(context, allTags)
        }
    }
}
