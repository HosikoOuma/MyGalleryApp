package com.example.nkdsify.ui

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.data.Screen
import com.example.nkdsify.ui.utils.BiometricUtils
import com.example.nkdsify.ui.utils.performVibration

@Composable
fun MyAppBottomBar(
    myAppState: MyAppState,
    context: Context,
    isVibrationEnabled: Boolean,
    isVisible: Boolean, // Теперь получаем видимость извне для синхронизации
    modifier: Modifier = Modifier
) {
    var lastTap by rememberSaveable { mutableLongStateOf(0L) }
    var tapCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(myAppState.currentScreen) {
        tapCount = 0
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it * 2 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it * 2 },
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ),
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(100),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarBubbleItem(
                    selected = myAppState.currentScreen is Screen.Settings,
                    onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Settings
                    },
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(id = R.string.settings_content_description)
                )
                NavBarBubbleItem(
                    selected = myAppState.currentScreen is Screen.Trash,
                    onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Trash
                    },
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.trash_content_description)
                )
                NavBarBubbleItem(
                    selected = myAppState.currentScreen is Screen.Folders || myAppState.currentScreen is Screen.FolderContent,
                    onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Folders
                        val now = System.currentTimeMillis()
                        if (now - myAppState.lastFoldersTapTime < 500) {
                            myAppState.foldersTapCount++
                        } else {
                            myAppState.foldersTapCount = 1
                        }
                        myAppState.lastFoldersTapTime = now

                        if (myAppState.foldersTapCount == 10) {
                            myAppState.foldersTapCount = 0
                            BiometricUtils.authenticate(
                                activity = context as AppCompatActivity,
                                onSuccess = { myAppState.revelationModeEnabled = true },
                                onError = { _, _ -> /* Do nothing */ },
                                onFailed = { /* Do nothing */ }
                            )
                        }
                    },
                    icon = Icons.Filled.PhotoLibrary,
                    contentDescription = stringResource(id = R.string.folders_content_description)
                )
                NavBarBubbleItem(
                    selected = myAppState.currentScreen is Screen.AllMedia,
                    onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.AllMedia
                    },
                    icon = Icons.Default.PermMedia,
                    contentDescription = stringResource(id = R.string.all_media_content_description)
                )
                NavBarBubbleItem(
                    selected = myAppState.currentScreen is Screen.Favorites,
                    onClick = {
                        if (isVibrationEnabled) performVibration(context)
                        myAppState.currentScreen = Screen.Favorites()
                        val now = System.currentTimeMillis()
                        if (now - lastTap < 500) {
                            tapCount++
                        } else {
                            tapCount = 1
                        }
                        lastTap = now

                        if (tapCount == 10) {
                            if (isVibrationEnabled) performVibration(context)
                            tapCount = 0
                            Toast.makeText(context, context.getString(R.string.uwu_toast), Toast.LENGTH_SHORT).show()
                            val mediaPlayer = MediaPlayer.create(context, R.raw.uwu)
                            mediaPlayer.setOnCompletionListener { it.release() }
                            mediaPlayer.start()
                        }
                    },
                    icon = Icons.Filled.Favorite,
                    contentDescription = stringResource(id = R.string.favorites_content_description)
                )
            }
        }
    }
}

@Composable
fun NavBarBubbleItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (selected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val iconColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
