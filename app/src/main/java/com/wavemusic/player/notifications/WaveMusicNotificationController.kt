package com.wavemusic.player.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.session.MediaSession
import android.os.Build
import android.util.LruCache
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wavemusic.player.MainActivity
import com.wavemusic.player.R
import com.wavemusic.player.data.model.Music
import kotlin.math.min

class WaveMusicNotificationController(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val artworkCache = object : LruCache<Long, Bitmap>(ARTWORK_CACHE_SIZE) {}

    @SuppressLint("MissingPermission")
    fun show(
        music: Music,
        isPlaying: Boolean,
        isLiked: Boolean,
        positionMs: Long,
        durationMs: Long,
        sessionToken: MediaSession.Token
    ) {
        val notification = buildNotification(
            music = music,
            isPlaying = isPlaying,
            isLiked = isLiked,
            positionMs = positionMs,
            durationMs = durationMs,
            sessionToken = sessionToken
        )

        if (!canPostNotifications()) return

        runCatching {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun buildNotification(
        music: Music,
        isPlaying: Boolean,
        isLiked: Boolean,
        positionMs: Long,
        durationMs: Long,
        sessionToken: MediaSession.Token
    ): Notification {
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

        val resolvedDuration = durationMs.takeIf { it > 0L } ?: music.durationMs
        val boundedPosition = if (resolvedDuration > 0L) {
            positionMs.coerceIn(0L, resolvedDuration)
        } else {
            0L
        }
        val playPauseIcon = if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play
        val playPauseLabel = if (isPlaying) "Pausar" else "Tocar"
        val favoriteIcon = if (isLiked) {
            R.drawable.ic_notification_favorite_filled
        } else {
            R.drawable.ic_notification_favorite
        }
        val favoriteLabel = if (isLiked) "Remover favorito" else "Favoritar"

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wave_music)
            .setLargeIcon(artworkForSession(music))
            .setContentTitle(music.title)
            .setContentText(music.artist)
            .setContentInfo(formatProgressText(boundedPosition, resolvedDuration))
            .setSubText("Wave Music")
            .setContentIntent(contentIntent)
            .setDeleteIntent(actionIntent(MusicNotificationReceiver.ACTION_CLOSE, REQUEST_CLOSE))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setColor(Color.rgb(34, 34, 40))
            .setColorized(true)
            .addAction(
                favoriteIcon,
                favoriteLabel,
                actionIntent(MusicNotificationReceiver.ACTION_FAVORITE, REQUEST_FAVORITE)
            )
            .addAction(
                R.drawable.ic_notification_previous,
                "Anterior",
                actionIntent(MusicNotificationReceiver.ACTION_PREVIOUS, REQUEST_PREVIOUS)
            )
            .addAction(
                playPauseIcon,
                playPauseLabel,
                actionIntent(MusicNotificationReceiver.ACTION_PLAY_PAUSE, REQUEST_PLAY_PAUSE)
            )
            .addAction(
                R.drawable.ic_notification_next,
                "Proxima",
                actionIntent(MusicNotificationReceiver.ACTION_NEXT, REQUEST_NEXT)
            )
            .addAction(
                R.drawable.ic_notification_close,
                "Fechar",
                actionIntent(MusicNotificationReceiver.ACTION_CLOSE, REQUEST_CLOSE)
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(1, 2, 3)
            )

        if (resolvedDuration > 0L) {
            builder.setProgress(
                PROGRESS_MAX,
                ((boundedPosition.toDouble() / resolvedDuration.toDouble()) * PROGRESS_MAX).toInt(),
                false
            )
        }

        return builder.build()
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun artworkForSession(music: Music): Bitmap {
        return loadAlbumArt(music)
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
            description = "Controles de midia para musicas locais"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @Synchronized
    private fun loadAlbumArt(music: Music): Bitmap {
        artworkCache.get(music.id)?.let { return it }

        val extracted = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, music.uri)
                retriever.embeddedPicture?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } finally {
                retriever.release()
            }
        }.getOrNull()

        val artwork = extracted?.toRoundedSquare() ?: createDefaultArtwork(music)
        artworkCache.put(music.id, artwork)
        return artwork
    }

    private fun Bitmap.toRoundedSquare(): Bitmap {
        val output = Bitmap.createBitmap(ARTWORK_SIZE, ARTWORK_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sourceSize = min(width, height)
        val sourceLeft = (width - sourceSize) / 2
        val sourceTop = (height - sourceSize) / 2
        val source = Rect(sourceLeft, sourceTop, sourceLeft + sourceSize, sourceTop + sourceSize)
        val target = RectF(0f, 0f, ARTWORK_SIZE.toFloat(), ARTWORK_SIZE.toFloat())

        canvas.drawRoundRect(target, ARTWORK_CORNER, ARTWORK_CORNER, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        })
        canvas.saveLayer(target, paint)
        canvas.drawRoundRect(target, ARTWORK_CORNER, ARTWORK_CORNER, paint.apply {
            color = Color.WHITE
        })
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, source, target, paint)
        paint.xfermode = null
        canvas.restore()

        return output
    }

    private fun createDefaultArtwork(music: Music): Bitmap {
        val bitmap = Bitmap.createBitmap(ARTWORK_SIZE, ARTWORK_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, ARTWORK_SIZE.toFloat(), ARTWORK_SIZE.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f,
            0f,
            ARTWORK_SIZE.toFloat(),
            ARTWORK_SIZE.toFloat(),
            intArrayOf(
                Color.rgb(124, 58, 237),
                Color.rgb(236, 72, 153),
                Color.rgb(6, 182, 212)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(bounds, ARTWORK_CORNER, ARTWORK_CORNER, paint)

        paint.shader = null
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 72f
        val initials = music.title
            .split(" ")
            .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank { "W" }
        val y = ARTWORK_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initials, ARTWORK_SIZE / 2f, y, paint)

        return bitmap
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

    private fun formatProgressText(positionMs: Long, durationMs: Long): String {
        if (durationMs <= 0L) return "Wave Music"
        return "${positionMs.formatDuration()} / ${durationMs.formatDuration()}"
    }

    private fun Long.formatDuration(): String {
        val totalSeconds = (this / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        const val CHANNEL_ID = "wave_music_media_controls"
        const val NOTIFICATION_ID = 4207
        const val PROGRESS_MAX = 1000

        private const val REQUEST_OPEN_APP = 11
        private const val REQUEST_FAVORITE = 12
        private const val REQUEST_PREVIOUS = 13
        private const val REQUEST_PLAY_PAUSE = 14
        private const val REQUEST_NEXT = 15
        private const val REQUEST_CLOSE = 16
        private const val ARTWORK_SIZE = 256
        private const val ARTWORK_CORNER = 34f
        private const val ARTWORK_CACHE_SIZE = 24
    }
}

