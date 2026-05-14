package com.wavemusic.player.data

import android.content.Context

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("wave_music_preferences", Context.MODE_PRIVATE)

    fun loadAudioQuality(): AudioQuality {
        return AudioQuality.fromKey(prefs.getString(KEY_AUDIO_QUALITY, AudioQuality.Automatic.key))
    }

    fun saveAudioQuality(quality: AudioQuality) {
        prefs.edit()
            .putString(KEY_AUDIO_QUALITY, quality.key)
            .apply()
    }

    fun loadMediaNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_MEDIA_NOTIFICATIONS, true)
    }

    fun saveMediaNotificationsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_MEDIA_NOTIFICATIONS, enabled)
            .apply()
    }

    private companion object {
        const val KEY_AUDIO_QUALITY = "audio_quality"
        const val KEY_MEDIA_NOTIFICATIONS = "media_notifications"
    }
}
