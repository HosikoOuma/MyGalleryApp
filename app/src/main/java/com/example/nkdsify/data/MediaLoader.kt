package com.example.nkdsify.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.util.Calendar

fun loadMediaFolders(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    selectedDate: Long? = null
): List<MediaFolder> {
    val foldersMap = mutableMapOf<Long, MutableList<MediaItem>>()
    val folderNames = mutableMapOf<Long, String>()

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()

    selectedDate?.let {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = it
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis / 1000
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis / 1000

        selectionParts.add("${MediaStore.Files.FileColumns.DATE_ADDED} BETWEEN ? AND ?")
        selectionArgs.add(startOfDay.toString())
        selectionArgs.add(endOfDay.toString())
    }

    val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(separator = " AND ") else null

    val sortColumn = when (sortType) {
        SortType.DATE_MODIFIED -> MediaStore.Files.FileColumns.DATE_MODIFIED
        SortType.DATE_ADDED -> MediaStore.Files.FileColumns.DATE_ADDED
        SortType.NAME -> MediaStore.Files.FileColumns.DISPLAY_NAME
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs.toTypedArray(), sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val bucketId = cursor.getLong(bucketIdColumn)
            val bucketName = cursor.getString(bucketNameColumn)
            val mediaType = cursor.getInt(mediaTypeColumn)

            val uri = ContentUris.withAppendedId(collection, id)
            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val item = MediaItem(uri, isVideo)

            if (!foldersMap.containsKey(bucketId)) {
                foldersMap[bucketId] = mutableListOf()
                folderNames[bucketId] = bucketName
            }
            foldersMap[bucketId]?.add(item)
        }
    }

    return foldersMap.mapNotNull { entry ->
        val folderName = folderNames[entry.key]
        val itemsInFolder = entry.value
        val coverItem = itemsInFolder.firstOrNull { !it.isVideo } ?: itemsInFolder.firstOrNull()
        if (folderName != null && coverItem != null) {
            MediaFolder(id = entry.key, name = folderName, coverUri = coverItem.uri, items = itemsInFolder)
        } else {
            null
        }
    }.sortedBy { it.name }
}

fun loadFavoriteMediaItems(
    context: Context,
    favoriteUris: Set<Uri>,
    sortType: SortType,
    sortAscending: Boolean,
    selectedDate: Long? = null
): List<MediaItem> {
    val contentUris = favoriteUris.filter { it.scheme == "content" }
    if (contentUris.isEmpty()) {
        return emptyList()
    }

    val favoriteItems = mutableListOf<MediaItem>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE
    )

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()

    selectionParts.add("${MediaStore.Files.FileColumns._ID} IN (${contentUris.joinToString { "?" }})")
    selectionArgs.addAll(contentUris.map { ContentUris.parseId(it).toString() })

    selectedDate?.let {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = it
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis / 1000
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis / 1000

        selectionParts.add("${MediaStore.Files.FileColumns.DATE_ADDED} BETWEEN ? AND ?")
        selectionArgs.add(startOfDay.toString())
        selectionArgs.add(endOfDay.toString())
    }

    val selection = selectionParts.joinToString(separator = " AND ")

    val sortColumn = when (sortType) {
        SortType.DATE_MODIFIED -> MediaStore.Files.FileColumns.DATE_MODIFIED
        SortType.DATE_ADDED -> MediaStore.Files.FileColumns.DATE_ADDED
        SortType.NAME -> MediaStore.Files.FileColumns.DISPLAY_NAME
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs.toTypedArray(), sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val mediaType = cursor.getInt(mediaTypeColumn)
            val uri = ContentUris.withAppendedId(collection, id)
            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            favoriteItems.add(MediaItem(uri, isVideo))
        }
    }
    return favoriteItems
}
