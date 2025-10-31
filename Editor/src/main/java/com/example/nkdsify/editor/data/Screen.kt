package com.example.nkdsify.editor.data

import android.net.Uri

sealed class Screen {
    data class Edit(val uri: Uri) : Screen()
    data class VideoEdit(val uri: Uri) : Screen()
}
