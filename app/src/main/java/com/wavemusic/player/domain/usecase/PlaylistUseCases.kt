package com.wavemusic.player.domain.usecase

import com.wavemusic.player.data.model.Music
import com.wavemusic.player.data.model.Playlist
import com.wavemusic.player.utils.helpers.resolveSongs

fun playableSongsFor(playlist: Playlist, allSongs: List<Music>): List<Music> {
    return playlist.resolveSongs(allSongs)
}
