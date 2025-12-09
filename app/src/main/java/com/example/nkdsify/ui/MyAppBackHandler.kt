package com.example.nkdsify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.Screen

@Composable
fun MyAppBackHandler(myAppState: MyAppState) {
    // Priority 1: Restore previous viewer state if it exists
    BackHandler(enabled = myAppState.previousViewerState != null) {
        myAppState.viewerState = myAppState.previousViewerState
        myAppState.previousViewerState = null
    }

    // Priority 2: Clear selection if in selection mode
    BackHandler(enabled = myAppState.isSelectionMode && myAppState.previousViewerState == null) {
        myAppState.selectedItems.clear()
    }

    // Priority 3: Handle navigation between screens
    val canNavigateBack = !myAppState.isSelectionMode && myAppState.previousViewerState == null

    when (val screen = myAppState.currentScreen) {
        is Screen.FolderContent,
        is Screen.AllMedia,
        is Screen.Trash -> {
            BackHandler(enabled = canNavigateBack) {
                myAppState.currentScreen = Screen.Folders
            }
        }
        is Screen.Favorites -> {
            BackHandler(enabled = canNavigateBack) {
                if (screen.openAlbumName != null) {
                    myAppState.currentScreen = Screen.Favorites()
                } else {
                    myAppState.currentScreen = Screen.Folders
                }
            }
        }
        is Screen.Settings -> {
            BackHandler(enabled = canNavigateBack) {
                myAppState.currentScreen = Screen.Folders
            }
        }
        is Screen.TagManagement,
        is Screen.SecretStorage,
        is Screen.ViewHistory,
        is Screen.About -> {
            BackHandler(enabled = canNavigateBack) {
                myAppState.currentScreen = Screen.Settings
            }
        }
        is Screen.MediaByTag -> {
            BackHandler(enabled = canNavigateBack) {
                myAppState.currentScreen = Screen.TagManagement
            }
        }
        is Screen.Folders -> {
            // Top-level screen, do nothing special. Let system handle it.
        }
    }
}
