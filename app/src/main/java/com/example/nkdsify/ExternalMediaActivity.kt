package com.example.nkdsify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.nkdsify.data.MediaItem
import com.example.nkdsify.data.MediaViewerState
import com.example.nkdsify.ui.components.MediaViewer
import com.example.nkdsify.ui.components.utils.rememberCoilImageLoader
import com.example.nkdsify.ui.dialogs.*
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.utils.getMediaDetails
import kotlinx.collections.immutable.persistentListOf

class ExternalMediaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data ?: run {
            finish()
            return
        }

        setContent {
            val context = this
            val myAppState = rememberMyAppState()
            val theme = remember { SettingsRepository.getTheme(context) }
            val imageLoader = rememberCoilImageLoader(context)

            val mediaItem = remember(uri) {
                val details = getMediaDetails(context, uri)
                val isVideo = contentResolver.getType(uri)?.startsWith("video/") == true
                MediaItem(
                    uri = uri,
                    name = details?.name ?: "",
                    absolutePath = details?.path ?: "",
                    isVideo = isVideo,
                    size = details?.size ?: 0L,
                    dateAdded = details?.dateAdded ?: 0L,
                    dateModified = details?.dateModified ?: 0L
                )
            }

            val viewerState = remember(mediaItem) {
                MediaViewerState(
                    items = persistentListOf(mediaItem),
                    startIndex = 0,
                    isExternal = true
                )
            }
            
            LaunchedEffect(viewerState) {
                myAppState.viewerState = viewerState
            }

            NkdsifyAppTheme(theme = theme) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // Include necessary dialogs for interaction
                    InfoDialogs(myAppState = myAppState, screenWidth = 0, screenHeight = 0, onFind = {})
                    TagDialogs(myAppState = myAppState, onAddNewTag = {}, favorites = myAppState.favoritesList, isVibrationEnabled = myAppState.isVibrationEnabled)
                    DeletionDialogs(myAppState = myAppState, isVibrationEnabled = myAppState.isVibrationEnabled)
                    
                    MediaViewer(
                        myAppState = myAppState,
                        items = viewerState.items,
                        startIndex = viewerState.startIndex,
                        favorites = myAppState.favoritesList,
                        imageLoader = imageLoader,
                        isExternal = true
                    )
                }
            }
            
            BackHandler {
                finish()
            }
            
            LaunchedEffect(myAppState.viewerState) {
                if (myAppState.viewerState == null) {
                    finish()
                }
            }
        }
    }
}
