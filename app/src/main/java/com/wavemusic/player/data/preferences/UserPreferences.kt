package com.wavemusic.player.data.preferences

import android.content.Context
import com.wavemusic.player.data.model.AccentTheme
import com.wavemusic.player.data.model.AudioQuality
import com.wavemusic.player.data.model.EqualizerPreset
import com.wavemusic.player.data.model.SleepTimerOption

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

    fun loadEqualizerPreset(): EqualizerPreset {
        return EqualizerPreset.fromKey(prefs.getString(KEY_EQUALIZER, EqualizerPreset.Normal.key))
    }

    fun saveEqualizerPreset(preset: EqualizerPreset) {
        prefs.edit()
            .putString(KEY_EQUALIZER, preset.key)
            .apply()
    }

    fun loadAccentTheme(): AccentTheme {
        return AccentTheme.fromKey(prefs.getString(KEY_ACCENT_THEME, AccentTheme.Purple.key))
    }

    fun saveAccentTheme(theme: AccentTheme) {
        prefs.edit()
            .putString(KEY_ACCENT_THEME, theme.key)
            .apply()
    }

    fun loadCrossfadeEnabled(): Boolean {
        return prefs.getBoolean(KEY_CROSSFADE, false)
    }

    fun saveCrossfadeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_CROSSFADE, enabled)
            .apply()
    }

    fun loadContinueListeningEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONTINUE_LISTENING, true)
    }

    fun saveContinueListeningEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_CONTINUE_LISTENING, enabled)
            .apply()
    }

    fun loadResumePlaybackPositionEnabled(): Boolean {
        return prefs.getBoolean(KEY_RESUME_PLAYBACK_POSITION, true)
    }

    fun saveResumePlaybackPositionEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_RESUME_PLAYBACK_POSITION, enabled)
            .apply()
    }

    fun loadCustomEqualizerGains(): List<Short> {
        val defaults = EqualizerPreset.Custom.bandGains
        val saved = prefs.getString(KEY_CUSTOM_EQUALIZER, "").orEmpty()
            .split(",")
            .mapNotNull { it.toShortOrNull() }
        return if (saved.size == defaults.size) saved else defaults
    }

    fun saveCustomEqualizerGains(gains: List<Short>) {
        prefs.edit()
            .putString(KEY_CUSTOM_EQUALIZER, gains.joinToString(","))
            .apply()
    }

    fun loadLocalVideosEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_VIDEOS, false)
    }

    fun saveLocalVideosEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_LOCAL_VIDEOS, enabled)
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
        const val KEY_EQUALIZER = "equalizer"
        const val KEY_CUSTOM_EQUALIZER = "custom_equalizer"
        const val KEY_ACCENT_THEME = "accent_theme"
        const val KEY_CROSSFADE = "crossfade"
        const val KEY_CONTINUE_LISTENING = "continue_listening"
        const val KEY_RESUME_PLAYBACK_POSITION = "resume_playback_position"
        const val KEY_LOCAL_VIDEOS = "local_videos"
    }
}

