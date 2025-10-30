package com.example.nkdsify.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TagsRepository {
    private const val PREFS_NAME = "media_tags"
    private const val TAGS_KEY = "tags_map"
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

    fun getTagsForItem(context: Context, uri: Uri): Set<String> {
        val allTags = getTags(context)
        return allTags[uri.toString()] ?: emptySet()
    }

    fun setTagsForItem(context: Context, uri: Uri, tags: Set<String>) {
        val allTags = getTags(context).toMutableMap()
        if (tags.isEmpty()) {
            allTags.remove(uri.toString())
        } else {
            allTags[uri.toString()] = tags
        }
        saveTags(context, allTags)
    }

    fun removeTagFromAllItems(context: Context, tag: String) {
        val allTags = getTags(context).toMutableMap()
        val updatedTags = allTags.mapValues {
            it.value.toMutableSet().apply { remove(tag) }
        }.filterValues { it.isNotEmpty() }
        saveTags(context, updatedTags)
    }

    fun renameTag(context: Context, oldTag: String, newTag: String) {
        if (oldTag == newTag) return
        val allTags = getTags(context)
        val updatedTags = allTags.mapValues { (_, tags) ->
            if (tags.contains(oldTag)) {
                tags.toMutableSet().apply {
                    remove(oldTag)
                    add(newTag)
                }
            } else {
                tags
            }
        }
        saveTags(context, updatedTags)
    }
}