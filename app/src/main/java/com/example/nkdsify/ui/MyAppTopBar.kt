package com.example.nkdsify.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.utils.getMediaDetails
import com.example.nkdsify.ui.utils.performVibration
import kotlinx.collections.immutable.toImmutableList

@Composable
fun MyAppTopBar(
    myAppState: MyAppState,
    isVibrationEnabled: Boolean,
    title: String,
    favorites: SnapshotStateList<String>,
    context: Context
) {
    // simplified: TopBar now operates on MyAppState directly
    TopBar(myAppState = myAppState, favorites = favorites, title = title, context = context)
}
