package com.wavemusic.player.domain.player

import android.media.session.MediaSession
import com.wavemusic.player.data.model.Music

data class WaveMusicForegroundSnapshot(
    val music: Music?,
    val isPlaying: Boolean,
    val isLiked: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val sessionToken: MediaSession.Token?
)

object WaveMusicForegroundState {
    private var snapshot = WaveMusicForegroundSnapshot(
        music = null,
        isPlaying = false,
        isLiked = false,
        positionMs = 0L,
        durationMs = 0L,
        sessionToken = null
    )

    @Synchronized
    fun update(
        music: Music?,
        isPlaying: Boolean,
        isLiked: Boolean,
        positionMs: Long,
        durationMs: Long,
        sessionToken: MediaSession.Token?
    ) {
        snapshot = WaveMusicForegroundSnapshot(
            music = music,
            isPlaying = isPlaying,
            isLiked = isLiked,
            positionMs = positionMs,
            durationMs = durationMs,
            sessionToken = sessionToken
        )
    }

    @Synchronized
    fun clear() {
        snapshot = WaveMusicForegroundSnapshot(
            music = null,
            isPlaying = false,
            isLiked = false,
            positionMs = 0L,
            durationMs = 0L,
            sessionToken = null
        )
    }

    @Synchronized
    fun snapshot(): WaveMusicForegroundSnapshot = snapshot
}

