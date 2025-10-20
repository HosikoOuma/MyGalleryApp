package com.example.nkdsify.ui.utils

import android.content.Context
import com.example.nkdsify.data.Theme

object SettingsRepository {
    private const val PREFS_NAME = "app_settings"
    private const val BLUR_ENABLED_KEY = "blur_enabled"
    private const val MUTE_VIDEO_BY_DEFAULT_KEY = "mute_video_by_default"
    private const val THEME_KEY = "theme"
    private const val HIDDEN_FOLDERS_KEY = "hidden_folders"

    fun setBlurEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(BLUR_ENABLED_KEY, enabled).apply()
    }

    fun isBlurEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(BLUR_ENABLED_KEY, true) // Enabled by default
    }

    fun setMuteVideoByDefault(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(MUTE_VIDEO_BY_DEFAULT_KEY, enabled).apply()
    }

    fun isMuteVideoByDefault(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(MUTE_VIDEO_BY_DEFAULT_KEY, false)
    }

    fun setTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(THEME_KEY, theme.name).apply()
    }

    fun getTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(THEME_KEY, Theme.SYSTEM.name) ?: Theme.SYSTEM.name
        return Theme.valueOf(themeName)
    }

    fun setHiddenFolders(context: Context, hiddenFolders: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(HIDDEN_FOLDERS_KEY, hiddenFolders).apply()
    }

    fun getHiddenFolders(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(HIDDEN_FOLDERS_KEY, emptySet()) ?: emptySet()
    }
}
