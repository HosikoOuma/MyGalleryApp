package com.example.nkdsify.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.nkdsify.ui.utils.TrashRepository
import java.util.Calendar

fun loadAllMedia(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    hiddenFolderIds: Set<String>,
    selectedDate: Long? = null
): List<MediaItem> {
    val mediaItems = mutableListOf<MediaItem>()
    val trashedUris = TrashRepository.getTrashedUris(context)

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED
    )

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()

    selectionParts.add("${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)")
    selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
    selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

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
    if (hiddenFolderIds.isNotEmpty()) {
        selectionParts.add("${MediaStore.Files.FileColumns.BUCKET_ID} NOT IN (${hiddenFolderIds.joinToString { "?" }})")
        selectionArgs.addAll(hiddenFolderIds)
    }

    val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(separator = " AND ") else null

    val sortColumn = when (sortType) {
        SortType.DATE_MODIFIED -> MediaStore.Files.FileColumns.DATE_MODIFIED
        SortType.DATE_ADDED -> MediaStore.Files.FileColumns.DATE_ADDED
        SortType.NAME -> MediaStore.Files.FileColumns.DISPLAY_NAME
        SortType.SIZE -> MediaStore.Files.FileColumns.SIZE
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs.toTypedArray(), sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)

            if(uri in trashedUris) continue

            val mediaType = cursor.getInt(mediaTypeColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)

            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            mediaItems.add(MediaItem(uri, name, isVideo, size, dateAdded, dateModified))
        }
    }

    return mediaItems
}

fun loadMediaFolders(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    selectedDate: Long? = null
): List<MediaFolder> {
    val foldersMap = mutableMapOf<Long, MutableList<MediaItem>>()
    val folderNames = mutableMapOf<Long, String>()
    val trashedUris = TrashRepository.getTrashedUris(context)

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED
    )

    val selectionParts = mutableListOf<String>()
    val selectionArgs = mutableListOf<String>()

    selectionParts.add("${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)")
    selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
    selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

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
        SortType.SIZE -> MediaStore.Files.FileColumns.SIZE
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs.toTypedArray(), sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)

            if(uri in trashedUris) continue

            val bucketId = cursor.getLong(bucketIdColumn)
            val bucketName = cursor.getString(bucketNameColumn)
            val mediaType = cursor.getInt(mediaTypeColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)

            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val item = MediaItem(uri, name, isVideo, size, dateAdded, dateModified)

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
        val totalSize = itemsInFolder.sumOf { it.size }
        val dateRange = if (itemsInFolder.isNotEmpty()) {
            val dates = itemsInFolder.map { it.dateModified }
            Pair(dates.minOrNull() ?: 0L, dates.maxOrNull() ?: 0L)
        } else {
            Pair(0L, 0L)
        }
        val coverItem = itemsInFolder.firstOrNull { !it.isVideo } ?: itemsInFolder.firstOrNull()
        if (folderName != null && coverItem != null) {
            MediaFolder(id = entry.key, name = folderName, items = itemsInFolder, coverUri = coverItem.uri, totalSize = totalSize, dateRange = dateRange, itemCount = itemsInFolder.size)
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
    val trashedUris = TrashRepository.getTrashedUris(context)
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED
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
        SortType.SIZE -> MediaStore.Files.FileColumns.SIZE
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs.toTypedArray(), sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)

            if(uri in trashedUris) continue

            val mediaType = cursor.getInt(mediaTypeColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            favoriteItems.add(MediaItem(uri, name, isVideo, size, dateAdded, dateModified))
        }
    }
    return favoriteItems
}

fun loadTrashedMediaItems(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean
): List<MediaItem> {
    val trashedUris = TrashRepository.getTrashedUris(context)
    if (trashedUris.isEmpty()) {
        return emptyList()
    }

    val trashedItems = mutableListOf<MediaItem>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED
    )

    val selection = "${MediaStore.Files.FileColumns._ID} IN (${trashedUris.joinToString { "?" }})"
    val selectionArgs = trashedUris.map { ContentUris.parseId(it).toString() }.toTypedArray()

    val sortColumn = when (sortType) {
        SortType.DATE_MODIFIED -> MediaStore.Files.FileColumns.DATE_MODIFIED
        SortType.DATE_ADDED -> MediaStore.Files.FileColumns.DATE_ADDED
        SortType.NAME -> MediaStore.Files.FileColumns.DISPLAY_NAME
        SortType.SIZE -> MediaStore.Files.FileColumns.SIZE
    }
    val sortDirection = if (sortAscending) "ASC" else "DESC"
    val sortOrder = "$sortColumn $sortDirection"

    context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)

            val mediaType = cursor.getInt(mediaTypeColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)

            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            trashedItems.add(MediaItem(uri, name, isVideo, size, dateAdded, dateModified))
        }
    }
    return trashedItems
}
