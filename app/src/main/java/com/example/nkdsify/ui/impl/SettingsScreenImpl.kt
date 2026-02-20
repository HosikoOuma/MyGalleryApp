package com.example.nkdsify.ui.impl

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.Language
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.screens.SettingsActions
import com.example.nkdsify.ui.screens.SettingsScreen
import com.example.nkdsify.ui.screens.SettingsState
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreenImpl(
    myAppState: MyAppState,
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    onFontFamilyChange: (com.example.nkdsify.data.AppFontFamily) -> Unit
) {
    val settingsState = SettingsState(
        isBlurEnabled = myAppState.isBlurEnabled,
        isBlurInFolderEnabled = myAppState.isBlurInFolderEnabled,
        isTrashBlurEnabled = myAppState.isTrashBlurEnabled,
        isMuteVideoByDefault = myAppState.isMuteVideoByDefault,
        isBlurAllMediaEnabled = myAppState.isBlurAllMediaEnabled,
        selectedTheme = myAppState.selectedTheme,
        selectedZoomType = myAppState.selectedZoomType,
        isVibrationEnabled = myAppState.isVibrationEnabled,
        isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
        isShuffleButtonVisible = myAppState.isShuffleButtonVisible,
        isShakeToBlurEnabled = myAppState.isShakeToBlurEnabled,
        isLoopVideoEnabled = myAppState.isLoopVideoEnabled,
        selectedBlurType = myAppState.selectedBlurType,
        isSwipeToDismissEnabled = myAppState.isSwipeToDismissEnabled,
        useLargeFab = myAppState.useLargeFab,
        autoDeleteTrashEnabled = myAppState.autoDeleteTrashEnabled,
        autoDeleteTrashDays = myAppState.autoDeleteTrashDays,
        selectedLanguage = myAppState.selectedLanguage,
        currentVersion = myAppState.currentVersion,
        selectedFontFamily = myAppState.selectedFontFamily,
        isKeepControlsVisible = myAppState.isKeepControlsVisible,
        viewerControlsPosition = myAppState.viewerControlsPosition,
        isVideoPreviewSlideshowEnabled = myAppState.isVideoPreviewSlideshowEnabled,
        videoSlideshowIntervalMs = myAppState.videoSlideshowIntervalMs,
        useLowQualityVideoPreview = myAppState.useLowQualityVideoPreview
    )

    val settingsActions = SettingsActions(
        onBlurEnabledChange = {
            myAppState.isBlurEnabled = it
            SettingsRepository.setBlurEnabled(context, it)
        },
        onBlurInFolderEnabledChange = {
            myAppState.isBlurInFolderEnabled = it
            SettingsRepository.setBlurInFolderEnabled(context, it)
        },
        onTrashBlurEnabledChange = {
            myAppState.isTrashBlurEnabled = it
            SettingsRepository.setTrashBlurEnabled(context, it)
        },
        onMuteVideoByDefaultChange = {
            myAppState.isMuteVideoByDefault = it
            SettingsRepository.setMuteVideoByDefault(context, it)
        },
        onBlurAllMediaEnabledChange = {
            myAppState.isBlurAllMediaEnabled = it
            SettingsRepository.setBlurAllMediaEnabled(context, it)
        },
        onEasterEggClick = {
            if (myAppState.isVibrationEnabled) performVibration(context)
            myAppState.easterEggTapCount++
            if (myAppState.easterEggTapCount == 10) {
                myAppState.easterEggTapCount = 0
                myAppState.showEasterEggDialog = true
                val mediaPlayer = android.media.MediaPlayer.create(context, R.raw.uwu)
                mediaPlayer.setOnCompletionListener { it.release() }
                mediaPlayer.start()
            }
        },
        onThemeChange = { theme ->
            myAppState.selectedTheme = theme
            SettingsRepository.setTheme(context, theme)
        },
        onManageHiddenFoldersClick = {
            myAppState.currentScreen = Screen.HiddenFolders
        },
        onZoomTypeChange = {
            myAppState.selectedZoomType = it
            SettingsRepository.setZoomType(context, it)
        },
        onManageTagsClick = {
            myAppState.currentScreen = Screen.TagManagement
        },
        onBackupAndRestoreClick = {
            myAppState.showBackupAndRestoreDialog = true
        },
        onGoToSecretStorage = {
            BiometricUtils.authenticate(
                activity = context as AppCompatActivity,
                onSuccess = { myAppState.currentScreen = Screen.SecretStorage },
                onError = { _, _ -> /* Do nothing */ },
                onFailed = { /* Do nothing */ }
            )
        },
        onViewHistoryClick = { myAppState.currentScreen = Screen.ViewHistory },
        onVibrationEnabledChange = {
            myAppState.isVibrationEnabled = it
            SettingsRepository.setVibrationEnabled(context, it)
        },
        onShowFileCountChange = {
            myAppState.isShowFileCountEnabled = it
            SettingsRepository.setShowFileCount(context, it)
        },
        onShuffleButtonVisibleChange = {
            myAppState.isShuffleButtonVisible = it
            SettingsRepository.setShuffleButtonVisible(context, it)
        },
        onShakeToBlurEnabledChange = {
            myAppState.isShakeToBlurEnabled = it
            SettingsRepository.setShakeToBlurEnabled(context, it)
        },
        onLoopVideoEnabledChange = {
            myAppState.isLoopVideoEnabled = it
            SettingsRepository.setLoopVideoEnabled(context, it)
        },
        onBlurTypeChange = {
            myAppState.selectedBlurType = it
            SettingsRepository.setBlurType(context, it)
        },
        onSwipeToDismissEnabledChange = {
            myAppState.isSwipeToDismissEnabled = it
            SettingsRepository.setSwipeToDismissEnabled(context, it)
        },
        onUseLargeFabChange = {
            myAppState.useLargeFab = it
            SettingsRepository.setUseLargeFab(context, it)
        },
        onAutoDeleteTrashEnabledChange = {
            myAppState.autoDeleteTrashEnabled = it
            SettingsRepository.setAutoDeleteTrashEnabled(context, it)
        },
        onAutoDeleteTrashDaysChange = {
            myAppState.autoDeleteTrashDays = it
            SettingsRepository.setAutoDeleteTrashDays(context, it)
        },
        onLanguageChange = { language -> myAppState.selectedLanguage = language },
        onCheckForUpdates = {
            coroutineScope.launch {
                myAppState.checkForUpdates(true)
            }
        },
        onAboutClick = { myAppState.currentScreen = Screen.About },
        onFontFamilyChange = onFontFamilyChange,
        onKeepControlsVisibleChange = {
            myAppState.isKeepControlsVisible = it
            SettingsRepository.setKeepControlsVisible(context, it)
        },
        onHelpClick = {
            if (myAppState.selectedLanguage == Language.SPECIAL) {
                myAppState.showHelpAttentionDialog = true
            } else {
                myAppState.currentScreen = Screen.Help
            }
        },
        onViewerControlsPositionChange = {
            myAppState.viewerControlsPosition = it
            SettingsRepository.setViewerControlsPosition(context, it)
        },
        onVideoPreviewSlideshowChange = {
            myAppState.isVideoPreviewSlideshowEnabled = it
            SettingsRepository.setVideoPreviewSlideshow(context, it)
        },
        onVideoSlideshowIntervalChange = {
            myAppState.videoSlideshowIntervalMs = it
            SettingsRepository.setVideoSlideshowInterval(context, it)
        },
        onUseLowQualityVideoPreviewChange = {
            myAppState.useLowQualityVideoPreview = it
            SettingsRepository.setUseLowQualityVideoPreview(context, it)
        }
    )

    SettingsScreen(state = settingsState, actions = settingsActions)
}
