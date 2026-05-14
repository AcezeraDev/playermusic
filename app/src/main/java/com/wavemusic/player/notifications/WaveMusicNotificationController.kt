package com.wavemusic.player.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wavemusic.player.MainActivity
import com.wavemusic.player.data.Music

class WaveMusicNotificationController(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    @SuppressLint("MissingPermission")
    fun show(music: Music, isPlaying: Boolean) {
        if (!canPostNotifications()) return

        createChannel()

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            launchIntent,
            pendingIntentFlags()
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (isPlaying) "Pausar" else "Tocar"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(music.title)
            .setContentText(music.artist)
            .setSubText("Wave Music")
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Anterior",
                actionIntent(MusicNotificationReceiver.ACTION_PREVIOUS, REQUEST_PREVIOUS)
            )
            .addAction(
                playPauseIcon,
                playPauseLabel,
                actionIntent(MusicNotificationReceiver.ACTION_PLAY_PAUSE, REQUEST_PLAY_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Próxima",
                actionIntent(MusicNotificationReceiver.ACTION_NEXT, REQUEST_NEXT)
            )
            .build()

        runCatching {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Controles do Wave Music",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controles de mídia para músicas locais"
            setShowBadge(false)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MusicNotificationReceiver::class.java).apply {
            this.action = action
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            pendingIntentFlags()
        )
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private companion object {
        const val CHANNEL_ID = "wave_music_media_controls"
        const val NOTIFICATION_ID = 4207
        const val REQUEST_OPEN_APP = 11
        const val REQUEST_PREVIOUS = 12
        const val REQUEST_PLAY_PAUSE = 13
        const val REQUEST_NEXT = 14
    }
}
