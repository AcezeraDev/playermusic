package com.wavemusic.player.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        actionHandler?.invoke(intent.action.orEmpty())
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.wavemusic.player.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.wavemusic.player.action.NEXT"
        const val ACTION_PREVIOUS = "com.wavemusic.player.action.PREVIOUS"

        var actionHandler: ((String) -> Unit)? = null
    }
}
