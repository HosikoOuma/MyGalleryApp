package com.example.nkdsify.ui.utils

import android.content.Context
import androidx.core.content.edit
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType

object SettingsRepository {
    private const val PREFS_NAME = "app_settings"
    private const val BLUR_ENABLED_KEY = "blur_enabled"
    private const val BLUR_TYPE_KEY = "blur_type"
    private const val BLUR_IN_FOLDER_ENABLED_KEY = "blur_in_folder_enabled"
    private const val MUTE_VIDEO_BY_DEFAULT_KEY = "mute_video_by_default"
    private const val THEME_KEY = "theme"
    private const val HIDDEN_FOLDERS_KEY = "hidden_folders"
    private const val TRASH_BLUR_ENABLED_KEY = "trash_blur_enabled"
    private const val ZOOM_TYPE_KEY = "zoom_type"
    private const val BLUR_ALL_MEDIA_ENABLED_KEY = "blur_all_media_enabled"
    private const val VIBRATION_ENABLED_KEY = "vibration_enabled"
    private const val SHOW_FILE_COUNT_KEY = "show_file_count"
    private const val SHUFFLE_BUTTON_VISIBLE_KEY = "shuffle_button_visible"
    private const val SHAKE_TO_BLUR_KEY = "shake_to_blur"
    private const val LOOP_VIDEO_KEY = "loop_video"
    private const val SWIPE_TO_DISMISS_ENABLED_KEY = "swipe_to_dismiss_enabled"

    fun setSwipeToDismissEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(SWIPE_TO_DISMISS_ENABLED_KEY, enabled)
        }
    }

    fun isSwipeToDismissEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SWIPE_TO_DISMISS_ENABLED_KEY, true)
    }

    fun setBlurType(context: Context, blurType: BlurType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(BLUR_TYPE_KEY, blurType.name)
        }
    }

    fun getBlurType(context: Context): BlurType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blurTypeName = prefs.getString(BLUR_TYPE_KEY, BlurType.BLUR.name) ?: BlurType.BLUR.name
        return BlurType.valueOf(blurTypeName)
    }

    fun setLoopVideoEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(LOOP_VIDEO_KEY, enabled)
        }
    }

    fun isLoopVideoEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(LOOP_VIDEO_KEY, true)
    }

    fun setBlurInFolderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(BLUR_IN_FOLDER_ENABLED_KEY, enabled)
        }
    }

    fun isBlurInFolderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(BLUR_IN_FOLDER_ENABLED_KEY, false)
    }

    fun setShakeToBlurEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(SHAKE_TO_BLUR_KEY, enabled)
        }
    }

    fun isShakeToBlurEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SHAKE_TO_BLUR_KEY, false)
    }

    fun setShuffleButtonVisible(context: Context, visible: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(SHUFFLE_BUTTON_VISIBLE_KEY, visible)
        }
    }

    fun isShuffleButtonVisible(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SHUFFLE_BUTTON_VISIBLE_KEY, false)
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

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(VIBRATION_ENABLED_KEY, enabled)
        }
    }

    fun isVibrationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(VIBRATION_ENABLED_KEY, true) // Enabled by default
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
}