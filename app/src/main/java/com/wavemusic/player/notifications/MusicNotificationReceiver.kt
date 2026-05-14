package com.wavemusic.player.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wavemusic.player.domain.player.WaveMusicPlaybackService

class MusicNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action.orEmpty()
        val handler = actionHandler
        if (handler != null) {
            handler(action)
        } else if (action == ACTION_CLOSE) {
            WaveMusicPlaybackService.stop(context)
        }
    }

    companion object {
        const val ACTION_FAVORITE = "com.wavemusic.player.action.FAVORITE"
        const val ACTION_PLAY_PAUSE = "com.wavemusic.player.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.wavemusic.player.action.NEXT"
        const val ACTION_PREVIOUS = "com.wavemusic.player.action.PREVIOUS"
        const val ACTION_CLOSE = "com.wavemusic.player.action.CLOSE"

        var actionHandler: ((String) -> Unit)? = null
    }
}

