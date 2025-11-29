package com.example.nkdsify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.Screen

@Composable
fun MyAppBackHandler(myAppState: MyAppState) {
    // Priority 1: Restore viewer state if it exists
    BackHandler(enabled = myAppState.previousViewerState != null) {
        myAppState.viewerState = myAppState.previousViewerState
        myAppState.previousViewerState = null
    }

    // Priority 2: Clear selection
    BackHandler(enabled = myAppState.isSelectionMode && myAppState.previousViewerState == null) {
        myAppState.selectedItems.clear()
    }

    // Priority 3: Navigate back from screens
    BackHandler(enabled = myAppState.currentScreen is Screen.FolderContent && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Favorites && (myAppState.currentScreen as Screen.Favorites).openAlbumName == null && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Favorites && (myAppState.currentScreen as Screen.Favorites).openAlbumName != null && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Favorites()
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Settings && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.TagManagement && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Settings
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Trash && myAppState.previousViewerState == null) { myAppState.currentScreen = Screen.Folders }
    BackHandler(enabled = myAppState.currentScreen is Screen.AllMedia && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.MediaByTag && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.TagManagement
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.SecretStorage && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Settings
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.ViewHistory && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Settings
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.About && myAppState.previousViewerState == null) {
        myAppState.currentScreen = Screen.Settings
    }
}
