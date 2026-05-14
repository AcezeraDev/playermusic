package com.wavemusic.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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

        return if (raw.trimStart().startsWith("[")) {
            parsePlaylistsJson(raw)
        } else {
            parseLegacyPlaylists(raw).also { playlists ->
                if (playlists.isNotEmpty()) savePlaylists(playlists)
            }
        }
    }

    private fun parseLegacyPlaylists(raw: String): List<Playlist> {
        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 3) return@mapNotNull null

                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val name = android.net.Uri.decode(parts[1]).orEmpty().ifBlank { "Playlist" }
                val songIds = parts[2]
                    .split(",")
                    .mapNotNull { it.toLongOrNull() }

                Playlist(id = id, name = name, songIds = songIds, createdAt = id)
            }
            .toList()
    }

    private fun parsePlaylistsJson(raw: String): List<Playlist> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(item.toPlaylist(System.currentTimeMillis() + index))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val raw = JSONArray().apply {
            playlists.forEach { playlist ->
                put(playlist.toJson())
            }
        }

        prefs.edit()
            .putString(KEY_PLAYLISTS, raw.toString())
            .apply()
    }

    fun loadQueueIds(): List<Long> {
        return loadLongList(KEY_QUEUE)
    }

    fun saveQueueIds(ids: List<Long>) {
        saveLongList(KEY_QUEUE, ids)
    }

    fun loadRecentIds(): List<Long> {
        return loadLongList(KEY_RECENTS)
    }

    fun saveRecentIds(ids: List<Long>) {
        saveLongList(KEY_RECENTS, ids.take(MAX_RECENTS))
    }

    fun loadPlaybackStats(): PlaybackStats {
        val playCounts = prefs.getString(KEY_PLAY_COUNTS, "").orEmpty()
            .lineSequence()
            .mapNotNull { row ->
                val parts = row.split(":")
                if (parts.size != 2) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                val count = parts[1].toIntOrNull() ?: return@mapNotNull null
                id to count
            }
            .toMap()

        return PlaybackStats(
            playCounts = playCounts,
            totalListenMs = prefs.getLong(KEY_TOTAL_LISTEN_MS, 0L)
        )
    }

    fun savePlaybackStats(stats: PlaybackStats) {
        val counts = stats.playCounts.entries.joinToString(separator = "\n") { (id, count) ->
            "$id:$count"
        }

        prefs.edit()
            .putString(KEY_PLAY_COUNTS, counts)
            .putLong(KEY_TOTAL_LISTEN_MS, stats.totalListenMs)
            .apply()
    }

    fun exportBackupJson(
        likedIds: Set<Long>,
        playlists: List<Playlist>,
        queueIds: List<Long>,
        stats: PlaybackStats
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("likedIds", JSONArray(likedIds.toList()))
        root.put("queueIds", JSONArray(queueIds))
        root.put("totalListenMs", stats.totalListenMs)

        val playlistsJson = JSONArray()
        playlists.forEach { playlist ->
            playlistsJson.put(
                playlist.toJson()
            )
        }
        root.put("playlists", playlistsJson)

        val countsJson = JSONObject()
        stats.playCounts.forEach { (id, count) ->
            countsJson.put(id.toString(), count)
        }
        root.put("playCounts", countsJson)

        return root.toString(2)
    }

    fun importBackupJson(rawJson: String): LibraryBackup? {
        return runCatching {
            val root = JSONObject(rawJson)
            val likedIds = root.optJSONArray("likedIds").toLongSet()
            val queueIds = root.optJSONArray("queueIds").toLongList()

            val playlists = buildList {
                val array = root.optJSONArray("playlists") ?: JSONArray()
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        item.toPlaylist(System.currentTimeMillis() + index)
                    )
                }
            }

            val counts = mutableMapOf<Long, Int>()
            val countsJson = root.optJSONObject("playCounts") ?: JSONObject()
            countsJson.keys().forEach { key ->
                key.toLongOrNull()?.let { id ->
                    counts[id] = countsJson.optInt(key, 0)
                }
            }

            LibraryBackup(
                likedIds = likedIds,
                playlists = playlists,
                queueIds = queueIds,
                playCounts = counts,
                totalListenMs = root.optLong("totalListenMs", 0L)
            )
        }.getOrNull()
    }

    private fun loadLongList(key: String): List<Long> {
        return prefs.getString(key, "").orEmpty()
            .split(",")
            .mapNotNull { it.toLongOrNull() }
    }

    private fun saveLongList(key: String, ids: List<Long>) {
        prefs.edit()
            .putString(key, ids.joinToString(separator = ","))
            .apply()
    }

    private fun JSONArray?.toLongList(): List<Long> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                add(optLong(index))
            }
        }
    }

    private fun JSONArray?.toLongSet(): Set<Long> {
        return toLongList().toSet()
    }

    private fun Playlist.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("description", description)
            .put("imageUri", imageUri ?: "")
            .put("createdAt", createdAt)
            .put("songIds", JSONArray(songIds))
    }

    private fun JSONObject.toPlaylist(fallbackId: Long): Playlist {
        val id = optLong("id", fallbackId)
        return Playlist(
            id = id,
            name = optString("name", "Playlist").ifBlank { "Playlist" },
            songIds = optJSONArray("songIds").toLongList(),
            description = optString("description", ""),
            imageUri = optString("imageUri", "").ifBlank { null },
            createdAt = optLong("createdAt", id)
        )
    }

    private companion object {
        const val KEY_LIKED_IDS = "liked_ids"
        const val KEY_PLAYLISTS = "playlists"
        const val KEY_QUEUE = "queue"
        const val KEY_RECENTS = "recents"
        const val KEY_PLAY_COUNTS = "play_counts"
        const val KEY_TOTAL_LISTEN_MS = "total_listen_ms"
        const val MAX_RECENTS = 30
    }
}
