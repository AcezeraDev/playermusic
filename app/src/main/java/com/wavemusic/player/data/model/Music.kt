package com.wavemusic.player.data.model

import android.net.Uri

data class Music(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val folder: String = "Músicas",
    val dateAddedSeconds: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = "audio/*"
) {
    val duration: String
        get() = formatDuration(durationMs)

    val sizeLabel: String
        get() {
            if (sizeBytes <= 0L) return "Tamanho desconhecido"
            val mb = sizeBytes / (1024f * 1024f)
            return "%.1f MB".format(mb)
        }

    val isVideo: Boolean
        get() = mimeType.startsWith("video/", ignoreCase = true)

    val mediaTypeLabel: String
        get() = if (isVideo) "Video" else "Musica"
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

