package com.example.nkdsify.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nkdsify.R

@Composable
fun EasterEggDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.easter_egg_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.avatar),
                    contentDescription = stringResource(id = R.string.avatar_content_description),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.easter_egg_author), fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.close_button))
            }
        }
    )
}
