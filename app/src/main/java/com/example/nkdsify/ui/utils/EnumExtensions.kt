package com.example.nkdsify.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.R
import com.example.nkdsify.data.AppFontFamily
import com.example.nkdsify.data.BlurType
import com.example.nkdsify.data.FabAction
import com.example.nkdsify.data.Language
import com.example.nkdsify.data.Theme
import com.example.nkdsify.data.ZoomType

@Composable
fun Theme.getDisplayName(): String {
    return when (this) {
        Theme.SYSTEM -> stringResource(id = R.string.theme_system)
        Theme.LIGHT -> stringResource(id = R.string.theme_light)
        Theme.DARK -> stringResource(id = R.string.theme_dark)
        Theme.AMOLED -> stringResource(id = R.string.theme_amoled)
    }
}

@Composable
fun FabAction.getDisplayName(): String {
    return when (this) {
        FabAction.SHUFFLE -> stringResource(id = R.string.fab_action_shuffle)
        FabAction.CAMERA -> stringResource(id = R.string.fab_action_camera)
    }
}

@Composable
fun AppFontFamily.getDisplayName(): String {
    return when (this) {
        AppFontFamily.SYSTEM -> stringResource(R.string.font_family_default)
        AppFontFamily.JETBRAINS_MONO -> stringResource(R.string.font_family_jetbrains_mono)
        AppFontFamily.GOOGLE_SANS -> stringResource(R.string.font_family_google_sans)
    }
}

@Composable
fun ZoomType.getDisplayName(): String {
    return when (this) {
        ZoomType.PINCH -> stringResource(id = R.string.zoom_type_pinch)
        ZoomType.DOUBLE_TAP -> stringResource(id = R.string.zoom_type_double_tap)
    }
}

@Composable
fun BlurType.getDisplayName(): String {
    return when (this) {
        BlurType.BLUR -> stringResource(id = R.string.blur_type_blur)
        BlurType.PLACEHOLDER -> stringResource(id = R.string.blur_type_placeholder)
    }
}

@Composable
fun Language.getDisplayName(): String {
    return when (this) {
        Language.SYSTEM -> stringResource(id = R.string.language_system)
        Language.ENGLISH -> stringResource(id = R.string.language_english)
        Language.RUSSIAN -> stringResource(id = R.string.language_russian)
        Language.SPECIAL -> stringResource(id = R.string.language_special)
    }
}
