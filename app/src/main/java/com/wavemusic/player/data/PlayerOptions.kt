package com.wavemusic.player.data

import androidx.compose.ui.graphics.Color

enum class EqualizerPreset(
    val key: String,
    val label: String,
    val bandGains: List<Short>
) {
    Normal("normal", "Normal", listOf(0, 0, 0, 0, 0)),
    BassBoost("bass_boost", "Bass Boost", listOf(900, 520, 120, -80, -160)),
    Pop("pop", "Pop", listOf(180, 420, 620, 360, 180)),
    Rock("rock", "Rock", listOf(650, 320, -120, 360, 720)),
    Electronic("electronic", "Eletrônica", listOf(760, 420, 120, 520, 840)),
    Vocal("vocal", "Vocal", listOf(-220, 80, 660, 520, 120));

    companion object {
        fun fromKey(key: String?): EqualizerPreset {
            return entries.firstOrNull { it.key == key } ?: Normal
        }
    }
}

enum class AccentTheme(
    val key: String,
    val label: String,
    val primary: Color,
    val secondary: Color
) {
    Purple("purple", "Roxo", Color(0xFF8B5CF6), Color(0xFFEC4899)),
    Blue("blue", "Azul", Color(0xFF38BDF8), Color(0xFF6366F1)),
    Pink("pink", "Rosa", Color(0xFFEC4899), Color(0xFFF97316)),
    Cyan("cyan", "Ciano", Color(0xFF22D3EE), Color(0xFF14B8A6));

    companion object {
        fun fromKey(key: String?): AccentTheme {
            return entries.firstOrNull { it.key == key } ?: Purple
        }
    }
}

enum class SleepTimerOption(
    val key: String,
    val label: String,
    val durationMs: Long
) {
    Five("5", "5 min", 5 * 60 * 1000L),
    Ten("10", "10 min", 10 * 60 * 1000L),
    Fifteen("15", "15 min", 15 * 60 * 1000L),
    Thirty("30", "30 min", 30 * 60 * 1000L),
    Hour("60", "1 hora", 60 * 60 * 1000L);
}

data class PlaybackStats(
    val playCounts: Map<Long, Int> = emptyMap(),
    val totalListenMs: Long = 0L
) {
    fun mostPlayedId(): Long? {
        return playCounts.maxByOrNull { it.value }?.key
    }
}

data class LibraryBackup(
    val likedIds: Set<Long>,
    val playlists: List<Playlist>,
    val queueIds: List<Long>,
    val playCounts: Map<Long, Int>,
    val totalListenMs: Long,
    val resumePositions: Map<Long, Long> = emptyMap()
)
