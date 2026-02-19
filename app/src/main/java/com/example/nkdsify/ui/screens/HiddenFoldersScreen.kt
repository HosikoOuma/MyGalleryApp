
package com.example.nkdsify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nkdsify.MyAppState
import com.example.nkdsify.ui.utils.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenFoldersScreen(
    myAppState: MyAppState,
    listState: LazyListState
) {
    val context = LocalContext.current

    LazyColumn(state = listState) {
        items(myAppState.allFolders) { folder ->
            val isHidden = myAppState.hiddenFolders.contains(folder.id.toString())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = {
                            val newHiddenFolders = myAppState.hiddenFolders.toMutableSet()
                            if (isHidden) {
                                newHiddenFolders.remove(folder.id.toString())
                            } else {
                                newHiddenFolders.add(folder.id.toString())
                            }
                            myAppState.hiddenFolders = newHiddenFolders
                            SettingsRepository.setHiddenFolders(context, newHiddenFolders)
                        }
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isHidden,
                    onCheckedChange = null
                )
                Text(
                    text = folder.name,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
