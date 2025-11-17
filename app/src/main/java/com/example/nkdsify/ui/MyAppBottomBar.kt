package com.example.nkdsify.ui

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun MyAppBottomBar(
    myAppState: MyAppState,
    context: Context,
    isVibrationEnabled: Boolean
) {
    var lastTap by rememberSaveable { mutableLongStateOf(0L) }
    var tapCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(myAppState.currentScreen) {
        tapCount = 0
    }

    NavigationBar {
        AnimatableNavigationBarItem(
            selected = myAppState.currentScreen is Screen.Settings,
            onClick = { 
                if (isVibrationEnabled) performVibration(context)
                myAppState.currentScreen = Screen.Settings 
            },
            icon = Icons.Filled.Settings,
            label = stringResource(id = R.string.screen_title_settings),
            contentDescription = stringResource(id = R.string.settings_content_description)
        )
        AnimatableNavigationBarItem(
            selected = myAppState.currentScreen is Screen.Trash,
            onClick = { if (isVibrationEnabled) performVibration(context); myAppState.currentScreen = Screen.Trash },
            icon = Icons.Filled.Delete,
            label = stringResource(id = R.string.screen_title_trash),
            contentDescription = stringResource(id = R.string.trash_content_description)
        )
        AnimatableNavigationBarItem(
            selected = myAppState.currentScreen is Screen.Folders || myAppState.currentScreen is Screen.FolderContent,
            onClick = { if (isVibrationEnabled) performVibration(context); myAppState.currentScreen = Screen.Folders },
            icon = Icons.Filled.PhotoLibrary,
            label = stringResource(id = R.string.screen_title_folders),
            contentDescription = stringResource(id = R.string.folders_content_description)
        )
        AnimatableNavigationBarItem(
            selected = myAppState.currentScreen is Screen.AllMedia,
            onClick = { if (isVibrationEnabled) performVibration(context); myAppState.currentScreen = Screen.AllMedia },
            icon = Icons.Default.PermMedia,
            label = stringResource(id = R.string.screen_title_all_media),
            contentDescription = stringResource(id = R.string.all_media_content_description)
        )
        AnimatableNavigationBarItem(
            selected = myAppState.currentScreen is Screen.Favorites,
            onClick = {
                if (isVibrationEnabled) performVibration(context)
                myAppState.currentScreen = Screen.Favorites()
                val now = System.currentTimeMillis()
                if (now - lastTap < 500) {
                    tapCount++
                } else {
                    tapCount = 1
                }
                lastTap = now

                if (tapCount == 10) {
                    if (isVibrationEnabled) performVibration(context)
                    tapCount = 0
                    Toast.makeText(context, context.getString(R.string.uwu_toast), Toast.LENGTH_SHORT).show()
                    val mediaPlayer = MediaPlayer.create(context, R.raw.uwu)
                    mediaPlayer.setOnCompletionListener { it.release() }
                    mediaPlayer.start()
                }
            },
            icon = Icons.Filled.Favorite,
            label = stringResource(id = R.string.screen_title_favorites),
            contentDescription = stringResource(id = R.string.favorites_content_description)
        )
    }
}
