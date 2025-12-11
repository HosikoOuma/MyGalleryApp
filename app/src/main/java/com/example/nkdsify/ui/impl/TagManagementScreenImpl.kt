package com.example.nkdsify.ui.impl

import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.screens.TagManagementScreen
import com.example.nkdsify.ui.utils.TagsRepository
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.toImmutableList

@Composable
fun TagManagementScreenImpl(
    myAppState: MyAppState,
    onAddNewTag: (String) -> Unit,
    onMoveTag: (Int, Int) -> Unit,
    context: android.content.Context
) {
    TagManagementScreen(
        onDeleteTag = {
            if (myAppState.isVibrationEnabled) performVibration(context)
            TagsRepository.removeTagFromAllItems(context, it)
            myAppState.tags = TagsRepository.getTags(context)
            myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
        },
        onEditTag = { oldTag, newTag ->
            if (myAppState.isVibrationEnabled) performVibration(context)
            TagsRepository.renameTag(context, oldTag, newTag)
            myAppState.tags = TagsRepository.getTags(context)
            myAppState.allTags = TagsRepository.getAllTags(context).toImmutableList()
        },
        onTagClick = { tag -> myAppState.currentScreen = Screen.MediaByTag(tag) },
        allTags = myAppState.allTags,
        onAddNewTag = onAddNewTag,
        onMoveTag = onMoveTag,
        showAddDialog = myAppState.showAddDialog,
        onDismissAddDialog = { myAppState.showAddDialog = false }
    )
}

