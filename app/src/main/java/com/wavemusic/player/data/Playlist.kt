package com.wavemusic.player.data

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
    val description: String = "",
    val imageUri: String? = null,
    val createdAt: Long = id
)
