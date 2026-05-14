package com.wavemusic.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.wavemusic.player.data.model.Music

object MusicStore {
    fun loadDeviceSongs(context: Context): List<Music> {
        return (loadAudio(context) + loadVideos(context))
            .sortedWith(
                compareByDescending<Music> { it.dateAddedSeconds }
                    .thenByDescending { it.id }
            )
    }

    private fun loadAudio(context: Context): List<Music> {
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

        val songs = mutableListOf<Music>()
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
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
                songs += Music(
                    id = id,
                    title = cursor.getString(titleColumn).orEmpty().ifBlank { "Musica sem titulo" },
                    artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Artista desconhecido" },
                    album = cursor.getString(albumColumn).orEmpty().ifBlank { "Album desconhecido" },
                    durationMs = cursor.getLong(durationColumn).coerceAtLeast(0),
                    uri = ContentUris.withAppendedId(collection, id),
                    folder = cursor.getString(folderColumn).orEmpty().toFolderLabel("Musicas"),
                    dateAddedSeconds = cursor.getLong(dateAddedColumn).coerceAtLeast(0),
                    sizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0),
                    mimeType = cursor.getString(mimeTypeColumn).orEmpty().ifBlank { "audio/*" }
                )
            }
        }
        return songs
    }

    private fun loadVideos(context: Context): List<Music> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Video.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            projection += MediaStore.Video.Media.DATA
        }

        val videos = mutableListOf<Music>()
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val folderColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                val folder = cursor.getString(folderColumn).orEmpty().toFolderLabel("Videos")
                val rawTitle = cursor.getString(titleColumn).orEmpty()
                    .ifBlank { cursor.getString(displayNameColumn).orEmpty() }
                val title = rawTitle
                    .substringBeforeLast('.', missingDelimiterValue = rawTitle)
                    .ifBlank { "Video sem titulo" }

                videos += Music(
                    id = -mediaStoreId,
                    title = title,
                    artist = folder,
                    album = "Videos",
                    durationMs = cursor.getLong(durationColumn).coerceAtLeast(0),
                    uri = ContentUris.withAppendedId(collection, mediaStoreId),
                    folder = folder,
                    dateAddedSeconds = cursor.getLong(dateAddedColumn).coerceAtLeast(0),
                    sizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0),
                    mimeType = cursor.getString(mimeTypeColumn).orEmpty().ifBlank { "video/*" }
                )
            }
        }
        return videos
    }

    private fun String.toFolderLabel(default: String): String {
        return trim('/')
            .substringAfterLast('/', default)
            .ifBlank { default }
    }
}

