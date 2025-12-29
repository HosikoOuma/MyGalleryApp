package com.example.nkdsify.ui

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nkdsify.MyAppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppTopBar(
    myAppState: MyAppState,
    isVibrationEnabled: Boolean,
    title: String,
    favorites: SnapshotStateList<String>,
    context: Context,
    scrollBehavior: TopAppBarScrollBehavior // Добавляем параметр
) {
    TopBar(
        myAppState = myAppState, 
        favorites = favorites, 
        title = title, 
        context = context,
        scrollBehavior = scrollBehavior // Пробрасываем дальше
    )
}
