package com.example.nkdsify.ui.impl

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.SoftwareKeyboardController
import coil.ImageLoader
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.screens.FoldersGrid
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FoldersScreen(
    visibleFolders: ImmutableList<MediaFolder>,
    imageLoader: ImageLoader,
    keyboardController: SoftwareKeyboardController?,
    myAppState: MyAppState,
    gridState: LazyGridState
) {
    FoldersGrid(
        folders = visibleFolders,
        imageLoader = imageLoader,
        onFolderClick = { folder ->
            keyboardController?.hide()
            myAppState.currentScreen = Screen.FolderContent(folder)
        },
        isBlurEnabled = myAppState.isBlurEnabled,
        gridState = gridState,
        isShowFileCountEnabled = myAppState.isShowFileCountEnabled,
        blurType = myAppState.selectedBlurType
    )
}