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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                    text = "Suas músicas baixadas, agora em neon",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (isLoading) {
            item {
                EmptyMusicScreen(
                    title = "Carregando músicas",
                    message = "Estamos lendo os áudios do dispositivo. Isso pode levar alguns segundos na primeira vez."
                )
            }
        } else if (songs.isNotEmpty()) {
            item {
                FeaturedSongCard(
                    music = songs.first(),
                    count = songs.size,
                    onClick = { onSongClick(songs.first()) }
                )
            }

            item {
                HomeStatsRow(
                    songs = songs,
                    likedCount = likedIds.size,
                    queueCount = queueCount,
                    totalListenMs = playbackStats.totalListenMs
                )
            }

            val recentSongs = recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }.take(5)
            if (recentSongs.isNotEmpty()) {
                item {
                    SectionTitle("Continuar ouvindo", "Histórico recente")
                }
                items(recentSongs, key = { "recent-${it.id}" }) { music ->
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
            }

            val mostPlayed = playbackStats.playCounts.entries
                .sortedByDescending { it.value }
                .mapNotNull { entry -> songs.firstOrNull { it.id == entry.key } }
                .take(3)
            if (mostPlayed.isNotEmpty()) {
                item {
                    SectionTitle("Mais tocadas", "Seu gosto aparecendo por aqui")
                }
                items(mostPlayed, key = { "top-${it.id}" }) { music ->
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
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Músicas do dispositivo",
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

            items(items = songs, key = { it.id }) { music ->
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
        } else {
            item {
                EmptyMusicScreen(
                    title = "Nenhuma música encontrada",
                    message = "Baixe arquivos de áudio no aparelho e toque em atualizar.",
                    actionText = "Atualizar",
                    onAction = onRefresh
                )
            }
        }
    }
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
                text = "Permita acessar suas músicas",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "O Wave Music precisa ler os áudios baixados no dispositivo para montar sua biblioteca.",
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
                    text = "Biblioteca local",
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$count músicas encontradas",
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
        StatCard(
            label = "Músicas",
            value = songs.size.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Curtidas",
            value = likedCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Fila",
            value = queueCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Ouvido",
            value = listeningLabel(totalListenMs),
            modifier = Modifier.weight(1f)
        )
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
