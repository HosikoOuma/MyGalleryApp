package com.example.nkdsify.ui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.nkdsify.data.MediaDetails
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface

fun getMediaDetails(context: Context, uri: Uri): MediaDetails? {
    val projection = if (uri.scheme == "content") {
        arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
    } else {
        arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
    }
    try {
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedColumn = if (uri.scheme == "content") cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED) else -1
                val dateModifiedColumn = if (uri.scheme == "content") cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED) else -1
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                val mimeType = cursor.getString(mimeTypeColumn)
                val isVideo = mimeType?.startsWith("video/") ?: false

                var resolution = context.getString(R.string.unknown_resolution)
                try {
                    if (isVideo) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        if (width != null && height != null) {
                            resolution = "$width x $height"
                        }
                        retriever.release()
                    } else { // Get resolution only for images
                        var inputStream: InputStream? = null
                        try {
                            inputStream = context.contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeStream(inputStream, null, options)
                                if (options.outWidth != -1 && options.outHeight != -1) {
                                    resolution = "${options.outWidth} x ${options.outHeight}"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("getMediaDetails", "Failed to get resolution for URI: $uri", e)
                        } finally {
                            inputStream?.close()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("getMediaDetails", "Failed to get resolution for URI: $uri", e)
                }
                val exif = if (!isVideo) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            ExifInterface(inputStream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                MediaDetails(
                    name = cursor.getString(nameColumn),
                    size = cursor.getLong(sizeColumn),
                    dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0,
                    dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) else 0,
                    path = cursor.getString(dataColumn),
                    resolution = resolution,
                    isVideo = isVideo,
                    exif = exif
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e("getMediaDetails", "Failed to get media details for URI: $uri", e)
        return null
    }
}

fun formatFileSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", sizeInBytes / 1024.0.pow(digitGroups), units[digitGroups])
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val instant = Instant.ofEpochSecond(timestamp)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun formatDateRange(startMillis: Long, endMillis: Long): String {
    if (startMillis == 0L || endMillis == 0L) return ""
    val startDate = Instant.ofEpochSecond(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val endDate = Instant.ofEpochSecond(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return if (startDate == endDate) {
        formatter.format(startDate)
    } else {
        "${formatter.format(startDate)} - ${formatter.format(endDate)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaseConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmButtonText: String,
    isDestructive: Boolean = false
) {
    val context = LocalContext.current
    val isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(context)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vibrate = { if (isVibrationEnabled) performVibration(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0) // ИСПРАВЛЕНО
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // ИСПРАВЛЕНО
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
                Button(
                    onClick = {
                        vibrate()
                        onConfirm()
                    },
                    colors = if (isDestructive) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmButtonText)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseConfirmDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.confirm_deletion_title),
        message = stringResource(id = R.string.confirm_deletion_message),
        confirmButtonText = stringResource(id = R.string.delete_button),
        isDestructive = true
    )
}

@Composable
fun ConfirmTrashDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseConfirmDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.confirm_move_to_trash_title),
        message = stringResource(id = R.string.confirm_move_to_trash_message),
        confirmButtonText = stringResource(id = R.string.move_to_trash_button)
    )
}

@Composable
fun ConfirmMoveToSecretDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseConfirmDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.confirm_move_to_secret_title),
        message = stringResource(id = R.string.confirm_move_to_secret_message),
        confirmButtonText = stringResource(id = R.string.confirm_move_to_secret_button)
    )
}

@Composable
fun ConfirmRestoreFromSecretDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseConfirmDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.confirm_restore_from_secret_title),
        message = stringResource(id = R.string.confirm_restore_from_secret_message),
        confirmButtonText = stringResource(id = R.string.restore_button)
    )
}

@Composable
fun ConfirmDeleteFromSecretDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseConfirmDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.confirm_delete_from_secret_title),
        message = stringResource(id = R.string.confirm_delete_from_secret_message),
        confirmButtonText = stringResource(id = R.string.delete_button),
        isDestructive = true
    )
}

@Composable
fun ConfirmRestoreDialog(onConfirm: () -> Unit,
                         onDismiss: () -> Unit
) {
    BaseConfirmDialog(
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.confirm_restore_title),
        message = stringResource(id = R.string.confirm_restore_message),
        confirmButtonText = stringResource(id = R.string.restore_button)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalMediaErrorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(context)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vibrate = { if (isVibrationEnabled) performVibration(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0) // ИСПРАВЛЕНО
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // ИСПРАВЛЕНО
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.external_media_error_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(id = R.string.external_media_error_message),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        vibrate()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsDialog(
    uri: Uri,
    details: MediaDetails,
    onDismiss: () -> Unit,
    onSetAsWallpaper: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onMoveToSecret: () -> Unit,
    onBlur: () -> Unit,
    isBlurred: Boolean,
    onFind: () -> Unit
) {
    val context = LocalContext.current
    val isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(context)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vibrate: () -> Unit = { if (isVibrationEnabled) performVibration(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0) // ИСПРАВЛЕНО
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // ИСПРАВЛЕНО
        ) {
            Text(
                text = stringResource(id = R.string.details_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailItem(label = stringResource(R.string.details_name_label), value = details.name)
                DetailItem(label = stringResource(R.string.details_path_label), value = details.path)
                DetailItem(label = stringResource(R.string.details_size_label), value = formatFileSize(details.size))
                DetailItem(label = stringResource(R.string.details_date_added_label), value = formatTimestamp(details.dateAdded))
                DetailItem(label = stringResource(R.string.details_date_modified_label), value = formatTimestamp(details.dateModified))
                DetailItem(label = stringResource(R.string.details_resolution_label), value = details.resolution)
                if (details.exif != null) {
                    Spacer(Modifier.height(8.dp))
                    ExifData(exif = details.exif)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.actions_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { TextButton(onClick = { vibrate(); onSetAsWallpaper() }) { 
                    Icon(Icons.Default.Wallpaper, contentDescription = stringResource(id = R.string.set_as_wallpaper_content_description))
                } }
                item { TextButton(onClick = { vibrate(); onBlur() }) { 
                    Icon(if (isBlurred) Icons.Default.BlurOff else Icons.Default.BlurOn, contentDescription = null)
                } }
                item { TextButton(onClick = { vibrate(); onCopy() }) { 
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_content_description))
                } }
                item { TextButton(onClick = { vibrate(); onMove() }) { 
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(id = R.string.move_content_description))
                } }
                item { TextButton(onClick = { vibrate(); onRename() }) { 
                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.rename_content_description))
                } }
                item { TextButton(onClick = { vibrate(); onMoveToSecret() }) { 
                    Icon(Icons.Default.Lock, contentDescription = stringResource(id = R.string.move_to_secret_storage_content_description))
                } }
                 item { TextButton(onClick = { vibrate(); onFind() }) { 
                    Icon(Icons.Outlined.FindInPage, contentDescription = stringResource(id = R.string.find_in_page_content_description))
                } }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { vibrate(); onDismiss() }) {
                    Text(stringResource(id = R.string.dialog_close))
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ExifData(exif: ExifInterface) {
    val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)

    // Use getAttributeDouble which can handle rational values.
    val aperture = exif.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.0)
    val shutterSpeed = exif.getAttributeDouble(ExifInterface.TAG_SHUTTER_SPEED_VALUE, 0.0)
    val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED)
    val focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)

    if (!cameraModel.isNullOrBlank()) {
        Text("Camera: $cameraModel")
    }

    // The value is an APEX value. F-number = 2^(aperture_apex / 2)
    if (aperture > 0.0) {
        val fStop = 2.0.pow(aperture / 2.0)
        Text("Aperture: f/${String.format("%.1f", fStop)}")
    }

    // The value is an APEX value. Exposure time = 1 / (2^shutter_speed_apex)
    if (shutterSpeed > 0.0) {
        val exposureTime = 1.0 / 2.0.pow(shutterSpeed)
        if (exposureTime < 1.0) {
            Text("Shutter speed: 1/${(1.0 / exposureTime).roundToInt()}s")
        } else {
            Text("Shutter speed: ${String.format("%.1f", exposureTime)}s")
        }
    }

    if (!iso.isNullOrBlank()) {
        Text("ISO: $iso")
    }

    if (focalLength > 0.0) {
        Text("Focal length: ${focalLength.roundToInt()} mm")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val context = LocalContext.current
    val isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(context)) }
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = R.string.rename_dialog_title))
        },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onRename(newName)
                },
                enabled = newName.isNotBlank()
            ) {
                Text(stringResource(id = R.string.rename_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}

fun renameMedia(context: Context, uri: Uri, newName: String) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName)
        }
        context.contentResolver.update(uri, contentValues, null, null)
    } catch (e: Exception) {
        Log.e("renameMedia", "Failed to rename media: $uri", e)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDetailsDialog(
    details: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0) // ИСПРАВЛЕНО
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // ИСПРАВЛЕНО
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.details_content_description),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(id = R.string.dialog_ok))
                }
            }
        }
    }
}