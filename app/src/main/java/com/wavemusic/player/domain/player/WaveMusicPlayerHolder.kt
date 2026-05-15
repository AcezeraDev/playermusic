package com.wavemusic.player.domain.player

import android.content.Context
import android.media.session.MediaSession
import androidx.media3.exoplayer.ExoPlayer

object WaveMusicPlayerHolder {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    @Synchronized
    fun player(context: Context): ExoPlayer {
        return player ?: ExoPlayer.Builder(context.applicationContext)
            .build()
            .also { player = it }
    }

    @Synchronized
    fun mediaSession(context: Context): MediaSession {
        return mediaSession ?: MediaSession(
            context.applicationContext,
            "WaveMusicSession"
        ).also { mediaSession = it }
    }

    @Synchronized
    fun releasePlayer(exoPlayer: ExoPlayer) {
        if (player === exoPlayer) {
            runCatching { exoPlayer.release() }
            player = null
        }
    }

    @Synchronized
    fun releaseMediaSession(session: MediaSession) {
        if (mediaSession === session) {
            runCatching { session.release() }
            mediaSession = null
        }
    }
}

