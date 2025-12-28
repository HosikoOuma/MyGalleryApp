package com.example.nkdsify.ui.utils

import android.content.Context
import androidx.core.content.edit
import com.example.nkdsify.data.AppFontFamily
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.FabAction
import com.example.nkdsify.data.Language
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
    private const val USE_LARGE_FAB_KEY = "use_large_fab"
    private const val AUTO_DELETE_TRASH_ENABLED_KEY = "auto_delete_trash_enabled"
    private const val AUTO_DELETE_TRASH_DAYS_KEY = "auto_delete_trash_days"
    private const val LANGUAGE_KEY = "language"
    private const val CHECK_FOR_UPDATES_ON_STARTUP_KEY = "check_for_updates_on_startup"
    private const val SPECIAL_LANGUAGE_UNLOCKED_KEY = "special_language_unlocked"
    private const val FAB_ACTION_KEY = "fab_action"
    private const val FIRST_LAUNCH_KEY = "first_launch"
    private const val KEEP_CONTROLS_VISIBLE_KEY = "keep_controls_visible"
    private const val BLURRED_URIS_KEY = "blurred_uris"


    fun setFontFamily(context: Context, fontFamily: AppFontFamily) {context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("font_family", fontFamily.name).apply()
    }

    fun getFontFamily(context: Context): AppFontFamily {
        val fontName = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("font_family", AppFontFamily.SYSTEM.name) ?: AppFontFamily.SYSTEM.name
        return AppFontFamily.valueOf(fontName)
    }

    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(FIRST_LAUNCH_KEY, true)
    }

    fun setFirstLaunchDone(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(FIRST_LAUNCH_KEY, false)
        }
    }

    fun setFabAction(context: Context, fabAction: FabAction) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(FAB_ACTION_KEY, fabAction.name)
        }
    }

    fun getFabAction(context: Context): FabAction {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fabActionName = prefs.getString(FAB_ACTION_KEY, FabAction.SHUFFLE.name) ?: FabAction.SHUFFLE.name
        return FabAction.valueOf(fabActionName)
    }

    fun setSpecialLanguageUnlocked(context: Context, unlocked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(SPECIAL_LANGUAGE_UNLOCKED_KEY, unlocked)
        }
    }

    fun isSpecialLanguageUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SPECIAL_LANGUAGE_UNLOCKED_KEY, false)
    }

    fun setCheckForUpdatesOnStartup(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(CHECK_FOR_UPDATES_ON_STARTUP_KEY, enabled)
        }
    }

    fun isCheckForUpdatesOnStartupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(CHECK_FOR_UPDATES_ON_STARTUP_KEY, true)
    }

    fun setLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString(LANGUAGE_KEY, language.name)
        }
    }

    fun getLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val languageName = prefs.getString(LANGUAGE_KEY, Language.SYSTEM.name) ?: Language.SYSTEM.name
        return Language.valueOf(languageName)
    }

    fun setAutoDeleteTrashEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(AUTO_DELETE_TRASH_ENABLED_KEY, enabled)
        }
    }

    fun isAutoDeleteTrashEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(AUTO_DELETE_TRASH_ENABLED_KEY, false)
    }

    fun setAutoDeleteTrashDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(AUTO_DELETE_TRASH_DAYS_KEY, days)
        }
    }

    fun getAutoDeleteTrashDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(AUTO_DELETE_TRASH_DAYS_KEY, 30)
    }

    fun setUseLargeFab(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(USE_LARGE_FAB_KEY, enabled)
        }
    }

    fun isUseLargeFab(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(USE_LARGE_FAB_KEY, false)
    }

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
        return prefs.getBoolean(BLUR_ENABLED_KEY, false) // Enabled by default
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
        return prefs.getBoolean(TRASH_BLUR_ENABLED_KEY, false) // Enabled by default
    }

    fun setZoomType(context: Context, zoomType: ZoomType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(ZOOM_TYPE_KEY, zoomType.name)
        }
    }

    fun getZoomType(context: Context): ZoomType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val zoomTypeName = prefs.getString(ZOOM_TYPE_KEY, ZoomType.DOUBLE_TAP.name) ?: ZoomType.DOUBLE_TAP.name
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

    fun setKeepControlsVisible(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEEP_CONTROLS_VISIBLE_KEY, enabled)
        }
    }

    fun isKeepControlsVisible(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEEP_CONTROLS_VISIBLE_KEY, false)
    }

    fun setBlurredUris(context: Context, uris: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putStringSet(BLURRED_URIS_KEY, uris)
        }
    }

    fun getBlurredUris(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(BLURRED_URIS_KEY, emptySet()) ?: emptySet()
    }

}
