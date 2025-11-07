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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun SettingsScreen(
    isBlurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
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
    onShuffleButtonVisibleChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var zoomTypeMenuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
            Text("Blur folder previews")
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
            Text("Blur media in trash")
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
            Text("Mute video by default")
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
            Text("Blur all media")
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
            Text("Show file count in folders")
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
            Text("Show Shuffle Button")
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
            Text("Vibration")
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
            Text("Theme")
            Box {
                TextButton(onClick = { 
                    if (isVibrationEnabled) performVibration(context)
                    themeMenuExpanded = true 
                }) {
                    Text(selectedTheme.name)
                }
                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    Theme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.name) },
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
            Text("Zoom Gesture")
            Box {
                TextButton(onClick = { 
                    if (isVibrationEnabled) performVibration(context)
                    zoomTypeMenuExpanded = true 
                }) {
                    Text(selectedZoomType.name)
                }
                DropdownMenu(
                    expanded = zoomTypeMenuExpanded,
                    onDismissRequest = { zoomTypeMenuExpanded = false }
                ) {
                    ZoomType.entries.forEach { zoomType ->
                        DropdownMenuItem(
                            text = { Text(zoomType.name) },
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onManageHiddenFoldersClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Manage Hidden Folders")
        }
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onManageTagsClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Manage Tags")
        }
        Button(
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                onBackupAndRestoreClick()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Backup and Restore")
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
            Text("GitHub")
        }
        Button(
            onClick = onEasterEggClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🐱")
        }
    }
}
