package com.wavemusic.player.utils.helpers

import com.wavemusic.player.data.model.Music
import com.wavemusic.player.data.model.Playlist

fun Playlist.resolveSongs(allSongs: List<Music>): List<Music> {
    val songsById = allSongs.associateBy { it.id }
    return songIds.mapNotNull { songsById[it] }
}
