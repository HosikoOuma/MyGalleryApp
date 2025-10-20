package com.example.nkdsify.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.nkdsify.data.MediaFolder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersGrid(
    folders: List<MediaFolder>,
    imageLoader: ImageLoader,
    onFolderClick: (MediaFolder) -> Unit,
    isBlurEnabled: Boolean
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No folders found")
        }
        return
    }
    val gridState = rememberLazyGridState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {        items(folders, key = { it.id }, contentType = { "folder" }) { folder ->
            Card(modifier = Modifier
                .padding(8.dp)
                .aspectRatio(1f)
                .pointerInput(folder) { detectTapGestures(onTap = { _ -> onFolderClick(folder) }) }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {                Column {
                    Image(painter = rememberAsyncImagePainter(model = folder.coverUri, imageLoader = imageLoader), 
                          contentDescription = "Folder cover", 
                          contentScale = ContentScale.Crop, 
                          modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(if (isBlurEnabled) Modifier.blur(16.dp) else Modifier))
                    Text(text = folder.name, modifier = Modifier.padding(8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        }
//        CustomVerticalScrollbar(gridState = gridState, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
    }
}
