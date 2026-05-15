package com.wavemusic.player.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueUseCasesTest {
    @Test
    fun addQueueIdIgnoresDuplicates() {
        assertEquals(listOf(1L, 2L), addQueueId(listOf(1L, 2L), 2L))
    }

    @Test
    fun moveQueueIdMovesWithinBounds() {
        assertEquals(listOf(2L, 1L, 3L), moveQueueId(listOf(1L, 2L, 3L), 1, 0))
    }

    @Test
    fun playNextQueueIdAddsToFront() {
        assertEquals(listOf(3L, 1L, 2L), playNextQueueId(listOf(1L, 2L), currentId = 9L, id = 3L))
    }

    @Test
    fun consumeNextValidQueueIdSkipsStaleItems() {
        val result = consumeNextValidQueueId(listOf(9L, 2L, 3L), setOf(2L, 3L))

        assertEquals(2L, result.first)
        assertEquals(listOf(3L), result.second)
    }
}
