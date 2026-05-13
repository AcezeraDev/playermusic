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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.components.MusicCard
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

private data class LibrarySection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val target: String
)

@Composable
fun LibraryScreen(
    songs: List<Music>,
    currentMusicId: Long?,
    likedIds: Set<Long>,
    playlists: List<Playlist>,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onRemoveFromPlaylist: (Music, Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf("root") }
    var selectedName by rememberSaveable { mutableStateOf("") }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }

    val sections = listOf(
        LibrarySection(
            title = "Músicas curtidas",
            subtitle = "${likedIds.size} faixas salvas",
            icon = Icons.Rounded.Favorite,
            colors = listOf(WavePink, WavePurple),
            target = "liked"
        ),
        LibrarySection(
            title = "Playlists",
            subtitle = "${playlists.size} playlists criadas",
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            colors = listOf(WavePurple, WaveBlue),
            target = "playlists"
        ),
        LibrarySection(
            title = "Álbuns",
            subtitle = "${songs.map { it.album }.distinct().size} álbuns no dispositivo",
            icon = Icons.Rounded.Album,
            colors = listOf(WaveBlue, WavePink),
            target = "albums"
        ),
        LibrarySection(
            title = "Artistas",
            subtitle = "${songs.map { it.artist }.distinct().size} artistas encontrados",
            icon = Icons.Rounded.Person,
            colors = listOf(WavePurple, WavePink),
            target = "artists"
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LibraryHeader(
                title = titleForPage(page, selectedName),
                subtitle = subtitleForPage(page, songs, playlists, likedIds),
                showBack = page != "root",
                onBack = {
                    page = when (page) {
                        "albumSongs" -> "albums"
                        "artistSongs" -> "artists"
                        "playlistSongs" -> "playlists"
                        else -> "root"
                    }
                    selectedName = ""
                    if (page != "playlistSongs") selectedPlaylistId = null
                }
            )
        }

        when (page) {
            "root" -> {
                items(sections) { section ->
                    LibrarySectionCard(
                        section = section,
                        onClick = { page = section.target }
                    )
                }
            }

            "liked" -> {
                val likedSongs = songs.filter { it.id in likedIds }
                songItems(
                    songs = likedSongs,
                    currentMusicId = currentMusicId,
                    likedIds = likedIds,
                    playlists = playlists,
                    onSongClick = onSongClick,
                    onToggleLike = onToggleLike,
                    onAddToPlaylist = onAddToPlaylist
                )
            }

            "playlists" -> {
                item {
                    PlaylistCreator(onCreatePlaylist = onCreatePlaylist)
                }

                if (playlists.isEmpty()) {
                    item {
                        EmptyMusicScreen(
                            title = "Nenhuma playlist criada",
                            message = "Crie uma playlist e adicione músicas pelo menu de opções de cada faixa."
                        )
                    }
                } else {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = {
                                selectedPlaylistId = playlist.id
                                selectedName = playlist.name
                                page = "playlistSongs"
                            },
                            onDelete = { onDeletePlaylist(playlist) }
                        )
                    }
                }
            }

            "playlistSongs" -> {
                val playlist = playlists.firstOrNull { it.id == selectedPlaylistId }
                val playlistSongs = playlist?.songIds.orEmpty()
                    .mapNotNull { id -> songs.firstOrNull { it.id == id } }

                if (playlist == null || playlistSongs.isEmpty()) {
                    item {
                        EmptyMusicScreen(
                            title = "Playlist vazia",
                            message = "Adicione músicas pelo botão de opções na lista inicial ou na busca."
                        )
                    }
                } else {
                    items(playlistSongs, key = { it.id }) { music ->
                        MusicCard(
                            music = music,
                            isCurrent = music.id == currentMusicId,
                            isLiked = music.id in likedIds,
                            playlists = playlists,
                            onClick = onSongClick,
                            onToggleLike = onToggleLike,
                            onAddToPlaylist = { song, selectedPlaylist ->
                                onAddToPlaylist(song, selectedPlaylist)
                            }
                        )
                        Text(
                            text = "Remover desta playlist",
                            color = WavePink,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .padding(start = 10.dp, top = 4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onRemoveFromPlaylist(music, playlist) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            "albums" -> {
                val albums = songs.groupBy { it.album.ifBlank { "Álbum desconhecido" } }
                items(albums.keys.sorted(), key = { it }) { album ->
                    CollectionRow(
                        title = album,
                        subtitle = "${albums[album].orEmpty().size} músicas",
                        icon = Icons.Rounded.Album,
                        colors = listOf(WaveBlue, WavePink),
                        onClick = {
                            selectedName = album
                            page = "albumSongs"
                        }
                    )
                }
            }

            "albumSongs" -> {
                val albumSongs = songs.filter { it.album.ifBlank { "Álbum desconhecido" } == selectedName }
                songItems(
                    songs = albumSongs,
                    currentMusicId = currentMusicId,
                    likedIds = likedIds,
                    playlists = playlists,
                    onSongClick = onSongClick,
                    onToggleLike = onToggleLike,
                    onAddToPlaylist = onAddToPlaylist
                )
            }

            "artists" -> {
                val artists = songs.groupBy { it.artist.ifBlank { "Artista desconhecido" } }
                items(artists.keys.sorted(), key = { it }) { artist ->
                    CollectionRow(
                        title = artist,
                        subtitle = "${artists[artist].orEmpty().size} músicas",
                        icon = Icons.Rounded.Person,
                        colors = listOf(WavePurple, WaveBlue),
                        onClick = {
                            selectedName = artist
                            page = "artistSongs"
                        }
                    )
                }
            }

            "artistSongs" -> {
                val artistSongs = songs.filter { it.artist.ifBlank { "Artista desconhecido" } == selectedName }
                songItems(
                    songs = artistSongs,
                    currentMusicId = currentMusicId,
                    likedIds = likedIds,
                    playlists = playlists,
                    onSongClick = onSongClick,
                    onToggleLike = onToggleLike,
                    onAddToPlaylist = onAddToPlaylist
                )
            }
        }
    }
}

private fun LazyListScope.songItems(
    songs: List<Music>,
    currentMusicId: Long?,
    likedIds: Set<Long>,
    playlists: List<Playlist>,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit
) {
    if (songs.isEmpty()) {
        item {
            EmptyMusicScreen(
                title = "Nada por aqui",
                message = "Quando houver músicas nesta seção, elas aparecerão aqui."
            )
        }
    } else {
        items(songs, key = { it.id }) { music ->
            MusicCard(
                music = music,
                isCurrent = music.id == currentMusicId,
                isLiked = music.id in likedIds,
                playlists = playlists,
                onClick = onSongClick,
                onToggleLike = onToggleLike,
                onAddToPlaylist = onAddToPlaylist
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit
) {
    Column {
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibrarySectionCard(
    section: LibrarySection,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = WaveSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientIcon(
                icon = section.icon,
                colors = section.colors,
                contentDescription = section.title
            )

            Spacer(modifier = Modifier.size(16.dp))

            Column {
                Text(
                    text = section.title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = section.subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PlaylistCreator(onCreatePlaylist: (String) -> Unit) {
    var playlistName by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WaveSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nova playlist",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nome") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WaveTextPrimary,
                        unfocusedTextColor = WaveTextPrimary,
                        focusedBorderColor = WaveBlue,
                        unfocusedBorderColor = WaveSurface,
                        focusedContainerColor = WaveSurface.copy(alpha = 0.62f),
                        unfocusedContainerColor = WaveSurface.copy(alpha = 0.56f),
                        cursorColor = WaveBlue,
                        focusedPlaceholderColor = WaveTextSecondary,
                        unfocusedPlaceholderColor = WaveTextSecondary
                    )
                )
                Spacer(modifier = Modifier.size(10.dp))
                Button(
                    onClick = {
                        val name = playlistName.trim()
                        if (name.isNotEmpty()) {
                            onCreatePlaylist(name)
                            playlistName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Criar")
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = WaveSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientIcon(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                colors = listOf(WavePurple, WaveBlue),
                contentDescription = playlist.name
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${playlist.songIds.size} músicas",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Excluir playlist",
                    tint = WavePink
                )
            }
        }
    }
}

@Composable
private fun CollectionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = WaveSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientIcon(
                icon = icon,
                colors = colors,
                contentDescription = title
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun GradientIcon(
    icon: ImageVector,
    colors: List<Color>,
    contentDescription: String?
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = WaveTextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun titleForPage(page: String, selectedName: String): String {
    return when (page) {
        "liked" -> "Músicas curtidas"
        "playlists" -> "Playlists"
        "playlistSongs" -> selectedName
        "albums" -> "Álbuns"
        "albumSongs" -> selectedName
        "artists" -> "Artistas"
        "artistSongs" -> selectedName
        else -> "Biblioteca"
    }
}

private fun subtitleForPage(
    page: String,
    songs: List<Music>,
    playlists: List<Playlist>,
    likedIds: Set<Long>
): String {
    return when (page) {
        "liked" -> "${likedIds.size} músicas curtidas"
        "playlists", "playlistSongs" -> "${playlists.size} playlists criadas"
        "albums", "albumSongs" -> "${songs.map { it.album }.distinct().size} álbuns no dispositivo"
        "artists", "artistSongs" -> "${songs.map { it.artist }.distinct().size} artistas encontrados"
        else -> "${songs.size} músicas carregadas do dispositivo"
    }
}
