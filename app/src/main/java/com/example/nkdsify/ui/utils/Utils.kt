package com.example.nkdsify.ui.utils

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.nkdsify.R
import com.example.nkdsify.data.MediaDetails
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

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

                MediaDetails(
                    name = cursor.getString(nameColumn),
                    size = cursor.getLong(sizeColumn),
                    dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0,
                    dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) else 0,
                    path = cursor.getString(dataColumn),
                    resolution = resolution,
                    isVideo = isVideo
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
@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.confirm_deletion_title)) },
        text = { Text(stringResource(id = R.string.confirm_deletion_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(id = R.string.delete_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun ConfirmTrashDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.confirm_move_to_trash_title)) },
        text = { Text(stringResource(id = R.string.confirm_move_to_trash_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
            ) {
                Text(stringResource(id = R.string.move_to_trash_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun ConfirmRestoreDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.confirm_restore_title)) },
        text = { Text(stringResource(id = R.string.confirm_restore_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
            ) {
                Text(stringResource(id = R.string.restore_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun ExternalMediaErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.external_media_error_title)) },
        text = { Text(stringResource(id = R.string.external_media_error_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_ok))
            }
        }
    )
}

@Composable
fun MediaDetailsDialog(
    details: MediaDetails,
    onDismiss: () -> Unit,
    onSetAsWallpaper: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit
) {
    val context = LocalContext.current
    val isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.details_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(id = R.string.details_name, details.name))
                Text(stringResource(id = R.string.details_size, formatFileSize(details.size)))
                Text(stringResource(id = R.string.details_date_added, formatTimestamp(details.dateAdded)))
                Text(stringResource(id = R.string.details_date_modified, formatTimestamp(details.dateModified)))
                Text(stringResource(id = R.string.details_path, details.path))
                Text(stringResource(id = R.string.details_resolution, details.resolution))
            }
        },
        confirmButton = {
            Row {
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onSetAsWallpaper()
                }) {
                    Icon(Icons.Default.Wallpaper, contentDescription = stringResource(id = R.string.set_as_wallpaper_content_description))
                }
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onCopy()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_content_description))
                }
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onMove()
                }) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(id = R.string.move_content_description))
                }
                IconButton(onClick = {
                    if (isVibrationEnabled) performVibration(context)
                    onRename()
                }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.rename_content_description))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isVibrationEnabled) performVibration(context)
                onDismiss()
            }) {
                Text(stringResource(id = R.string.dialog_ok))
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.rename_dialog_title)) },
        text = {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onRename(newName) }) {
                Text(stringResource(id = R.string.rename_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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

object FavoritesRepository {
    private const val PREFS_NAME = "MyGalleryAppPrefs"
    private const val FAVORITES_KEY = "favorites"

    private fun getSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): Set<String> = getSharedPreferences(context).getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()

    fun saveFavorites(context: Context, favorites: Set<String>) {
        getSharedPreferences(context).edit { putStringSet(FAVORITES_KEY, favorites) }
    }
}
