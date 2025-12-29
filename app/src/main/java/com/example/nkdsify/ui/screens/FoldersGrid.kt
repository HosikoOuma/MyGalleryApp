package com.example.nkdsify.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.nkdsify.R
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.MediaFolder
import com.example.nkdsify.ui.utils.SettingsRepository.isVibrationEnabled
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun FoldersGrid(
    folders: List<MediaFolder>,
    imageLoader: ImageLoader,
    onFolderClick: (MediaFolder) -> Unit,
    isBlurEnabled: Boolean,
    gridState: LazyGridState,
    isShowFileCountEnabled: Boolean,
    blurType: BlurType

) {
    val context = LocalContext.current
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_folders_found), color = Color.Gray)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(8.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            if (folder.items.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = { onFolderClick(folder)
                                if (isVibrationEnabled(context)) performVibration(context)
                            }
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (isBlurEnabled && blurType == BlurType.PLACEHOLDER) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FrontHand, // Вы можете поменять эту иконку на любую другую, например, на иконку руки
                                        contentDescription = "Hidden content",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = folder.items.first().uri,
                                        imageLoader = imageLoader
                                    ),
                                    contentDescription = "Folder preview for ${folder.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (isBlurEnabled && blurType == BlurType.BLUR) Modifier.blur(16.dp) else Modifier)
                                )
                            }
                        }
                        Text(
                            text = if (isShowFileCountEnabled) "${folder.name} (${folder.items.size})" else folder.name,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
