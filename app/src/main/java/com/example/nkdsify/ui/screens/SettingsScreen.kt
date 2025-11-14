package com.example.nkdsify.ui.screens
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.FabAction
import com.example.nkdsify.data.Language
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.performVibration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isBlurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
    isBlurInFolderEnabled: Boolean,
    onBlurInFolderEnabledChange: (Boolean) -> Unit,
    isTrashBlurEnabled: Boolean,
    onTrashBlurEnabledChange: (Boolean) -> Unit,
    isMuteVideoByDefault: Boolean,
    onMuteVideoByDefaultChange: (Boolean) -> Unit,
    isBlurAllMediaEnabled: Boolean,
    onBlurAllMediaEnabledChange: (Boolean) -> Unit,
    onEasterEggClick: () -> Unit,
    selectedTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    onManageHiddenFoldersClick: () -> Unit,
    selectedZoomType: ZoomType,
    onZoomTypeChange: (ZoomType) -> Unit,
    onManageTagsClick: () -> Unit,
    onBackupAndRestoreClick: () -> Unit,
    onGoToSecretStorage: () -> Unit,
    isVibrationEnabled: Boolean,
    onVibrationEnabledChange: (Boolean) -> Unit,
    isShowFileCountEnabled: Boolean,
    onShowFileCountChange: (Boolean) -> Unit,
    isShuffleButtonVisible: Boolean,
    onShuffleButtonVisibleChange: (Boolean) -> Unit,
    isShakeToBlurEnabled: Boolean,
    onShakeToBlurEnabledChange: (Boolean) -> Unit,
    isLoopVideoEnabled: Boolean,
    onLoopVideoEnabledChange: (Boolean) -> Unit,
    selectedBlurType: BlurType,
    onBlurTypeChange: (BlurType) -> Unit,
    isSwipeToDismissEnabled: Boolean,
    onSwipeToDismissEnabledChange: (Boolean) -> Unit,
    useLargeFab: Boolean,
    onUseLargeFabChange: (Boolean) -> Unit,
    autoDeleteTrashEnabled: Boolean,
    onAutoDeleteTrashEnabledChange: (Boolean) -> Unit,
    autoDeleteTrashDays: Int,
    onAutoDeleteTrashDaysChange: (Int) -> Unit,
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    onCheckForUpdates: () -> Unit,
    currentVersion: String,
    selectedFabAction: FabAction,
    onFabActionChange: (FabAction) -> Unit
) {
    val context = LocalContext.current
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var zoomTypeMenuExpanded by remember { mutableStateOf(false) }
    var blurTypeMenuExpanded by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var fabActionMenuExpanded by remember { mutableStateOf(false) }
    var showSpecialLanguageDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var specialLanguageUnlocked by remember { mutableStateOf(SettingsRepository.isSpecialLanguageUnlocked(context)) }
    val isBiometricAvailable = remember { BiometricUtils.isBiometricAvailable(context) }

    @Composable
    fun getThemeName(theme: Theme): String {
        return when (theme) {
            Theme.SYSTEM -> stringResource(id = R.string.theme_system)
            Theme.LIGHT -> stringResource(id = R.string.theme_light)
            Theme.DARK -> stringResource(id = R.string.theme_dark)
        }
    }

    @Composable
    fun getFabActionName(fabAction: FabAction): String {
        return when (fabAction) {
            FabAction.SHUFFLE -> stringResource(id = R.string.fab_action_shuffle)
            FabAction.CAMERA -> stringResource(id = R.string.fab_action_camera)
        }
    }


    @Composable
    fun getZoomTypeName(zoomType: ZoomType): String {
        return when (zoomType) {
            ZoomType.PINCH -> stringResource(id = R.string.zoom_type_pinch)
            ZoomType.DOUBLE_TAP -> stringResource(id = R.string.zoom_type_double_tap)
        }
    }

    @Composable
    fun getBlurTypeName(blurType: BlurType): String {
        return when (blurType) {
            BlurType.BLUR -> stringResource(id = R.string.blur_type_blur)
            BlurType.PLACEHOLDER -> stringResource(id = R.string.blur_type_placeholder)
        }
    }

    @Composable
    fun getLanguageName(language: Language): String {
        return when (language) {
            Language.SYSTEM -> stringResource(id = R.string.language_system)
            Language.ENGLISH -> stringResource(id = R.string.language_english)
            Language.RUSSIAN -> stringResource(id = R.string.language_russian)
            Language.SPECIAL -> stringResource(id = R.string.language_special)
        }
    }

    if (showSpecialLanguageDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSpecialLanguageDialog = false },
            title = { Text(stringResource(id = R.string.enter_special_language_code_title)) },
            text = {
                TextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(id = R.string.special_language_code_label)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (code == "Трип-МоиПрефиолетовыеВнутренности") {
                        SettingsRepository.setSpecialLanguageUnlocked(context, true)
                        specialLanguageUnlocked = true
                        onLanguageChange(Language.SPECIAL)
                    }
                    showSpecialLanguageDialog = false
                }) {
                    Text(stringResource(id = R.string.activate_button))
                }
            },
            dismissButton = {
                Button(onClick = { showSpecialLanguageDialog = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
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
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(stringResource(id = R.string.main_section_title), Icons.Default.Build)
                SettingsRow(title = stringResource(id = R.string.language_label)) {
                    Box {
                        TextButton(onClick = { languageMenuExpanded = true }) {
                            Text(getLanguageName(selectedLanguage))
                        }
                        DropdownMenu(expanded = languageMenuExpanded, onDismissRequest = { languageMenuExpanded = false }) {
                            Language.entries.forEach { language ->
                                if (language == Language.SPECIAL && !specialLanguageUnlocked) {
                                    DropdownMenuItem(text = { Text(getLanguageName(language)) }, onClick = { showSpecialLanguageDialog = true; languageMenuExpanded = false })
                                } else {
                                    DropdownMenuItem(text = { Text(getLanguageName(language)) }, onClick = { onLanguageChange(language); languageMenuExpanded = false })
                                }
                            }
                        }
                    }
                }
                SettingsRow(title = stringResource(id = R.string.theme_label)) {
                    Box {
                        TextButton(onClick = { themeMenuExpanded = true }) {
                            Text(getThemeName(selectedTheme))
                        }
                        DropdownMenu(expanded = themeMenuExpanded, onDismissRequest = { themeMenuExpanded = false }) {
                            Theme.entries.forEach { theme ->
                                DropdownMenuItem(text = { Text(getThemeName(theme)) }, onClick = { onThemeChange(theme); themeMenuExpanded = false })
                            }
                        }
                    }
                }
                SettingsRow(title = stringResource(id = R.string.fab_action_title)) {
                    Box {
                        TextButton(onClick = { fabActionMenuExpanded = true }) {
                            Text(getFabActionName(selectedFabAction))
                        }
                        DropdownMenu(expanded = fabActionMenuExpanded, onDismissRequest = { fabActionMenuExpanded = false }) {
                            FabAction.entries.forEach { fabAction ->
                                DropdownMenuItem(text = { Text(getFabActionName(fabAction)) }, onClick = { onFabActionChange(fabAction); fabActionMenuExpanded = false })
                            }
                        }
                    }
                }
                SettingsRow(title = stringResource(id = R.string.auto_delete_trash_label)) {
                    Switch(checked = autoDeleteTrashEnabled, onCheckedChange = onAutoDeleteTrashEnabledChange)
                }
                if (autoDeleteTrashEnabled) {
                    SettingsRow(title = stringResource(id = R.string.auto_delete_trash_days_label)) {
                        TextField(value = autoDeleteTrashDays.toString(), onValueChange = { value -> onAutoDeleteTrashDaysChange(value.filter { it.isDigit() }.toIntOrNull() ?: 0) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(80.dp))
                    }
                }
                SettingsRow(title = stringResource(id = R.string.show_shuffle_button_label)) {
                    Switch(checked = isShuffleButtonVisible, onCheckedChange = onShuffleButtonVisibleChange)
                }
                SettingsRow(title = stringResource(id = R.string.use_large_shuffle_button_label)) {
                    Switch(checked = useLargeFab, onCheckedChange = onUseLargeFabChange)
                }
                SettingsRow(title = stringResource(id = R.string.vibration_label)) {
                    Switch(checked = isVibrationEnabled, onCheckedChange = onVibrationEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.show_file_count_in_folders_label)) {
                    Switch(checked = isShowFileCountEnabled, onCheckedChange = onShowFileCountChange)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(stringResource(id = R.string.privacy_section_title), Icons.Default.PrivacyTip)
                SettingsRow(title = stringResource(id = R.string.blur_folder_previews_label)) {
                    Switch(checked = isBlurEnabled, onCheckedChange = onBlurEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.blur_in_folders_label)) {
                    Switch(checked = isBlurInFolderEnabled, onCheckedChange = onBlurInFolderEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.blur_media_in_trash_label)) {
                    Switch(checked = isTrashBlurEnabled, onCheckedChange = onTrashBlurEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.blur_all_media_label)) {
                    Switch(checked = isBlurAllMediaEnabled, onCheckedChange = onBlurAllMediaEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.shake_to_blur_label)) {
                    Switch(checked = isShakeToBlurEnabled, onCheckedChange = onShakeToBlurEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.blur_type_label)) {
                    Box {
                        TextButton(onClick = { blurTypeMenuExpanded = true }) {
                            Text(getBlurTypeName(selectedBlurType))
                        }
                        DropdownMenu(expanded = blurTypeMenuExpanded, onDismissRequest = { blurTypeMenuExpanded = false }) {
                            BlurType.entries.forEach { blurType ->
                                DropdownMenuItem(text = { Text(getBlurTypeName(blurType)) }, onClick = { onBlurTypeChange(blurType); blurTypeMenuExpanded = false })
                            }
                        }
                    }
                }
                if (isBiometricAvailable) {
                    Button(onClick = onGoToSecretStorage, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(stringResource(id = R.string.secret_storage))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(stringResource(id = R.string.media_section_title), Icons.Default.Photo)
                SettingsRow(title = stringResource(id = R.string.mute_video_by_default_label)) {
                    Switch(checked = isMuteVideoByDefault, onCheckedChange = onMuteVideoByDefaultChange)
                }
                SettingsRow(title = stringResource(id = R.string.loop_video_label)) {
                    Switch(checked = isLoopVideoEnabled, onCheckedChange = onLoopVideoEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.swipe_to_dismiss_label)) {
                    Switch(checked = isSwipeToDismissEnabled, onCheckedChange = onSwipeToDismissEnabledChange)
                }
                SettingsRow(title = stringResource(id = R.string.zoom_gesture_label)) {
                    Box {
                        TextButton(onClick = { zoomTypeMenuExpanded = true }) {
                            Text(getZoomTypeName(selectedZoomType))
                        }
                        DropdownMenu(expanded = zoomTypeMenuExpanded, onDismissRequest = { zoomTypeMenuExpanded = false }) {
                            ZoomType.entries.forEach { zoomType ->
                                DropdownMenuItem(text = { Text(getZoomTypeName(zoomType)) }, onClick = { onZoomTypeChange(zoomType); zoomTypeMenuExpanded = false })
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsButtonRow(onClick = onManageHiddenFoldersClick, text = stringResource(id = R.string.manage_hidden_folders_button))
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsButtonRow(onClick = onManageTagsClick, text = stringResource(id = R.string.manage_tags_button))
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsButtonRow(onClick = onBackupAndRestoreClick, text = stringResource(id = R.string.backup_and_restore_button))
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsHeader(stringResource(id = R.string.about_section_title), Icons.Default.Info)
                SettingsButtonRow(onClick = { val intent = Intent(Intent.ACTION_VIEW, "https://github.com/HosikoOuma/MyGalleryApp".toUri()); context.startActivity(intent) }, text = stringResource(id = R.string.github_button))
                Divider()
                SettingsButtonRow(onClick = onCheckForUpdates, text = stringResource(id = R.string.check_for_updates_button))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(id = R.string.version_label, currentVersion), modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onEasterEggClick, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(id = R.string.easter_egg_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}


@Composable
private fun SettingsButtonRow(onClick: () -> Unit, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Fix for ripple crash
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}