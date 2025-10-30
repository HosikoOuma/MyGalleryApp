package com.example.nkdsify.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TrashRepository {
    private const val PREFS_NAME = "trash_prefs"
    private const val TRASH_KEY = "trashed_uris"
    private val gson = Gson()

    fun getTrashedUris(context: Context): Set<Uri> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(TRASH_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<Set<String>>() {}.type
            val uriStrings: Set<String> = gson.fromJson(json, type)
            uriStrings.map { it.toUri() }.toSet()
        } else {
            emptySet()
        }
    }

    fun saveTrashedUris(context: Context, uris: Set<Uri>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStrings = uris.map { it.toString() }.toSet()
        val json = gson.toJson(uriStrings)
        prefs.edit {
            putString(TRASH_KEY, json)
        }
    }

    fun addToTrash(context: Context, uris: List<Uri>) {
        val currentTrashed = getTrashedUris(context)
        val newTrashed = currentTrashed + uris
        saveTrashedUris(context, newTrashed)
    }

    fun removeFromTrash(context: Context, uris: List<Uri>) {
        val currentTrashed = getTrashedUris(context)
        val newTrashed = currentTrashed - uris.toSet()
        saveTrashedUris(context, newTrashed)
    }

    fun clearTrash(context: Context) {
        saveTrashedUris(context, emptySet())
    }
}
