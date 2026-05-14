package com.wavemusic.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.components.MusicCard
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun SearchScreen(
    songs: List<Music>,
    currentMusicId: Long?,
    likedIds: Set<Long>,
    playlists: List<Playlist>,
    queuedIds: Set<Long>,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onAddToQueue: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(SearchFilter.All) }
    val filteredSongs = songs.filter {
        val term = query.trim()
        if (term.isBlank()) {
            true
        } else {
            when (filter) {
                SearchFilter.All -> it.title.contains(term, true) ||
                    it.artist.contains(term, true) ||
                    it.album.contains(term, true) ||
                    it.duration.contains(term, true) ||
                    it.folder.contains(term, true)
                SearchFilter.Title -> it.title.contains(term, true)
                SearchFilter.Artist -> it.artist.contains(term, true)
                SearchFilter.Album -> it.album.contains(term, true)
                SearchFilter.Duration -> it.duration.contains(term, true)
                SearchFilter.Folder -> it.folder.contains(term, true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Text(
            text = "Buscar",
            color = WaveTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Filtre músicas, artistas ou álbuns",
            color = WaveTextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SearchFilter.entries, key = { it.name }) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WaveBlue.copy(alpha = 0.2f),
                        selectedLabelColor = WaveTextPrimary,
                        containerColor = WaveSurface.copy(alpha = 0.56f),
                        labelColor = WaveTextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Pesquisar") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Pesquisar"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WaveTextPrimary,
                unfocusedTextColor = WaveTextPrimary,
                focusedBorderColor = WaveBlue,
                unfocusedBorderColor = WaveSurface,
                focusedContainerColor = WaveSurface.copy(alpha = 0.62f),
                unfocusedContainerColor = WaveSurface.copy(alpha = 0.56f),
                cursorColor = WaveBlue,
                focusedLeadingIconColor = WaveBlue,
                unfocusedLeadingIconColor = WaveTextSecondary,
                focusedPlaceholderColor = WaveTextSecondary,
                unfocusedPlaceholderColor = WaveTextSecondary
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = filteredSongs, key = { it.id }) { music ->
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
            if (filteredSongs.isEmpty()) {
                item {
                    EmptyMusicScreen(
                        title = "Nada encontrado",
                        message = "Tente buscar pelo nome da música, artista ou álbum."
                    )
                }
            }
        }
    }
}

private enum class SearchFilter(val label: String) {
    All("Tudo"),
    Title("Música"),
    Artist("Artista"),
    Album("Álbum"),
    Duration("Duração"),
    Folder("Pasta")
}
