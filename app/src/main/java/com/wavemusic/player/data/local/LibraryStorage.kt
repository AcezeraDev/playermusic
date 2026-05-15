package com.wavemusic.player.data.local

import android.content.Context
import com.wavemusic.player.data.model.LibraryBackup
import com.wavemusic.player.data.model.LocalTagOverride
import com.wavemusic.player.data.model.PlaybackStats
import com.wavemusic.player.data.model.Playlist
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

    fun loadResumePositions(): Map<Long, Long> {
        val raw = prefs.getString(KEY_RESUME_POSITIONS, "").orEmpty()
        if (raw.isBlank()) return emptyMap()

        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val id = key.toLongOrNull() ?: return@forEach
                    val position = json.optLong(key, 0L)
                    if (position >= RESUME_MIN_POSITION_MS) put(id, position)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun loadResumePosition(id: Long): Long {
        return loadResumePositions()[id] ?: 0L
    }

    fun saveResumePosition(id: Long, positionMs: Long, durationMs: Long) {
        val positions = loadResumePositions().toMutableMap()
        val cleanPosition = positionMs.coerceAtLeast(0L)
        val almostFinished = durationMs > 0L && durationMs - cleanPosition <= RESUME_END_THRESHOLD_MS

        if (cleanPosition < RESUME_MIN_POSITION_MS || almostFinished) {
            positions.remove(id)
        } else {
            positions[id] = cleanPosition
        }

        saveResumePositions(positions)
    }

    fun replaceResumePositions(positions: Map<Long, Long>) {
        saveResumePositions(positions.filterValues { it >= RESUME_MIN_POSITION_MS })
    }

    fun loadLyrics(id: Long): String {
        return prefs.getString(KEY_LYRICS_PREFIX + id, "").orEmpty()
    }

    fun saveLyrics(id: Long, lyrics: String) {
        val editor = prefs.edit()
        if (lyrics.isBlank()) {
            editor.remove(KEY_LYRICS_PREFIX + id)
        } else {
            editor.putString(KEY_LYRICS_PREFIX + id, lyrics.trim())
        }
        editor.apply()
    }

    fun loadTagOverrides(): Map<Long, LocalTagOverride> {
        val raw = prefs.getString(KEY_TAG_OVERRIDES, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val id = key.toLongOrNull() ?: return@forEach
                    val item = json.optJSONObject(key) ?: return@forEach
                    put(
                        id,
                        LocalTagOverride(
                            title = item.optString("title", ""),
                            artist = item.optString("artist", ""),
                            album = item.optString("album", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveTagOverride(id: Long, override: LocalTagOverride) {
        val overrides = loadTagOverrides().toMutableMap()
        overrides[id] = override
        saveTagOverrides(overrides)
    }

    fun clearTagOverride(id: Long) {
        val overrides = loadTagOverrides().toMutableMap()
        overrides.remove(id)
        saveTagOverrides(overrides)
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

    private fun saveResumePositions(positions: Map<Long, Long>) {
        val sorted = positions.entries
            .sortedByDescending { it.value }
            .take(MAX_RESUME_POSITIONS)

        val json = JSONObject()
        sorted.forEach { (id, position) ->
            json.put(id.toString(), position)
        }

        prefs.edit()
            .putString(KEY_RESUME_POSITIONS, json.toString())
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

        val resumeJson = JSONObject()
        loadResumePositions().forEach { (id, position) ->
            resumeJson.put(id.toString(), position)
        }
        root.put("resumePositions", resumeJson)

        val lyricsJson = JSONObject()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_LYRICS_PREFIX) && value is String && value.isNotBlank()) {
                key.removePrefix(KEY_LYRICS_PREFIX).toLongOrNull()?.let { id ->
                    lyricsJson.put(id.toString(), value)
                }
            }
        }
        root.put("lyrics", lyricsJson)

        val tagOverridesJson = JSONObject()
        loadTagOverrides().forEach { (id, override) ->
            tagOverridesJson.put(
                id.toString(),
                JSONObject()
                    .put("title", override.title)
                    .put("artist", override.artist)
                    .put("album", override.album)
            )
        }
        root.put("tagOverrides", tagOverridesJson)

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

            val resumePositions = mutableMapOf<Long, Long>()
            val resumeJson = root.optJSONObject("resumePositions") ?: JSONObject()
            resumeJson.keys().forEach { key ->
                key.toLongOrNull()?.let { id ->
                    val position = resumeJson.optLong(key, 0L)
                    if (position >= RESUME_MIN_POSITION_MS) resumePositions[id] = position
                }
            }

            val lyrics = mutableMapOf<Long, String>()
            val lyricsJson = root.optJSONObject("lyrics") ?: JSONObject()
            lyricsJson.keys().forEach { key ->
                key.toLongOrNull()?.let { id ->
                    val text = lyricsJson.optString(key, "")
                    if (text.isNotBlank()) lyrics[id] = text
                }
            }

            val tagOverrides = mutableMapOf<Long, LocalTagOverride>()
            val tagOverridesJson = root.optJSONObject("tagOverrides") ?: JSONObject()
            tagOverridesJson.keys().forEach { key ->
                key.toLongOrNull()?.let { id ->
                    val item = tagOverridesJson.optJSONObject(key) ?: return@let
                    tagOverrides[id] = LocalTagOverride(
                        title = item.optString("title", ""),
                        artist = item.optString("artist", ""),
                        album = item.optString("album", "")
                    )
                }
            }

            LibraryBackup(
                likedIds = likedIds,
                playlists = playlists,
                queueIds = queueIds,
                playCounts = counts,
                totalListenMs = root.optLong("totalListenMs", 0L),
                resumePositions = resumePositions,
                lyrics = lyrics,
                tagOverrides = tagOverrides
            )
        }.getOrNull()
    }

    private fun saveTagOverrides(overrides: Map<Long, LocalTagOverride>) {
        val json = JSONObject()
        overrides.forEach { (id, override) ->
            json.put(
                id.toString(),
                JSONObject()
                    .put("title", override.title)
                    .put("artist", override.artist)
                    .put("album", override.album)
            )
        }
        prefs.edit()
            .putString(KEY_TAG_OVERRIDES, json.toString())
            .apply()
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
        const val KEY_RESUME_POSITIONS = "resume_positions"
        const val KEY_PLAY_COUNTS = "play_counts"
        const val KEY_TOTAL_LISTEN_MS = "total_listen_ms"
        const val KEY_LYRICS_PREFIX = "lyrics_"
        const val KEY_TAG_OVERRIDES = "tag_overrides"
        const val MAX_RECENTS = 30
        const val MAX_RESUME_POSITIONS = 200
        const val RESUME_MIN_POSITION_MS = 5_000L
        const val RESUME_END_THRESHOLD_MS = 10_000L
    }
}

