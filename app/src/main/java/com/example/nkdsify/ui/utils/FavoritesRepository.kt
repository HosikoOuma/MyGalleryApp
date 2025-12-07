package com.example.nkdsify.ui.utils

import android.content.Context

object FavoritesRepository {
    private const val PREFS_NAME = "MyGalleryAppPrefs"
    private const val FAVORITES_KEY = "favorites"

    fun saveFavorites(context: Context, favorites: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(FAVORITES_KEY, favorites).apply()
    }

    fun getFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
    }
}
