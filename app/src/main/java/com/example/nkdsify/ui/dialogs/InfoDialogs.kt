package com.example.nkdsify.ui.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.nkdsify.FileOperation
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.AlbumDetails
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.components.AlbumDetailsDialog
import com.example.nkdsify.ui.utils.MediaDetailsDialog
import com.example.nkdsify.ui.utils.RenameDialog
import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.renameMedia
import com.example.nkdsify.ui.utils.SelectionDetailsDialog

@Composable
fun InfoDialogs(myAppState: MyAppState, screenWidth: Int, screenHeight: Int) {
    val context = LocalContext.current
    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedImageUri = result.uriContent

            if (croppedImageUri != null) {
                if (myAppState.isSettingWallpaper) {
                    try {
                        val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                        context.contentResolver.openInputStream(croppedImageUri)?.use { inputStream ->
                            wallpaperManager.setStream(inputStream)
                            android.widget.Toast.makeText(context, context.getString(R.string.wallpaper_set_successfully), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, context.getString(R.string.failed_to_set_wallpaper, e.message), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    myAppState.isSettingWallpaper = false
                    myAppState.viewerState = null
                }
            }
        }
        myAppState.showDetailsDialog = null
    }
    if (myAppState.showSelectionDetailsDialog) {
        SelectionDetailsDialog(
            details = myAppState.selectionDetails,
            onDismiss = { myAppState.showSelectionDetailsDialog = false }
        )
    }

    if (myAppState.showDetailsDialog != null) {
        val uri = myAppState.showDetailsDialog!!
        val details = getMediaDetails(context, uri)
        if (details != null) {
            MediaDetailsDialog(
                details = details,
                onDismiss = { myAppState.showDetailsDialog = null },
                onSetAsWallpaper = {
                    myAppState.isSettingWallpaper = true
                    val cropOptions = CropImageContractOptions(uri, CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        fixAspectRatio = true,
                        aspectRatioX = screenWidth,
                        aspectRatioY = screenHeight,
                        outputRequestWidth = screenWidth,
                        outputRequestHeight = screenHeight,
                        outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT
                    ))
                    cropImageLauncher.launch(cropOptions)
                },
                onCopy = {
                    myAppState.filesToProcess = listOf(uri)
                    myAppState.currentFileOperation = FileOperation.COPY
                    myAppState.showFolderSelectionDialog = true
                    myAppState.showDetailsDialog = null
                },
                onMove = {
                    myAppState.filesToProcess = listOf(uri)
                    myAppState.currentFileOperation = FileOperation.MOVE
                    myAppState.showFolderSelectionDialog = true
                    myAppState.showDetailsDialog = null
                },
                onRename = {
                    myAppState.showRenameDialog = uri
                    myAppState.showDetailsDialog = null
                },
                onMoveToSecret = {
                    myAppState.showConfirmMoveToSecretDialog = true
                },
            )
        }
    }

    if (myAppState.showRenameDialog != null) {
        val uri = myAppState.showRenameDialog!!
        val currentName = getMediaDetails(context, uri)?.name ?: ""
        RenameDialog(
            currentName = currentName,
            onDismiss = { myAppState.showRenameDialog = null },
            onRename = { newName ->
                renameMedia(context, uri, newName)
                myAppState.showRenameDialog = null
                myAppState.refreshTrigger++
            }
        )
    }

    if (myAppState.showAlbumDetailsDialog) {
        val screen = myAppState.currentScreen
        if (screen is Screen.FolderContent) {
            val folder = screen.folder
            val path = getMediaDetails(context, folder.items.first().uri)?.path?.substringBeforeLast('/') ?: ""
            AlbumDetailsDialog(
                details = AlbumDetails(path, folder.totalSize, folder.dateRange, folder.itemCount),
                onDismiss = { myAppState.showAlbumDetailsDialog = false })
        } else if (screen is Screen.Favorites && screen.openAlbumName != null) {
            val taggedAlbums = myAppState.favoriteItems
                .flatMap { item -> (myAppState.tags[item.uri.toString()] ?: emptySet()).map { tag -> tag to item } }
                .groupBy({ it.first }, { it.second })
            val albumItems = if (screen.openAlbumName == stringResource(id = R.string.album_name_all_favorites)) myAppState.favoriteItems else taggedAlbums[screen.openAlbumName] ?: emptySet()

            if (albumItems.isNotEmpty()) {
                val totalSize = albumItems.sumOf { it.size }
                AlbumDetailsDialog(
                    details = AlbumDetails(totalSize = totalSize, itemCount = albumItems.size),
                    onDismiss = { myAppState.showAlbumDetailsDialog = false })
            }
        }
    }
}
