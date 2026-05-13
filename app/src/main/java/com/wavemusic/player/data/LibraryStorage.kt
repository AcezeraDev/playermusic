package com.wavemusic.player.data

import android.content.Context
import android.net.Uri

class LibraryStorage(context: Context) {
    private val prefs = context.getSharedPreferences("wave_music_library", Context.MODE_PRIVATE)

    fun loadLikedIds(): Set<Long> {
        return prefs.getStringSet(KEY_LIKED_IDS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    fun saveLikedIds(ids: Set<Long>) {
        prefs.edit()
            .putStringSet(KEY_LIKED_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    fun loadPlaylists(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, "").orEmpty()
        if (raw.isBlank()) return emptyList()

        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 3) return@mapNotNull null

                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val name = Uri.decode(parts[1]).orEmpty().ifBlank { "Playlist" }
                val songIds = parts[2]
                    .split(",")
                    .mapNotNull { it.toLongOrNull() }

                Playlist(id = id, name = name, songIds = songIds)
            }
            .toList()
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val raw = playlists.joinToString(separator = "\n") { playlist ->
            val encodedName = Uri.encode(playlist.name)
            val ids = playlist.songIds.joinToString(separator = ",")
            "${playlist.id}|$encodedName|$ids"
        }

        prefs.edit()
            .putString(KEY_PLAYLISTS, raw)
            .apply()
    }

    private companion object {
        const val KEY_LIKED_IDS = "liked_ids"
        const val KEY_PLAYLISTS = "playlists"
    }
}
