package com.example.nkdsify.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.nkdsify.data.Language
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.performVibration


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
    onLanguageChange: (Language) -> Unit
) {
    val context = LocalContext.current
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var zoomTypeMenuExpanded by remember { mutableStateOf(false) }
    var blurTypeMenuExpanded by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var showSpecialLanguageDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    @Composable
    fun getThemeName(theme: Theme): String {
        return when (theme) {
            Theme.SYSTEM -> stringResource(id = R.string.theme_system)
            Theme.LIGHT -> stringResource(id = R.string.theme_light)
            Theme.DARK -> stringResource(id = R.string.theme_dark)
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
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.language_label))
            Box {
                TextButton(onClick = { 
                    if (isVibrationEnabled) performVibration(context)
                    languageMenuExpanded = true 
                }) {
                    Text(getLanguageName(selectedLanguage))
                }
                DropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false }
                ) {
                    Language.entries.forEach { language ->
                        if (language == Language.SPECIAL) {
                            DropdownMenuItem(
                                text = { Text(getLanguageName(language)) },
                                onClick = { 
                                    if (isVibrationEnabled) performVibration(context)
                                    showSpecialLanguageDialog = true
                                    languageMenuExpanded = false 
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(getLanguageName(language)) },
                                onClick = { 
                                    if (isVibrationEnabled) performVibration(context)
                                    onLanguageChange(language)
                                    languageMenuExpanded = false 
                                }
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.auto_delete_trash_label))
            Switch(
                checked = autoDeleteTrashEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onAutoDeleteTrashEnabledChange(it)
                }
            )
        }
        if (autoDeleteTrashEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.auto_delete_trash_days_label))
                TextField(
                    value = autoDeleteTrashDays.toString(),
                    onValueChange = { value ->
                        if (isVibrationEnabled) performVibration(context)
                        val intValue = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                        onAutoDeleteTrashDaysChange(intValue)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.use_large_shuffle_button_label))
            Switch(
                checked = useLargeFab,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onUseLargeFabChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.blur_folder_previews_label))
            Switch(
                checked = isBlurEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onBlurEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.blur_in_folders_label))
            Switch(
                checked = isBlurInFolderEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onBlurInFolderEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.blur_media_in_trash_label))
            Switch(
                checked = isTrashBlurEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onTrashBlurEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.mute_video_by_default_label))
            Switch(
                checked = isMuteVideoByDefault,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onMuteVideoByDefaultChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.blur_all_media_label))
            Switch(
                checked = isBlurAllMediaEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onBlurAllMediaEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.show_file_count_in_folders_label))
            Switch(
                checked = isShowFileCountEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onShowFileCountChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.show_shuffle_button_label))
            Switch(
                checked = isShuffleButtonVisible,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onShuffleButtonVisibleChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.vibration_label))
            Switch(
                checked = isVibrationEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onVibrationEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.shake_to_blur_label))
            Switch(
                checked = isShakeToBlurEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onShakeToBlurEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.loop_video_label))
            Switch(
                checked = isLoopVideoEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onLoopVideoEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.swipe_to_dismiss_label))
            Switch(
                checked = isSwipeToDismissEnabled,
                onCheckedChange = {
                    if (isVibrationEnabled) performVibration(context)
                    onSwipeToDismissEnabledChange(it)
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.theme_label))
            Box {
                TextButton(onClick = { 
                    if (isVibrationEnabled) performVibration(context)
                    themeMenuExpanded = true 
                }) {
                    Text(getThemeName(selectedTheme))
                }
                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    Theme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(getThemeName(theme)) },
                            onClick = { 
                                if (isVibrationEnabled) performVibration(context)
                                onThemeChange(theme)
                                themeMenuExpanded = false 
                            }
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.zoom_gesture_label))
            Box {
                TextButton(onClick = { 
                    if (isVibrationEnabled) performVibration(context)
                    zoomTypeMenuExpanded = true 
                }) {
                    Text(getZoomTypeName(selectedZoomType))
                }
                DropdownMenu(
                    expanded = zoomTypeMenuExpanded,
                    onDismissRequest = { zoomTypeMenuExpanded = false }
                ) {
                    ZoomType.entries.forEach { zoomType ->
                        DropdownMenuItem(
                            text = { Text(getZoomTypeName(zoomType)) },
                            onClick = { 
                                if (isVibrationEnabled) performVibration(context)
                                onZoomTypeChange(zoomType)
                                zoomTypeMenuExpanded = false 
                            }
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.blur_type_label))
            Box {
                TextButton(onClick = { 
                    if (isVibrationEnabled) performVibration(context)
                    blurTypeMenuExpanded = true 
                }) {
                    Text(getBlurTypeName(selectedBlurType))
                }
                DropdownMenu(
                    expanded = blurTypeMenuExpanded,
                    onDismissRequest = { blurTypeMenuExpanded = false }
                ) {
                    BlurType.entries.forEach { blurType ->
                        DropdownMenuItem(
                            text = { Text(getBlurTypeName(blurType)) },
                            onClick = { 
                                if (isVibrationEnabled) performVibration(context)
                                onBlurTypeChange(blurType)
                                blurTypeMenuExpanded = false 
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onManageHiddenFoldersClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(id = R.string.manage_hidden_folders_button))
        }
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onManageTagsClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(id = R.string.manage_tags_button))
        }
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onBackupAndRestoreClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(id = R.string.backup_and_restore_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                if (isVibrationEnabled) performVibration(context)
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/HosikoOuma/MyGalleryApp".toUri())
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(id = R.string.github_button))
        }
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onEasterEggClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(id = R.string.easter_egg_button))
        }
    }
}
