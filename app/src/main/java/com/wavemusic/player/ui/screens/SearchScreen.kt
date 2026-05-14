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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.components.MusicCard
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
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
    var detail by remember { mutableStateOf<SearchDetail?>(null) }
    val normalizedQuery = query.trim()

    val artistResults by remember(songs, normalizedQuery) {
        derivedStateOf {
            songs
                .filterNot { it.isVideo }
                .groupBy { it.artist.normalizedKey() }
                .values
                .mapNotNull { group ->
                    val sorted = group.sortedByDescending { it.dateAddedSeconds }
                    val first = sorted.firstOrNull() ?: return@mapNotNull null
                    ArtistResult(
                        name = first.artist,
                        songs = sorted
                    )
                }
                .filter { normalizedQuery.isBlank() || it.name.contains(normalizedQuery, ignoreCase = true) }
                .sortedBy { it.name.lowercase() }
        }
    }

    val albumResults by remember(songs, normalizedQuery) {
        derivedStateOf {
            songs
                .filterNot { it.isVideo }
                .groupBy { "${it.album.normalizedKey()}|${it.artist.normalizedKey()}" }
                .values
                .mapNotNull { group ->
                    val sorted = group.sortedByDescending { it.dateAddedSeconds }
                    val first = sorted.firstOrNull() ?: return@mapNotNull null
                    AlbumResult(
                        key = "${first.album.normalizedKey()}|${first.artist.normalizedKey()}",
                        name = first.album,
                        artist = first.artist,
                        songs = sorted
                    )
                }
                .filter {
                    normalizedQuery.isBlank() ||
                        it.name.contains(normalizedQuery, ignoreCase = true) ||
                        it.artist.contains(normalizedQuery, ignoreCase = true)
                }
                .sortedWith(compareBy<AlbumResult> { it.name.lowercase() }.thenBy { it.artist.lowercase() })
        }
    }

    val songResults by remember(songs, normalizedQuery) {
        derivedStateOf {
            songs
                .filterNot { it.isVideo }
                .filter {
                    normalizedQuery.isBlank() ||
                        it.title.contains(normalizedQuery, ignoreCase = true) ||
                        it.artist.contains(normalizedQuery, ignoreCase = true) ||
                        it.album.contains(normalizedQuery, ignoreCase = true)
                }
                .sortedByDescending { it.dateAddedSeconds }
        }
    }

    val videoResults by remember(songs, normalizedQuery) {
        derivedStateOf {
            songs
                .filter { it.isVideo }
                .filter {
                    normalizedQuery.isBlank() ||
                        it.title.contains(normalizedQuery, ignoreCase = true) ||
                        it.artist.contains(normalizedQuery, ignoreCase = true) ||
                        it.album.contains(normalizedQuery, ignoreCase = true) ||
                        it.folder.contains(normalizedQuery, ignoreCase = true)
                }
                .sortedByDescending { it.dateAddedSeconds }
        }
    }

    val detailSongs = when (val selectedDetail = detail) {
        is SearchDetail.Artist -> artistResults.firstOrNull { it.name == selectedDetail.name }?.songs
            ?: songs.filter { !it.isVideo && it.artist == selectedDetail.name }.sortedByDescending { it.dateAddedSeconds }
        is SearchDetail.Album -> albumResults.firstOrNull { it.key == selectedDetail.key }?.songs
            ?: songs.filter { "${it.album.normalizedKey()}|${it.artist.normalizedKey()}" == selectedDetail.key }
                .filterNot { it.isVideo }
                .sortedByDescending { it.dateAddedSeconds }
        null -> emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        SearchHeader(
            title = detail?.title ?: "Buscar",
            subtitle = detail?.subtitle ?: "Filtre músicas, artistas ou álbuns",
            showBack = detail != null,
            onBack = { detail = null }
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (detail == null) {
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
        }

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
            if (detail != null) {
                if (detailSongs.isEmpty()) {
                    item {
                        EmptyMusicScreen(
                            title = "Nenhum resultado encontrado",
                            message = "Não há músicas nesta seleção."
                        )
                    }
                } else {
                    items(detailSongs, key = { it.id }) { music ->
                        SearchMusicCard(
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
                return@LazyColumn
            }

            when (filter) {
                SearchFilter.All -> {
                    val hasResults = songResults.isNotEmpty() ||
                        videoResults.isNotEmpty() ||
                        artistResults.isNotEmpty() ||
                        albumResults.isNotEmpty()
                    if (!hasResults) {
                        item {
                            EmptyMusicScreen(
                                title = "Nenhum resultado encontrado",
                                message = "Tente buscar pelo nome da música, artista ou álbum."
                            )
                        }
                    } else {
                        if (songResults.isNotEmpty()) {
                            item { SearchSectionTitle("Músicas", "${songResults.size} resultado(s)") }
                            items(songResults, key = { "song-${it.id}" }) { music ->
                                SearchMusicCard(
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
                        if (videoResults.isNotEmpty()) {
                            item { SearchSectionTitle("Videos", "${videoResults.size} resultado(s)") }
                            items(videoResults, key = { "video-${it.id}" }) { music ->
                                SearchMusicCard(
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
                        if (artistResults.isNotEmpty()) {
                            item { SearchSectionTitle("Artistas", "${artistResults.size} resultado(s)") }
                            items(artistResults, key = { "artist-${it.name.normalizedKey()}" }) { artist ->
                                CollectionResultRow(
                                    title = artist.name,
                                    subtitle = "${artist.songs.size} música(s)",
                                    icon = Icons.Rounded.Person,
                                    music = artist.songs.firstOrNull(),
                                    gradient = listOf(WavePurple, WaveBlue),
                                    onClick = { detail = SearchDetail.Artist(artist.name) }
                                )
                            }
                        }
                        if (albumResults.isNotEmpty()) {
                            item { SearchSectionTitle("Álbuns", "${albumResults.size} resultado(s)") }
                            items(albumResults, key = { "album-${it.key}" }) { album ->
                                AlbumResultRow(
                                    album = album,
                                    onClick = { detail = SearchDetail.Album(album.key, album.name, album.artist) }
                                )
                            }
                        }
                    }
                }

                SearchFilter.Song -> {
                    if (songResults.isEmpty()) {
                        item {
                            EmptyMusicScreen(
                                title = "Nenhuma música encontrada",
                                message = "Tente buscar por outro nome de música."
                            )
                        }
                    } else {
                        items(songResults, key = { it.id }) { music ->
                            SearchMusicCard(
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
                }

                SearchFilter.Video -> {
                    if (videoResults.isEmpty()) {
                        item {
                            EmptyMusicScreen(
                                title = "Nenhum video encontrado",
                                message = "Tente buscar por outro nome de video."
                            )
                        }
                    } else {
                        items(videoResults, key = { it.id }) { music ->
                            SearchMusicCard(
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
                }

                SearchFilter.Artist -> {
                    if (artistResults.isEmpty()) {
                        item {
                            EmptyMusicScreen(
                                title = "Nenhum artista encontrado",
                                message = "Tente buscar por outro nome de artista."
                            )
                        }
                    } else {
                        items(artistResults, key = { it.name.normalizedKey() }) { artist ->
                            CollectionResultRow(
                                title = artist.name,
                                subtitle = "${artist.songs.size} música(s)",
                                icon = Icons.Rounded.Person,
                                music = artist.songs.firstOrNull(),
                                gradient = listOf(WavePurple, WaveBlue),
                                onClick = { detail = SearchDetail.Artist(artist.name) }
                            )
                        }
                    }
                }

                SearchFilter.Album -> {
                    if (albumResults.isEmpty()) {
                        item {
                            EmptyMusicScreen(
                                title = "Nenhum álbum encontrado",
                                message = "Tente buscar por outro nome de álbum."
                            )
                        }
                    } else {
                        items(albumResults, key = { it.key }) { album ->
                            AlbumResultRow(
                                album = album,
                                onClick = { detail = SearchDetail.Album(album.key, album.name, album.artist) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Voltar",
                    tint = WaveTextPrimary
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchMusicCard(
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
private fun SearchSectionTitle(title: String, subtitle: String) {
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

@Composable
private fun AlbumResultRow(
    album: AlbumResult,
    onClick: () -> Unit
) {
    CollectionResultRow(
        title = album.name,
        subtitle = "${album.artist} • ${album.songs.size} música(s)",
        icon = Icons.Rounded.Album,
        music = album.songs.firstOrNull(),
        gradient = listOf(WaveBlue, WavePink),
        onClick = onClick
    )
}

@Composable
private fun CollectionResultRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    music: Music?,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        color = WaveSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (music != null) {
                AlbumArtwork(
                    music = music,
                    modifier = Modifier.size(62.dp),
                    cornerRadius = 18.dp
                )
            } else {
                DefaultCollectionArtwork(
                    icon = icon,
                    gradient = gradient,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DefaultCollectionArtwork(
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient.ifEmpty { listOf(WaveSurfaceBright, WavePurple) })),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WaveTextPrimary,
            modifier = Modifier.size(30.dp)
        )
    }
}

private data class ArtistResult(
    val name: String,
    val songs: List<Music>
)

private data class AlbumResult(
    val key: String,
    val name: String,
    val artist: String,
    val songs: List<Music>
)

private sealed class SearchDetail {
    data class Artist(val name: String) : SearchDetail()
    data class Album(val key: String, val name: String, val artist: String) : SearchDetail()

    val title: String
        get() = when (this) {
            is Artist -> name
            is Album -> name
        }

    val subtitle: String
        get() = when (this) {
            is Artist -> "Músicas deste artista"
            is Album -> "$artist • músicas deste álbum"
        }
}

private enum class SearchFilter(val label: String) {
    All("Tudo"),
    Song("Música"),
    Video("Videos"),
    Artist("Artista"),
    Album("Álbum")
}

private fun String.normalizedKey(): String {
    return trim().lowercase().ifBlank { "desconhecido" }
}
