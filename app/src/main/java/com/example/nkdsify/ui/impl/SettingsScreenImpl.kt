package com.example.nkdsify.ui.impl

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
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
    onFontFamilyChange: (com.example.nkdsify.data.AppFontFamily) -> Unit,
    onFabActionChange: (com.example.nkdsify.data.FabAction) -> Unit
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
        selectedFabAction = myAppState.selectedFabAction,
        selectedFontFamily = myAppState.selectedFontFamily,
        isKeepControlsVisible = myAppState.isKeepControlsVisible
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
                val mediaPlayer = android.media.MediaPlayer.create(context, com.example.nkdsify.R.raw.uwu)
                mediaPlayer.setOnCompletionListener { it.release() }
                mediaPlayer.start()
            }
        },
        onThemeChange = { theme ->
            myAppState.selectedTheme = theme
            SettingsRepository.setTheme(context, theme)
        },
        onManageHiddenFoldersClick = {
            if (myAppState.isVibrationEnabled) performVibration(context)
            myAppState.showHiddenFoldersDialog = true
        },
        onZoomTypeChange = {
            myAppState.selectedZoomType = it
            SettingsRepository.setZoomType(context, it)
        },
        onManageTagsClick = {
            if (myAppState.isVibrationEnabled) performVibration(context)
            myAppState.currentScreen = Screen.TagManagement
        },
        onBackupAndRestoreClick = {
            if (myAppState.isVibrationEnabled) performVibration(context)
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
        onFabActionChange = onFabActionChange,
        onKeepControlsVisibleChange = {
            myAppState.isKeepControlsVisible = it
            SettingsRepository.setKeepControlsVisible(context, it)
        }
    )

    SettingsScreen(state = settingsState, actions = settingsActions)
}