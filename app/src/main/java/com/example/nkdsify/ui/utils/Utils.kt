package com.example.nkdsify.ui.utils

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.nkdsify.data.MediaDetails
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getMediaDetails(context: Context, uri: Uri): MediaDetails? {
    val projection = if (uri.scheme == "content") {
        arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )
    } else {
        arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA
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

                var resolution = "Unknown"
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

                MediaDetails(
                    name = cursor.getString(nameColumn),
                    size = cursor.getLong(sizeColumn),
                    dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0,
                    dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) else 0,
                    path = cursor.getString(dataColumn),
                    resolution = resolution
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
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val instant = Instant.ofEpochSecond(timestamp)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to permanently delete these items? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExternalMediaErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Action Not Allowed") },
        text = { Text("This action is not allowed for media outside of this application.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

fun deleteMediaPermanently(context: Context, uris: List<Uri>): IntentSender? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val mediaStoreUris = uris.mapNotNull { uri ->
            try {
                val id = ContentUris.parseId(uri)
                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE),
                    "${MediaStore.Files.FileColumns._ID} = ?",
                    arrayOf(id.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val mediaType = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE))
                        when (mediaType) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            else -> null
                        }
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
        if (mediaStoreUris.isNotEmpty()) {
            return MediaStore.createDeleteRequest(context.contentResolver, mediaStoreUris).intentSender
        }
    } else {
        try {
            uris.forEach { uri ->
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: RecoverableSecurityException) {
            return e.userAction.actionIntent.intentSender
        }
    }
    return null
}

@Composable
fun MediaDetailsDialog(details: MediaDetails, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name: ${details.name}")
                Text("Size: ${formatFileSize(details.size)}")
                Text("Date Added: ${formatTimestamp(details.dateAdded)}")
                Text("Date Modified: ${formatTimestamp(details.dateModified)}")
                Text("Path: ${details.path}")
                Text("Resolution: ${details.resolution}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
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
