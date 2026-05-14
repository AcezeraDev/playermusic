package com.wavemusic.player.domain.player

import android.content.Context
import android.media.MediaPlayer
import android.media.session.MediaSession

object WaveMusicPlayerHolder {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null

    @Synchronized
    fun mediaPlayer(): MediaPlayer {
        return mediaPlayer ?: MediaPlayer().also { mediaPlayer = it }
    }

    @Synchronized
    fun mediaSession(context: Context): MediaSession {
        return mediaSession ?: MediaSession(
            context.applicationContext,
            "WaveMusicSession"
        ).also { mediaSession = it }
    }

    @Synchronized
    fun releaseMediaPlayer(player: MediaPlayer) {
        if (mediaPlayer === player) {
            runCatching { player.release() }
            mediaPlayer = null
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

