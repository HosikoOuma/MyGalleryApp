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
import com.example.nkdsify.ui.utils.VibrationStrength

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
    selectedVibrationStrength: VibrationStrength,
    onVibrationStrengthChange: (VibrationStrength) -> Unit,
    isShowFileCountEnabled: Boolean,
    onShowFileCountChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var zoomTypeMenuExpanded by remember { mutableStateOf(false) }
    var vibrationStrengthMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                onCheckedChange = onBlurEnabledChange
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
                onCheckedChange = onTrashBlurEnabledChange
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
                onCheckedChange = onMuteVideoByDefaultChange
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
                onCheckedChange = onBlurAllMediaEnabledChange
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
                onCheckedChange = onShowFileCountChange
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
                TextButton(onClick = { themeMenuExpanded = true }) {
                    Text(selectedTheme.name)
                }
                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    Theme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.name) },
                            onClick = { onThemeChange(theme); themeMenuExpanded = false }
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
                TextButton(onClick = { zoomTypeMenuExpanded = true }) {
                    Text(selectedZoomType.name)
                }
                DropdownMenu(
                    expanded = zoomTypeMenuExpanded,
                    onDismissRequest = { zoomTypeMenuExpanded = false }
                ) {
                    ZoomType.entries.forEach { zoomType ->
                        DropdownMenuItem(
                            text = { Text(zoomType.name) },
                            onClick = { onZoomTypeChange(zoomType); zoomTypeMenuExpanded = false }
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
            Text("Vibration Strength")
            Box {
                TextButton(onClick = { vibrationStrengthMenuExpanded = true }) {
                    Text(selectedVibrationStrength.name)
                }
                DropdownMenu(
                    expanded = vibrationStrengthMenuExpanded,
                    onDismissRequest = { vibrationStrengthMenuExpanded = false }
                ) {
                    VibrationStrength.entries.forEach { strength ->
                        DropdownMenuItem(
                            text = { Text(strength.name) },
                            onClick = { onVibrationStrengthChange(strength); vibrationStrengthMenuExpanded = false }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onManageHiddenFoldersClick) {
            Text("Manage Hidden Folders")
        }
        Button(onClick = onManageTagsClick) {
            Text("Manage Tags")
        }
        Button(onClick = onBackupAndRestoreClick) {
            Text("Backup and Restore")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { 
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/HosikoOuma/MyGalleryApp".toUri())
            context.startActivity(intent)
        }) {
            Text("GitHub")
        }
        Button(onClick = onEasterEggClick) {
            Text("🐱")
        }
    }
}
