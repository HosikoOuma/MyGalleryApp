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
    private const val BLUR_ALL_MEDIA_ENABLED_KEY = "blur_all_media_enabled"
    private const val VIBRATION_STRENGTH_KEY = "vibration_strength"
    private const val SHOW_FILE_COUNT_KEY = "show_file_count"
    private const val ALL_MEDIA_CLICK_COUNT_KEY = "all_media_click_count"
    private const val EASTER_EGG_UNLOCKED_KEY = "easter_egg_unlocked"
    private const val SHUFFLE_BUTTON_VISIBLE_KEY = "shuffle_button_visible"

    fun setShuffleButtonVisible(context: Context, visible: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(SHUFFLE_BUTTON_VISIBLE_KEY, visible)
        }
    }

    fun isShuffleButtonVisible(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SHUFFLE_BUTTON_VISIBLE_KEY, true)
    }

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

    fun setBlurAllMediaEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(BLUR_ALL_MEDIA_ENABLED_KEY, enabled)
        }
    }

    fun isBlurAllMediaEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(BLUR_ALL_MEDIA_ENABLED_KEY, false)
    }

    fun setVibrationStrength(context: Context, strength: VibrationStrength) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(VIBRATION_STRENGTH_KEY, strength.name)
        }
    }

    fun getVibrationStrength(context: Context): VibrationStrength {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val strengthName = prefs.getString(VIBRATION_STRENGTH_KEY, VibrationStrength.MEDIUM.name) ?: VibrationStrength.MEDIUM.name
        return VibrationStrength.valueOf(strengthName)
    }

    fun setShowFileCount(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(SHOW_FILE_COUNT_KEY, enabled)
        }
    }

    fun isShowFileCountEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SHOW_FILE_COUNT_KEY, true)
    }

    fun incrementAllMediaClickCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = getAllMediaClickCount(context)
        prefs.edit {
            putInt(ALL_MEDIA_CLICK_COUNT_KEY, currentCount + 1)
        }
    }

    fun getAllMediaClickCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(ALL_MEDIA_CLICK_COUNT_KEY, 0)
    }

    fun resetAllMediaClickCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(ALL_MEDIA_CLICK_COUNT_KEY, 0).commit()
    }

    fun setEasterEggUnlocked(context: Context, unlocked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(EASTER_EGG_UNLOCKED_KEY, unlocked).commit()
    }

    fun isEasterEggUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(EASTER_EGG_UNLOCKED_KEY, false)
    }
}
