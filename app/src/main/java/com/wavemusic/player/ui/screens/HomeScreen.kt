package com.wavemusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.PlaybackStats
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.components.MusicCard
import com.wavemusic.player.ui.components.NeonVisualizer
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

private enum class HomeMediaFilter(val label: String) {
    Music("Musicas"),
    Video("Videos")
}

@Composable
fun HomeScreen(
    songs: List<Music>,
    currentMusicId: Long?,
    likedIds: Set<Long>,
    playlists: List<Playlist>,
    recentIds: List<Long>,
    playbackStats: PlaybackStats,
    queueCount: Int,
    isLoading: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onAddToQueue: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit,
    queuedIds: Set<Long>,
    modifier: Modifier = Modifier
) {
    var mediaFilter by rememberSaveable { mutableStateOf(HomeMediaFilter.Music) }
    val audioItems = songs.filterNot { it.isVideo }
    val videoItems = songs.filter { it.isVideo }
    val visibleItems = when (mediaFilter) {
        HomeMediaFilter.Music -> audioItems
        HomeMediaFilter.Video -> videoItems
    }

    if (!hasPermission) {
        PermissionRequestScreen(onRequestPermission = onRequestPermission, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Wave Music",
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Suas musicas e videos baixados, agora em neon",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HomeMediaFilter.entries, key = { it.name }) { item ->
                    val count = if (item == HomeMediaFilter.Music) audioItems.size else videoItems.size
                    FilterChip(
                        selected = mediaFilter == item,
                        onClick = { mediaFilter = item },
                        label = { Text("${item.label} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WavePink.copy(alpha = 0.22f),
                            selectedLabelColor = WaveTextPrimary,
                            containerColor = WaveSurface.copy(alpha = 0.56f),
                            labelColor = WaveTextSecondary
                        )
                    )
                }
            }
        }

        if (isLoading) {
            item {
                EmptyMusicScreen(
                    title = "Carregando biblioteca",
                    message = "Estamos lendo audios e videos do dispositivo."
                )
            }
        } else if (visibleItems.isNotEmpty()) {
            item {
                FeaturedSongCard(
                    music = visibleItems.first(),
                    count = visibleItems.size,
                    onClick = { onSongClick(visibleItems.first()) }
                )
            }

            item {
                HomeStatsRow(
                    songs = visibleItems,
                    likedCount = likedIds.size,
                    queueCount = queueCount,
                    totalListenMs = playbackStats.totalListenMs
                )
            }

            val recentSongs = recentIds
                .mapNotNull { id -> songs.firstOrNull { it.id == id } }
                .filter { it.isVideo == (mediaFilter == HomeMediaFilter.Video) }
                .take(5)
            if (recentSongs.isNotEmpty()) {
                item { SectionTitle("Continuar ouvindo", "Historico recente") }
                items(recentSongs, key = { "recent-${it.id}" }) { music ->
                    MediaCard(
                        music = music,
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        queuedIds = queuedIds,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }
            }

            val mostPlayed = playbackStats.playCounts.entries
                .sortedByDescending { it.value }
                .mapNotNull { entry -> songs.firstOrNull { it.id == entry.key } }
                .filter { it.isVideo == (mediaFilter == HomeMediaFilter.Video) }
                .take(3)
            if (mostPlayed.isNotEmpty()) {
                item { SectionTitle("Mais tocadas", "Seu gosto aparecendo por aqui") }
                items(mostPlayed, key = { "top-${it.id}" }) { music ->
                    MediaCard(
                        music = music,
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        queuedIds = queuedIds,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (mediaFilter == HomeMediaFilter.Video) "Videos do dispositivo" else "Musicas do dispositivo",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Atualizar lista",
                        tint = WaveBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { onRefresh() }
                            .padding(8.dp)
                    )
                }
            }

            items(items = visibleItems, key = { it.id }) { music ->
                MediaCard(
                    music = music,
                    currentMusicId = currentMusicId,
                    likedIds = likedIds,
                    playlists = playlists,
                    queuedIds = queuedIds,
                    onSongClick = onSongClick,
                    onToggleLike = onToggleLike,
                    onAddToPlaylist = onAddToPlaylist,
                    onAddToQueue = onAddToQueue,
                    onRemoveFromQueue = onRemoveFromQueue
                )
            }
        } else {
            item {
                EmptyMusicScreen(
                    title = if (mediaFilter == HomeMediaFilter.Video) "Nenhum video encontrado" else "Nenhuma musica encontrada",
                    message = if (mediaFilter == HomeMediaFilter.Video) {
                        "Baixe videos no aparelho e toque em atualizar."
                    } else {
                        "Baixe arquivos de audio no aparelho e toque em atualizar."
                    },
                    actionText = "Atualizar",
                    onAction = onRefresh
                )
            }
        }
    }
}

@Composable
private fun MediaCard(
    music: Music,
    currentMusicId: Long?,
    likedIds: Set<Long>,
    playlists: List<Playlist>,
    queuedIds: Set<Long>,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onAddToQueue: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit
) {
    MusicCard(
        music = music,
        isCurrent = music.id == currentMusicId,
        isLiked = music.id in likedIds,
        playlists = playlists,
        onClick = onSongClick,
        onToggleLike = onToggleLike,
        onAddToPlaylist = onAddToPlaylist,
        isQueued = music.id in queuedIds,
        onAddToQueue = onAddToQueue,
        onRemoveFromQueue = onRemoveFromQueue
    )
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(WavePurple, WavePink))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Permita acessar suas midias",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "O Wave Music precisa ler audios e videos baixados no dispositivo para montar sua biblioteca.",
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Permitir acesso")
            }
        }
    }
}

@Composable
fun EmptyMusicScreen(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(WaveSurface.copy(alpha = 0.64f))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = WaveTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = WaveTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun FeaturedSongCard(
    music: Music,
    count: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() }
            .background(Brush.linearGradient(listOf(WavePurple, WaveBlue, WavePink)))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (music.isVideo) "Biblioteca de videos" else "Biblioteca local",
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$count ${if (music.isVideo) "videos" else "musicas"} encontrados",
                    color = WaveTextPrimary.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = music.title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.artist,
                    color = WaveTextPrimary.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            AlbumArtwork(
                music = music,
                modifier = Modifier.size(96.dp),
                cornerRadius = 28.dp
            )
        }
    }
}

@Composable
private fun HomeStatsRow(
    songs: List<Music>,
    likedCount: Int,
    queueCount: Int,
    totalListenMs: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("Itens", songs.size.toString(), Modifier.weight(1f))
        StatCard("Curtidas", likedCount.toString(), Modifier.weight(1f))
        StatCard("Fila", queueCount.toString(), Modifier.weight(1f))
        StatCard("Ouvido", listeningLabel(totalListenMs), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(WaveSurface.copy(alpha = 0.68f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NeonVisualizer(
            isPlaying = true,
            seed = value.hashCode().toLong(),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            bars = 8
        )
        Text(
            text = value,
            color = WaveTextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            text = label,
            color = WaveTextSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = title,
            color = WaveTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            text = subtitle,
            color = WaveTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun listeningLabel(totalListenMs: Long): String {
    val minutes = (totalListenMs / 60000L).coerceAtLeast(0)
    return when {
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h"
    }
}
