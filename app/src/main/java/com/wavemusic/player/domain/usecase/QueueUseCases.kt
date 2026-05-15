package com.wavemusic.player.domain.usecase

fun addQueueId(queueIds: List<Long>, id: Long): List<Long> {
    return if (id in queueIds) queueIds else queueIds + id
}

fun playNextQueueId(queueIds: List<Long>, currentId: Long?, id: Long): List<Long> {
    if (id == currentId || id in queueIds) return queueIds
    return listOf(id) + queueIds
}

fun removeFirstQueueId(queueIds: List<Long>, id: Long): List<Long> {
    val index = queueIds.indexOf(id)
    if (index < 0) return queueIds
    return queueIds.toMutableList().also { it.removeAt(index) }
}

fun moveQueueId(queueIds: List<Long>, from: Int, to: Int): List<Long> {
    if (from !in queueIds.indices || to !in queueIds.indices) return queueIds
    return queueIds.toMutableList().also { mutable ->
        val item = mutable.removeAt(from)
        mutable.add(to, item)
    }
}

fun consumeNextValidQueueId(queueIds: List<Long>, validIds: Set<Long>): Pair<Long?, List<Long>> {
    val index = queueIds.indexOfFirst { it in validIds }
    if (index < 0) return null to emptyList()
    return queueIds[index] to queueIds.drop(index + 1)
}
