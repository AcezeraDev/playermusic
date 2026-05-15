package com.wavemusic.player.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test
    fun parseLyricsKeepsPlainLines() {
        val lines = parseLyrics("Primeira linha\nSegunda linha")

        assertEquals(2, lines.size)
        assertEquals(null, lines[0].timeMs)
        assertEquals("Primeira linha", lines[0].text)
    }

    @Test
    fun parseLyricsReadsLrcTimestamps() {
        val lines = parseLyrics("[00:10.50]Intro\n[01:02.003]Verso")

        assertEquals(2, lines.size)
        assertEquals(10_500L, lines[0].timeMs)
        assertEquals("Intro", lines[0].text)
        assertEquals(62_003L, lines[1].timeMs)
    }

    @Test
    fun activeLyricsIndexReturnsCurrentTimedLine() {
        val lines = parseLyrics("[00:05.00]Um\n[00:12.00]Dois\n[00:20.00]Tres")

        assertEquals(0, activeLyricsIndex(lines, 6_000L))
        assertEquals(1, activeLyricsIndex(lines, 15_000L))
    }
}
