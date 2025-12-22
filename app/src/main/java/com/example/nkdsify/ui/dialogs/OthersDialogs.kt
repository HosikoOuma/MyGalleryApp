package com.example.nkdsify.ui.dialogs

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.ui.components.UpdateDialog
import com.example.nkdsify.ui.utils.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OthersDialogs(myAppState: MyAppState) {
    val context = LocalContext.current

    if (myAppState.showUpdateDialog && myAppState.latestVersion != null) {
        UpdateDialog(
            onDismiss = { myAppState.showUpdateDialog = false },
            onConfirm = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HosikoOuma/MyGalleryApp/releases/latest"))
                context.startActivity(intent)
                myAppState.showUpdateDialog = false
            },
            onDoNotShowAgain = {
                SettingsRepository.setCheckForUpdatesOnStartup(context, false)
                myAppState.showUpdateDialog = false
            },
            onDownload = {
                myAppState.downloadUrl?.let { url ->
                    downloadAndUpdate(context, url, myAppState.latestVersion!!)
                }
                myAppState.showUpdateDialog = false
            },
            latestVersion = myAppState.latestVersion!!,
            releaseBody = myAppState.releaseBody
        )
    }

    if (myAppState.showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { myAppState.showDatePicker = false }, confirmButton = {
            TextButton(onClick = { myAppState.selectedDate = datePickerState.selectedDateMillis; myAppState.showDatePicker = false }) {
                Text(stringResource(id = R.string.dialog_ok))
            }
        }, dismissButton = {
            TextButton(onClick = { myAppState.showDatePicker = false }) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }) {
            DatePicker(state = datePickerState)
        }
    }

    // NEW: Secret help attention dialog
    if (myAppState.showHelpAttentionDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { myAppState.showHelpAttentionDialog = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Attention!", // Будет заменено на ресурс в след. шаге
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = stringResource(id = R.string.hp_attention),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = { myAppState.showHelpAttentionDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.dialog_ok))
                }
            }
        }
    }
}

private fun downloadAndUpdate(context: Context, url: String, version: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("MyGalleryApp Update")
            .setDescription("Downloading version $version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MyGalleryApp-$version.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
