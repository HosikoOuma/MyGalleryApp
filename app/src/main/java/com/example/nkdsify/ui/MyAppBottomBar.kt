package com.example.nkdsify.ui

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.nkdsify.MyAppState
import com.example.nkdsify.data.Screen

@Composable
fun MyAppBottomBar(
    myAppState: MyAppState,
    isVibrationEnabled: Boolean,
    context: Context
) {
    BottomBar(
        currentScreen = myAppState.currentScreen,
        onScreenChange = { screen ->
            myAppState.currentScreen = screen
        },
        context = context,
        onSettingsClick = { myAppState.currentScreen = Screen.Settings },
        isVibrationEnabled = isVibrationEnabled
    )
}
