package com.wavemusic.player.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object MusicStore {
    fun loadDeviceSongs(context: Context): List<Music> {
        val songs = mutableListOf<Music>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn).orEmpty().ifBlank { "Música sem título" }
                val artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Artista desconhecido" }
                val album = cursor.getString(albumColumn).orEmpty().ifBlank { "Álbum desconhecido" }
                val duration = cursor.getLong(durationColumn).coerceAtLeast(0)
                val uri = ContentUris.withAppendedId(collection, id)

                songs += Music(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    uri = uri
                )
            }
        }

        return songs
    }
}
