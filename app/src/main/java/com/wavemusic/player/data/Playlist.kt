package com.wavemusic.player.data

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>
)
