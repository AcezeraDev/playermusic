package com.wavemusic.player.data.model

data class LyricsLine(
    val timeMs: Long?,
    val text: String
)

fun parseLyrics(raw: String): List<LyricsLine> {
    if (raw.isBlank()) return emptyList()
    val parsed = mutableListOf<LyricsLine>()
    raw.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@forEach
        val matches = lyricsTimestampRegex.findAll(trimmed).toList()
        if (matches.isEmpty()) {
            parsed += LyricsLine(null, trimmed)
            return@forEach
        }
        val text = trimmed.replace(lyricsTimestampRegex, "").trim()
        matches.forEach { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
            val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            parsed += LyricsLine(
                timeMs = minutes * 60_000L + seconds * 1_000L + fraction,
                text = text.ifBlank { "..." }
            )
            }
    }

    return parsed.sortedWith(compareBy<LyricsLine> { it.timeMs ?: Long.MAX_VALUE }.thenBy { it.text })
}

fun activeLyricsIndex(lines: List<LyricsLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    val timed = lines.withIndex().filter { it.value.timeMs != null }
    if (timed.isEmpty()) return -1
    return timed.lastOrNull { it.value.timeMs.orEmptyTime() <= positionMs }?.index ?: timed.first().index
}

private fun Long?.orEmptyTime(): Long = this ?: Long.MAX_VALUE

private val lyricsTimestampRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
