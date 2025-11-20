package com.example.nkdsify.ui.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ViewedItem(val uri: String, val timestamp: Long)

object ViewHistoryRepository {

    private const val PREFS_NAME = "ViewHistoryPrefs"
    private const val HISTORY_KEY = "view_history"
    private const val MAX_HISTORY_SIZE = 200 // To prevent the history from growing indefinitely

    private val gson = Gson()

    fun addToHistory(context: Context, uri: Uri) {
        val history = getHistory(context).toMutableList()
        val uriString = uri.toString()

        // Remove any existing entry for this URI to update its timestamp
        history.removeAll { it.uri == uriString }

        // Add the new item to the top of the list
        history.add(0, ViewedItem(uriString, System.currentTimeMillis()))

        // Trim the list if it exceeds the max size
        val trimmedHistory = if (history.size > MAX_HISTORY_SIZE) {
            history.subList(0, MAX_HISTORY_SIZE)
        } else {
            history
        }

        // Save the updated list
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(trimmedHistory)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }

    fun getHistory(context: Context): List<ViewedItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(HISTORY_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<ViewedItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(HISTORY_KEY).apply()
    }
}
