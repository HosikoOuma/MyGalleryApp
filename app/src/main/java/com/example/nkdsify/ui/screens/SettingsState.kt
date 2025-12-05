package com.example.nkdsify.ui.screens

import com.example.nkdsify.data.AppFontFamily
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.FabAction
import com.example.nkdsify.data.Language
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType

data class SettingsState(
    val isBlurEnabled: Boolean,
    val isBlurInFolderEnabled: Boolean,
    val isTrashBlurEnabled: Boolean,
    val isMuteVideoByDefault: Boolean,
    val isBlurAllMediaEnabled: Boolean,
    val selectedTheme: Theme,
    val selectedZoomType: ZoomType,
    val isVibrationEnabled: Boolean,
    val isShowFileCountEnabled: Boolean,
    val isShuffleButtonVisible: Boolean,
    val isShakeToBlurEnabled: Boolean,
    val isLoopVideoEnabled: Boolean,
    val selectedBlurType: BlurType,
    val isSwipeToDismissEnabled: Boolean,
    val useLargeFab: Boolean,
    val autoDeleteTrashEnabled: Boolean,
    val autoDeleteTrashDays: Int,
    val selectedLanguage: Language,
    val currentVersion: String,
    val selectedFabAction: FabAction,
    val selectedFontFamily: AppFontFamily,
    val isKeepControlsVisible: Boolean
)

data class SettingsActions(
    val onBlurEnabledChange: (Boolean) -> Unit,
    val onBlurInFolderEnabledChange: (Boolean) -> Unit,
    val onTrashBlurEnabledChange: (Boolean) -> Unit,
    val onMuteVideoByDefaultChange: (Boolean) -> Unit,
    val onBlurAllMediaEnabledChange: (Boolean) -> Unit,
    val onEasterEggClick: () -> Unit,
    val onThemeChange: (Theme) -> Unit,
    val onManageHiddenFoldersClick: () -> Unit,
    val onZoomTypeChange: (ZoomType) -> Unit,
    val onManageTagsClick: () -> Unit,
    val onBackupAndRestoreClick: () -> Unit,
    val onGoToSecretStorage: () -> Unit,
    val onVibrationEnabledChange: (Boolean) -> Unit,
    val onShowFileCountChange: (Boolean) -> Unit,
    val onShuffleButtonVisibleChange: (Boolean) -> Unit,
    val onShakeToBlurEnabledChange: (Boolean) -> Unit,
    val onLoopVideoEnabledChange: (Boolean) -> Unit,
    val onBlurTypeChange: (BlurType) -> Unit,
    val onSwipeToDismissEnabledChange: (Boolean) -> Unit,
    val onUseLargeFabChange: (Boolean) -> Unit,
    val onAutoDeleteTrashEnabledChange: (Boolean) -> Unit,
    val onAutoDeleteTrashDaysChange: (Int) -> Unit,
    val onLanguageChange: (Language) -> Unit,
    val onCheckForUpdates: () -> Unit,
    val onFabActionChange: (FabAction) -> Unit,
    val onViewHistoryClick: () -> Unit,
    val onAboutClick: () -> Unit,
    val onFontFamilyChange: (AppFontFamily) -> Unit,
    val onKeepControlsVisibleChange: (Boolean) -> Unit
)
