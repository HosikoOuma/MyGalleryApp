package com.example.nkdsify.ui.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.nkdsify.data.MediaFolder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utilities to detect and filter-out "phantom" media items — entries with size == 0 and dateModified == 0
 * that remain in the MediaStore after failed/cancelled copy/move operations.
 *
 * These helpers run verification using filesystem checks (for file:// URIs) or querying
 * ContentResolver for size/date_modified fields.
 */

suspend fun sanitizeFolders(folders: List<MediaFolder>, context: Context): List<MediaFolder> {
    return withContext(Dispatchers.IO) {
        folders.map { folder ->
            val filteredItems = folder.items.filterNot { item ->
                isPhantomMedia(context, item.uri, item.size, item.dateModified)
            }
            folder.copy(items = filteredItems.toImmutableList())
        }
    }
}

fun isPhantomMedia(context: Context, uri: Uri, storedSize: Long, storedDateModified: Long): Boolean {
    // Quick fast-path: if stored metadata shows content, it's not phantom
    if (storedSize > 0 || storedDateModified > 0) return false

    try {
        if (uri.scheme == "file") {
            val path = uri.path ?: return false
            val f = java.io.File(path)
            return !(f.exists() && f.length() > 0)
        } else {
            // Query content resolver for SIZE or DATE_MODIFIED
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val realSize = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                    val realDate = if (dateIndex != -1) cursor.getLong(dateIndex) else 0L
                    return !(realSize > 0 || realDate > 0)
                }
            }
        }
    } catch (e: Exception) {
        // Conservatively return false (not phantom) if verification fails
        return false
    }

    // If all checks passed and found no evidence of data -> treat as phantom
    return true
}

