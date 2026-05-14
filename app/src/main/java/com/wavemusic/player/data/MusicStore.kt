package com.wavemusic.player.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

object MusicStore {
    fun loadDeviceSongs(context: Context): List<Music> {
        val songs = mutableListOf<Music>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            projection += MediaStore.Audio.Media.DATA
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val folderColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn).orEmpty().ifBlank { "Música sem título" }
                val artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Artista desconhecido" }
                val album = cursor.getString(albumColumn).orEmpty().ifBlank { "Álbum desconhecido" }
                val duration = cursor.getLong(durationColumn).coerceAtLeast(0)
                val dateAdded = cursor.getLong(dateAddedColumn).coerceAtLeast(0)
                val size = cursor.getLong(sizeColumn).coerceAtLeast(0)
                val mimeType = cursor.getString(mimeTypeColumn).orEmpty().ifBlank { "audio/*" }
                val rawFolder = cursor.getString(folderColumn).orEmpty()
                val folder = rawFolder
                    .trim('/')
                    .substringAfterLast('/', "Músicas")
                    .ifBlank { "Músicas" }
                val uri = ContentUris.withAppendedId(collection, id)

                songs += Music(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    uri = uri,
                    folder = folder,
                    dateAddedSeconds = dateAdded,
                    sizeBytes = size,
                    mimeType = mimeType
                )
            }
        }

        return songs
    }
}
