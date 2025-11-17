package com.example.nkdsify.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.MyAppState
import com.example.nkdsify.R
import com.example.nkdsify.ui.components.EasterEggDialog
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
                    // downloadAndUpdate(context, url, myAppState.latestVersion!!)
                }
                myAppState.showUpdateDialog = false
            },
            latestVersion = myAppState.latestVersion!!
        )
    }

    if (myAppState.showEasterEggDialog) {
        EasterEggDialog(onDismiss = { myAppState.showEasterEggDialog = false })
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
}
