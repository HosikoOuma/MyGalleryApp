package com.example.nkdsify.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.nkdsify.ui.utils.TrashRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.io.File
import java.util.Calendar

fun loadAllMedia(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    hiddenFolderIds: Set<String>,
    selectedDate: Long? = null
): ImmutableList<MediaItem> {
    val mediaItems = mutableListOf<MediaItem>()

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
        SortType.ALPHABET -> MediaStore.Files.FileColumns.DISPLAY_NAME
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
            val mediaType = cursor.getInt(mediaTypeColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)

            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            mediaItems.add(MediaItem(uri, name, isVideo, size, dateAdded, dateModified))
        }
    }

    return mediaItems.toImmutableList()
}

fun loadMediaFolders(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    selectedDate: Long? = null
): ImmutableList<MediaFolder> {
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
        SortType.ALPHABET -> MediaStore.Files.FileColumns.DISPLAY_NAME
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
            MediaFolder(id = entry.key, name = folderName, items = itemsInFolder.toImmutableList(), coverUri = coverItem.uri, totalSize = totalSize, dateRange = dateRange, itemCount = itemsInFolder.size)
        } else {
            null
        }
    }.sortedBy { it.name }.toImmutableList()
}

fun loadFavoriteMediaItems(
    context: Context,
    favoriteUris: Set<Uri>,
    sortType: SortType,
    sortAscending: Boolean,
    selectedDate: Long? = null
): ImmutableList<MediaItem> {
    val contentUris = favoriteUris.filter { it.scheme == "content" }
    if (contentUris.isEmpty()) {
        return persistentListOf()
    }

    val favoriteItems = mutableListOf<MediaItem>()
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
        SortType.ALPHABET -> MediaStore.Files.FileColumns.DISPLAY_NAME
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
            val mediaType = cursor.getInt(mediaTypeColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            favoriteItems.add(MediaItem(uri, name, isVideo, size, dateAdded, dateModified))
        }
    }
    return favoriteItems.toImmutableList()
}

fun loadTrashedMediaItems(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean
): ImmutableList<MediaItem> {
    val trashedUris = TrashRepository.getTrashedUris(context)
    val trashedItems = trashedUris.mapNotNull { uri ->
        uri.path?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val name = file.name
                val size = file.length()
                val lastModified = file.lastModified()
                val isVideo = name.endsWith(".mp4", true) ||
                        name.endsWith(".3gp", true) ||
                        name.endsWith(".mkv", true) ||
                        name.endsWith(".webm", true)

                MediaItem(uri, name, isVideo, size, lastModified / 1000, lastModified / 1000)
            } else {
                null
            }
        }
    }.toMutableList()

    val comparator = when (sortType) {
        SortType.DATE_MODIFIED -> compareBy<MediaItem> { it.dateModified }
        SortType.DATE_ADDED -> compareBy<MediaItem> { it.dateAdded }
        SortType.ALPHABET -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        SortType.SIZE -> compareBy<MediaItem> { it.size }
    }

    if (sortAscending) {
        trashedItems.sortWith(comparator)
    } else {
        trashedItems.sortWith(comparator.reversed())
    }

    return trashedItems.toImmutableList()
}
