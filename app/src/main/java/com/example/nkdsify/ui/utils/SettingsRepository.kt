package com.example.nkdsify.ui.utils

import android.content.Context
import androidx.core.content.edit
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType

object SettingsRepository {
    private const val PREFS_NAME = "app_settings"
    private const val BLUR_ENABLED_KEY = "blur_enabled"
    private const val MUTE_VIDEO_BY_DEFAULT_KEY = "mute_video_by_default"
    private const val THEME_KEY = "theme"
    private const val HIDDEN_FOLDERS_KEY = "hidden_folders"
    private const val TRASH_BLUR_ENABLED_KEY = "trash_blur_enabled"
    private const val ZOOM_TYPE_KEY = "zoom_type"

    fun setBlurEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(BLUR_ENABLED_KEY, enabled)
        }
    }

    fun isBlurEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(BLUR_ENABLED_KEY, true) // Enabled by default
    }

    fun setMuteVideoByDefault(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(MUTE_VIDEO_BY_DEFAULT_KEY, enabled)
        }
    }

    fun isMuteVideoByDefault(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(MUTE_VIDEO_BY_DEFAULT_KEY, false)
    }

    fun setTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(THEME_KEY, theme.name)
        }
    }

    fun getTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(THEME_KEY, Theme.SYSTEM.name) ?: Theme.SYSTEM.name
        return Theme.valueOf(themeName)
    }

    fun setHiddenFolders(context: Context, hiddenFolders: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putStringSet(HIDDEN_FOLDERS_KEY, hiddenFolders)
        }
    }

    fun getHiddenFolders(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(HIDDEN_FOLDERS_KEY, emptySet()) ?: emptySet()
    }

    fun setTrashBlurEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(TRASH_BLUR_ENABLED_KEY, enabled)
        }
    }

    fun isTrashBlurEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(TRASH_BLUR_ENABLED_KEY, true) // Enabled by default
    }

    fun setZoomType(context: Context, zoomType: ZoomType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(ZOOM_TYPE_KEY, zoomType.name)
        }
    }

    fun getZoomType(context: Context): ZoomType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val zoomTypeName = prefs.getString(ZOOM_TYPE_KEY, ZoomType.PINCH.name) ?: ZoomType.PINCH.name
        return ZoomType.valueOf(zoomTypeName)
    }
}
