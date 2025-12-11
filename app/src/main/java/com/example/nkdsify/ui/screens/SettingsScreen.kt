package com.example.nkdsify.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.nkdsify.R
import com.example.nkdsify.data.Language
import com.example.nkdsify.ui.dialogs.SpecialLanguageDialog
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.getDisplayName
import com.example.nkdsify.ui.utils.performVibration
import androidx.compose.animation.AnimatedVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions
) {
    val context = LocalContext.current
    var showSpecialLanguageDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var specialLanguageUnlocked by remember { mutableStateOf(SettingsRepository.isSpecialLanguageUnlocked(context)) }
    val isBiometricAvailable = remember { BiometricUtils.isBiometricAvailable(context) }

    val vibrate: () -> Unit = {
        if (state.isVibrationEnabled) performVibration(context)
    }

    if (showSpecialLanguageDialog) {
        SpecialLanguageDialog(
            onDismissRequest = { showSpecialLanguageDialog = false },
            onLanguageChange = actions.onLanguageChange,
            onSpecialLanguageUnlocked = { specialLanguageUnlocked = true },
            vibrate = vibrate
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(title = stringResource(id = R.string.main_section_title), icon = Icons.Default.Build)
                SettingsDropdown(
                    title = stringResource(id = R.string.language_label),
                    selectedValue = state.selectedLanguage,
                    items = Language.entries,
                    getItemName = { it.getDisplayName() },
                    onItemSelected = {
                        if (it == Language.SPECIAL && !specialLanguageUnlocked) {
                            showSpecialLanguageDialog = true
                        } else {
                            actions.onLanguageChange(it)
                        }
                    },
                    vibrate = vibrate
                )
                SettingsDropdown(
                    title = stringResource(id = R.string.theme_label),
                    selectedValue = state.selectedTheme,
                    items = com.example.nkdsify.data.Theme.entries,
                    getItemName = { it.getDisplayName() },
                    onItemSelected = actions.onThemeChange,
                    vibrate = vibrate
                )
                SettingsDropdown(
                    title = stringResource(id = R.string.fab_action_title),
                    selectedValue = state.selectedFabAction,
                    items = com.example.nkdsify.data.FabAction.entries,
                    getItemName = { it.getDisplayName() },
                    onItemSelected = actions.onFabActionChange,
                    vibrate = vibrate
                )
                SettingsDropdown(
                    title = stringResource(id = R.string.settings_item_font_family),
                    selectedValue = state.selectedFontFamily,
                    items = com.example.nkdsify.data.AppFontFamily.entries,
                    getItemName = { it.getDisplayName() },
                    onItemSelected = actions.onFontFamilyChange,
                    vibrate = vibrate
                )
                SettingsSwitch(title = stringResource(id = R.string.auto_delete_trash_label), isChecked = state.autoDeleteTrashEnabled, onCheckedChange = actions.onAutoDeleteTrashEnabledChange, vibrate = vibrate)
                AnimatedVisibility(visible = state.autoDeleteTrashEnabled) {
                    SettingsRow(title = { Text(text = stringResource(id = R.string.auto_delete_trash_days_label)) }) {
                        OutlinedTextField(value = state.autoDeleteTrashDays.toString(), onValueChange = { value -> actions.onAutoDeleteTrashDaysChange(value.filter { it.isDigit() }.toIntOrNull() ?: 0) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(80.dp))
                    }
                }
                SettingsSwitch(title = stringResource(id = R.string.show_shuffle_button_label), isChecked = state.isShuffleButtonVisible, onCheckedChange = actions.onShuffleButtonVisibleChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.use_large_shuffle_button_label), isChecked = state.useLargeFab, onCheckedChange = actions.onUseLargeFabChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.vibration_label), isChecked = state.isVibrationEnabled, onCheckedChange = actions.onVibrationEnabledChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.show_file_count_in_folders_label), isChecked = state.isShowFileCountEnabled, onCheckedChange = actions.onShowFileCountChange, vibrate = vibrate)
            }
        }

        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(title = stringResource(id = R.string.privacy_section_title), icon = Icons.Default.PrivacyTip)
                SettingsSwitch(title = stringResource(id = R.string.blur_folder_previews_label), isChecked = state.isBlurEnabled, onCheckedChange = actions.onBlurEnabledChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.blur_in_folders_label), isChecked = state.isBlurInFolderEnabled, onCheckedChange = actions.onBlurInFolderEnabledChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.blur_media_in_trash_label), isChecked = state.isTrashBlurEnabled, onCheckedChange = actions.onTrashBlurEnabledChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.blur_all_media_label), isChecked = state.isBlurAllMediaEnabled, onCheckedChange = actions.onBlurAllMediaEnabledChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.shake_to_blur_label), isChecked = state.isShakeToBlurEnabled, onCheckedChange = actions.onShakeToBlurEnabledChange, vibrate = vibrate)
                SettingsDropdown(
                    title = stringResource(id = R.string.blur_type_label),
                    selectedValue = state.selectedBlurType,
                    items = com.example.nkdsify.data.BlurType.entries,
                    getItemName = { it.getDisplayName() },
                    onItemSelected = actions.onBlurTypeChange,
                    vibrate = vibrate
                )
                SettingsButtonRow(onClick = {
                    vibrate()
                    actions.onManageHiddenFoldersClick()
                }, text = stringResource(id = R.string.manage_hidden_folders_button))
                SettingsButtonRow(
                    onClick = {
                        vibrate()
                        actions.onViewHistoryClick()
                    },
                    text = stringResource(id = R.string.view_history_title)
                )
                if (isBiometricAvailable) {
                    Button(
                        onClick = {
                            vibrate()
                            actions.onGoToSecretStorage()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(stringResource(id = R.string.secret_storage))
                    }
                }
            }
        }

        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(title = stringResource(id = R.string.media_section_title), icon = Icons.Default.Photo)
                SettingsSwitch(title = stringResource(id = R.string.mute_video_by_default_label), isChecked = state.isMuteVideoByDefault, onCheckedChange = actions.onMuteVideoByDefaultChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.loop_video_label), isChecked = state.isLoopVideoEnabled, onCheckedChange = actions.onLoopVideoEnabledChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.keep_controls_visible_label), isChecked = state.isKeepControlsVisible, onCheckedChange = actions.onKeepControlsVisibleChange, vibrate = vibrate)
                SettingsSwitch(title = stringResource(id = R.string.swipe_to_dismiss_label), isChecked = state.isSwipeToDismissEnabled, onCheckedChange = actions.onSwipeToDismissEnabledChange, vibrate = vibrate)
                SettingsDropdown(
                    title = stringResource(id = R.string.zoom_gesture_label),
                    selectedValue = state.selectedZoomType,
                    items = com.example.nkdsify.data.ZoomType.entries,
                    getItemName = { it.getDisplayName() },
                    onItemSelected = actions.onZoomTypeChange,
                    vibrate = vibrate
                )
            }
        }

        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(title = stringResource(id = R.string.tags_string), icon = Icons.AutoMirrored.Filled.Label)
                SettingsButtonRow(onClick = {
                    vibrate()
                    actions.onManageTagsClick()
                }, text = stringResource(id = R.string.manage_tags_button))
                SettingsButtonRow(onClick = {
                    vibrate()
                    actions.onBackupAndRestoreClick()
                }, text = stringResource(id = R.string.backup_and_restore_button))
            }
        }

        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(title = stringResource(id = R.string.about_section_title), icon = Icons.Default.Info)
                SettingsButtonRow(onClick = {
                    vibrate()
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/HosikoOuma/MyGalleryApp".toUri())
                    context.startActivity(intent)
                }, text = stringResource(id = R.string.github_button))
                SettingsButtonRow(onClick = {
                    vibrate()
                    actions.onCheckForUpdates()
                }, text = stringResource(id = R.string.check_for_updates_button))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.version_label, state.currentVersion),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    vibrate()
                    actions.onAboutClick()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(id = R.string.easter_egg_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
