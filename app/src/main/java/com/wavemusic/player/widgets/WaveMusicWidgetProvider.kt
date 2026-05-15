package com.wavemusic.player.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wavemusic.player.R
import com.wavemusic.player.notifications.MusicNotificationReceiver

class WaveMusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context))
        }
    }

    companion object {
        fun updateNowPlaying(context: Context, title: String, artist: String, isPlaying: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putBoolean(KEY_PLAYING, isPlaying)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, WaveMusicWidgetProvider::class.java)
            )
            ids.forEach { id ->
                manager.updateAppWidget(id, buildRemoteViews(context))
            }
        }

        private fun buildRemoteViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val title = prefs.getString(KEY_TITLE, "Wave Music").orEmpty()
            val artist = prefs.getString(KEY_ARTIST, "Escolha uma música").orEmpty()
            val isPlaying = prefs.getBoolean(KEY_PLAYING, false)

            return RemoteViews(context.packageName, R.layout.wave_music_widget).apply {
                setTextViewText(R.id.widget_title, title)
                setTextViewText(R.id.widget_artist, artist)
                setTextViewText(R.id.widget_play_pause, if (isPlaying) "Pausar" else "Tocar")
                setOnClickPendingIntent(
                    R.id.widget_previous,
                    actionIntent(context, MusicNotificationReceiver.ACTION_PREVIOUS, 50)
                )
                setOnClickPendingIntent(
                    R.id.widget_play_pause,
                    actionIntent(context, MusicNotificationReceiver.ACTION_PLAY_PAUSE, 51)
                )
                setOnClickPendingIntent(
                    R.id.widget_next,
                    actionIntent(context, MusicNotificationReceiver.ACTION_NEXT, 52)
                )
                setOnClickPendingIntent(
                    R.id.widget_favorite,
                    actionIntent(context, MusicNotificationReceiver.ACTION_FAVORITE, 53)
                )
            }
        }

        private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MusicNotificationReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private const val PREFS = "wave_music_widget"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_PLAYING = "playing"
    }
}

