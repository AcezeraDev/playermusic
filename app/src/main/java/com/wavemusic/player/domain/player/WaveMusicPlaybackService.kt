package com.wavemusic.player.domain.player

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.wavemusic.player.notifications.MusicNotificationReceiver
import com.wavemusic.player.notifications.WaveMusicNotificationController

class WaveMusicPlaybackService : Service() {
    private lateinit var notificationController: WaveMusicNotificationController
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        notificationController = WaveMusicNotificationController(applicationContext)
        mediaSession = MediaSession(applicationContext, "WaveMusicForegroundSession").apply {
            isActive = true
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = forward(MusicNotificationReceiver.ACTION_PLAY_PAUSE)
                override fun onPause() = forward(MusicNotificationReceiver.ACTION_PLAY_PAUSE)
                override fun onSkipToNext() = forward(MusicNotificationReceiver.ACTION_NEXT)
                override fun onSkipToPrevious() = forward(MusicNotificationReceiver.ACTION_PREVIOUS)
                override fun onStop() = forward(MusicNotificationReceiver.ACTION_CLOSE)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            MusicNotificationReceiver.ACTION_CLOSE -> {
                forward(MusicNotificationReceiver.ACTION_CLOSE)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            MusicNotificationReceiver.ACTION_PLAY_PAUSE,
            MusicNotificationReceiver.ACTION_NEXT,
            MusicNotificationReceiver.ACTION_PREVIOUS,
            MusicNotificationReceiver.ACTION_FAVORITE -> {
                forward(intent.action.orEmpty())
            }
        }

        keepPlaybackForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { mediaSession.release() }
        super.onDestroy()
    }

    private fun keepPlaybackForeground() {
        val state = WaveMusicForegroundState.snapshot()
        val music = state.music
        if (music == null || !state.isPlaying) {
            stopSelf()
            return
        }

        val notification = notificationController.buildNotification(
            music = music,
            isPlaying = state.isPlaying,
            isLiked = state.isLiked,
            positionMs = state.positionMs,
            durationMs = state.durationMs.takeIf { it > 0L } ?: music.durationMs,
            sessionToken = state.sessionToken ?: mediaSession.sessionToken
        )
        startForeground(WaveMusicNotificationController.NOTIFICATION_ID, notification)
    }

    private fun forward(action: String) {
        MusicNotificationReceiver.actionHandler?.invoke(action)
    }

    companion object {
        private const val ACTION_START = "com.wavemusic.player.domain.player.START_FOREGROUND"
        private const val ACTION_STOP = "com.wavemusic.player.domain.player.STOP_FOREGROUND"

        fun start(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, WaveMusicPlaybackService::class.java).apply {
                action = ACTION_START
            }
            runCatching {
                ContextCompat.startForegroundService(appContext, intent)
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, WaveMusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching {
                appContext.startService(intent)
            }.onFailure {
                appContext.stopService(Intent(appContext, WaveMusicPlaybackService::class.java))
            }
        }

        fun handleNotificationAction(context: Context, action: String) {
            if (action == MusicNotificationReceiver.ACTION_CLOSE) {
                stop(context)
                return
            }
            val appContext = context.applicationContext
            val intent = Intent(appContext, WaveMusicPlaybackService::class.java).apply {
                this.action = action
            }
            runCatching {
                appContext.startService(intent)
            }
        }
    }
}

