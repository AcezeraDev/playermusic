package com.wavemusic.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wavemusic.player.data.model.Music
import com.wavemusic.player.data.model.Playlist

@Composable
fun MusicListItem(
    music: Music,
    isCurrent: Boolean,
    isLiked: Boolean,
    playlists: List<Playlist>,
    onClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    modifier: Modifier = Modifier,
    isQueued: Boolean = false,
    onAddToQueue: (Music) -> Unit = {},
    onPlayNext: (Music) -> Unit = {},
    onRemoveFromQueue: (Music) -> Unit = {},
    onRemoveFromPlaylist: ((Music) -> Unit)? = null
) {
    MusicCard(
        music = music,
        isCurrent = isCurrent,
        isLiked = isLiked,
        playlists = playlists,
        onClick = onClick,
        onToggleLike = onToggleLike,
        onAddToPlaylist = onAddToPlaylist,
        isQueued = isQueued,
        onAddToQueue = onAddToQueue,
        onPlayNext = onPlayNext,
        onRemoveFromQueue = onRemoveFromQueue,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        modifier = modifier
    )
}
