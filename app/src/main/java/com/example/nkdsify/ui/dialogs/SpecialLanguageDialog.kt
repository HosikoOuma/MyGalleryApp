package com.example.nkdsify.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R
import com.example.nkdsify.data.Language
import com.example.nkdsify.ui.utils.SettingsRepository
import com.example.nkdsify.ui.components.utils.lexapro


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialLanguageDialog(
    onDismissRequest: () -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpecialLanguageUnlocked: () -> Unit,
    vibrate: () -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.enter_special_language_code_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(id = R.string.special_language_code_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        vibrate()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
                Button(onClick = {
                    if (lexapro(input)) {
                        SettingsRepository.setSpecialLanguageUnlocked(context, true)
                        onSpecialLanguageUnlocked()
                        onLanguageChange(Language.SPECIAL)
                    }
                    onDismissRequest()
                    vibrate()
                }) {
                    Text(stringResource(id = R.string.activate_button))
                }
            }
        }
    }
}


