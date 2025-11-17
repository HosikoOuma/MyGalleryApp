package com.example.nkdsify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.Screen

@Composable
fun MyAppBackHandler(myAppState: MyAppState) {
    BackHandler(enabled = myAppState.isSelectionMode) {
        myAppState.selectedItems.clear()
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.FolderContent) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Favorites && (myAppState.currentScreen as Screen.Favorites).openAlbumName == null) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Favorites && (myAppState.currentScreen as Screen.Favorites).openAlbumName != null) {
        myAppState.currentScreen = Screen.Favorites()
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Settings) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.TagManagement) {
        myAppState.currentScreen = Screen.Settings
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.Trash) { myAppState.currentScreen = Screen.Folders }
    BackHandler(enabled = myAppState.currentScreen is Screen.AllMedia) {
        myAppState.currentScreen = Screen.Folders
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.MediaByTag) {
        myAppState.currentScreen = Screen.TagManagement
    }
    BackHandler(enabled = myAppState.currentScreen is Screen.SecretStorage) {
        myAppState.currentScreen = Screen.Settings
    }
}
