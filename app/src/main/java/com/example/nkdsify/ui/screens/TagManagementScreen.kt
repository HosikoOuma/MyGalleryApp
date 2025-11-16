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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.abs


@Composable
fun TagManagementScreen(
    allTags: List<String>,
    onDeleteTag: (String) -> Unit,
    onEditTag: (oldTag: String, newTag: String) -> Unit,
    onAddNewTag: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onMoveTag: (from: Int, to: Int) -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    if (showEditDialog != null) {
        val oldTag = showEditDialog!!
        var newTag by remember(oldTag) { mutableStateOf(oldTag) }
        val isError = newTag.isNotBlank() && newTag != oldTag && newTag in allTags

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(stringResource(id = R.string.edit_tag_dialog_title)) },
            text = {
                Column {
                    TextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text(stringResource(id = R.string.new_tag_name_label)) },
                        isError = isError
                    )
                    if (isError) {
                        Text(stringResource(R.string.tag_already_exists), color = MaterialTheme.colorScheme.error)
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
                    enabled = newTag.isNotBlank() && (newTag == oldTag || newTag !in allTags)
                ) {
                    Text(stringResource(id = R.string.save_button))
                }
            },
            dismissButton = {
                Button(onClick = {
                    showEditDialog = null
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        var newTag by remember { mutableStateOf("") }
        val isError = newTag.isNotBlank() && newTag in allTags

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(id = R.string.add_tag_dialog_title)) },
            text = {
                Column {
                    TextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text(stringResource(id = R.string.tag_name_label)) },
                        isError = isError
                    )
                    if (isError) {
                        Text(stringResource(R.string.tag_already_exists), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNewTag(newTag)
                        showAddDialog = false
                        if (isVibrationEnabled(context)) performVibration(context)
                    },
                    enabled = newTag.isNotBlank() && newTag !in allTags
                ) {
                    Text(stringResource(id = R.string.add_button))
                }
            },
            dismissButton = {
                Button(onClick = {
                    showAddDialog = false
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    if (tagToDelete != null) {
        val tag = tagToDelete!!
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text(stringResource(R.string.delete_tag_dialog_title)) },
            text = { Text(stringResource(R.string.delete_tag_confirmation_text, tag)) },
            confirmButton = {
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
            },
            dismissButton = {
                Button(onClick = {
                    tagToDelete = null
                    if (isVibrationEnabled(context)) performVibration(context)
                }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (allTags.isEmpty()) {
            Text(stringResource(id = R.string.no_tags_found), modifier = Modifier.align(Alignment.Center))
        } else {
            val state = rememberReorderableLazyListState(onMove = { from, to ->
                onMoveTag(from.index, to.index)
                if (isVibrationEnabled(context)) {
                    performVibration(context)
                }
            })

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
                state = state.listState,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .reorderable(state),
                flingBehavior = slowerFlingBehavior
            ) {
                items(allTags, { it }) { tag ->
                    ReorderableItem(state, key = tag) { isDragging ->
                        LaunchedEffect(isDragging) {
                            if (isDragging) {
                                if (isVibrationEnabled(context)) {
                                    performVibration(context)
                                }
                            }
                        }
                        val elevation by animateDpAsState(if (isDragging) 16.dp else 2.dp)
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
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "Reorder",
                                        modifier = Modifier.detectReorder(state)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_new_tag_content_description))
        }
    }
}
