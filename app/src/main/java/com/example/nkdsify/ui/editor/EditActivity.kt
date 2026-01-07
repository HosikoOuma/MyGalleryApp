package com.example.nkdsify.ui.editor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.nkdsify.ContextUtils
import com.example.nkdsify.ui.theme.NkdsifyAppTheme

class EditActivity : ComponentActivity() {
    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ContextUtils.updateLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uri = intent.data ?: intent.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)
        uri?.let { viewModel.loadBitmap(this, it) }

        setContent {
            NkdsifyAppTheme {
                PhotoEditorScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}
