package com.example.nkdsify.ui.screens

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
import com.example.nkdsify.ui.utils.SettingsRepository.isVibrationEnabled
import com.example.nkdsify.ui.utils.performVibration
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    allTags: List<String>,
    onDeleteTag: (String) -> Unit,
    onEditTag: (oldTag: String, newTag: String) -> Unit,
    onAddNewTag: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onMoveTag: (from: Int, to: Int) -> Unit,
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    // --- DIALOGS LOGIC ---
    if (showEditDialog != null) {
        val oldTag = showEditDialog!!
        var newTag by remember(oldTag) { mutableStateOf(oldTag) }
        val isError = newTag.isNotBlank() && newTag != oldTag && newTag in allTags

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(text = stringResource(id = R.string.edit_tag_dialog_title)) },
            text = {
                Column {
                    TextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text(stringResource(id = R.string.new_tag_name_label)) },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            stringResource(R.string.tag_already_exists),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditTag(oldTag, newTag)
                        showEditDialog = null
                        if (isVibrationEnabled(context)) performVibration(context)
                    },
                    enabled = newTag.isNotBlank() && !isError
                ) {
                    Text(stringResource(id = R.string.save_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = null
                        if (isVibrationEnabled(context)) performVibration(context)
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        var newTag by remember { mutableStateOf("") }
        val isError = newTag.isNotBlank() && newTag in allTags

        AlertDialog(
            onDismissRequest = onDismissAddDialog,
            title = { Text(text = stringResource(id = R.string.add_tag_dialog_title)) },
            text = {
                Column {
                    TextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text(stringResource(id = R.string.tag_name_label)) },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            stringResource(R.string.tag_already_exists),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNewTag(newTag)
                        onDismissAddDialog()
                        if (isVibrationEnabled(context)) performVibration(context)
                    },
                    enabled = newTag.isNotBlank() && !isError
                ) {
                    Text(stringResource(id = R.string.add_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismissAddDialog()
                        if (isVibrationEnabled(context)) performVibration(context)
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    if (tagToDelete != null) {
        val tag = tagToDelete!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { tagToDelete = null },
            sheetState = sheetState,
            
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_tag_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.delete_tag_confirmation_text, tag),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            tagToDelete = null
                            if (isVibrationEnabled(context)) performVibration(context)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(id = R.string.dialog_cancel))
                    }
                    Button(
                        onClick = {
                            onDeleteTag(tag)
                            tagToDelete = null
                            if (isVibrationEnabled(context)) performVibration(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_button))
                    }
                }
            }
        }
    }

    // --- MAIN UI ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (allTags.isEmpty()) {
            Text(stringResource(id = R.string.no_tags_found), modifier = Modifier.align(Alignment.Center))
        } else {
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyColumnState(lazyListState) { from, to ->
                onMoveTag(from.index, to.index)
                if (isVibrationEnabled(context)) {
                    performVibration(context)
                }
            }
            val slowerFlingBehavior = remember {
                object : FlingBehavior {
                    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                        val reducedVelocity = initialVelocity * 0.5f
                        if (abs(reducedVelocity) > 1f) {
                            var velocityLeft = reducedVelocity
                            var lastValue = 0f
                            val flingSpec = exponentialDecay<Float>()
                            AnimationState(
                                initialValue = 0f,
                                initialVelocity = reducedVelocity,
                            ).animateDecay(flingSpec) {
                                val delta = value - lastValue
                                lastValue = value
                                velocityLeft = this.velocity
                                val consumed = scrollBy(delta)
                                if (abs(delta - consumed) > 0.5f) {
                                    cancelAnimation()
                                }
                            }
                            return velocityLeft
                        }
                        return reducedVelocity
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                flingBehavior = slowerFlingBehavior
            ) {
                items(allTags, key = { it }) { tag ->
                    ReorderableItem(reorderableState, key = tag) { isDragging ->
                        LaunchedEffect(isDragging) {
                            if (isDragging) {
                                if (isVibrationEnabled(context)) {
                                    performVibration(context)
                                }
                            }
                        }

                        val elevation by animateDpAsState(if (isDragging) 16.dp else 2.dp, label = "Elevation")

                        Card(
                            onClick = {
                                onTagClick(tag)
                                if (isVibrationEnabled(context)) performVibration(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tag, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        showEditDialog = tag
                                        if (isVibrationEnabled(context)) performVibration(context)
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(id = R.string.edit_tag_content_description)
                                        )
                                    }
                                    IconButton(onClick = {
                                        tagToDelete = tag
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(id = R.string.delete_tag_content_description)
                                        )
                                    }
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.draggableHandle()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragIndicator,
                                            contentDescription = "Reorder"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
