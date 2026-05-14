package com.wavemusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.wavemusic.player.data.PlaybackStats
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.components.AnimatedCard
import com.wavemusic.player.ui.components.AnimatedIconButton
import com.wavemusic.player.ui.components.MusicCard
import com.wavemusic.player.ui.components.PlaylistCard
import com.wavemusic.player.ui.components.PlaylistCover
import com.wavemusic.player.ui.components.PlaylistImagePicker
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
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
    recentIds: List<Long>,
    playbackStats: PlaybackStats,
    queuedIds: Set<Long>,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onCreatePlaylist: (String, String, String?, List<Long>) -> Unit,
    onUpdatePlaylist: (Playlist, String, String, String?, List<Long>) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onRemoveFromPlaylist: (Music, Playlist) -> Unit,
    onAddToQueue: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf("root") }
    var selectedName by rememberSaveable { mutableStateOf("") }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPlaylistEditor by rememberSaveable { mutableStateOf(false) }
    var playlistForEdit by remember { mutableStateOf<Playlist?>(null) }
    var playlistNotice by rememberSaveable { mutableStateOf<String?>(null) }

    val sections = remember(songs.size, playlists.size, likedIds.size, recentIds.size, playbackStats) {
        listOf(
            LibrarySection(
                title = "Musicas curtidas",
                subtitle = "${likedIds.size} faixas salvas",
                icon = Icons.Rounded.Favorite,
                colors = listOf(WavePink, WavePurple),
                target = "liked"
            ),
            LibrarySection(
                title = "Playlists",
                subtitle = "${playlists.size} colecoes criadas",
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                colors = listOf(WavePurple, WaveBlue),
                target = "playlists"
            ),
            LibrarySection(
                title = "Albuns",
                subtitle = "${songs.map { it.album }.distinct().size} albuns no dispositivo",
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
            ),
            LibrarySection(
                title = "Pastas",
                subtitle = "${songs.map { it.folder }.distinct().size} pastas com musicas",
                icon = Icons.Rounded.Folder,
                colors = listOf(WaveBlue, WavePurple),
                target = "folders"
            ),
            LibrarySection(
                title = "Historico",
                subtitle = "${recentIds.size} musicas recentes",
                icon = Icons.Rounded.History,
                colors = listOf(WavePink, WaveBlue),
                target = "history"
            ),
            LibrarySection(
                title = "Estatisticas",
                subtitle = "Mais tocadas e tempo ouvindo",
                icon = Icons.Rounded.QueryStats,
                colors = listOf(WavePurple, WavePink),
                target = "stats"
            )
        )
    }

    fun goBack() {
        page = when (page) {
            "albumSongs" -> "albums"
            "artistSongs" -> "artists"
            "playlistSongs" -> "playlists"
            "folderSongs" -> "folders"
            else -> "root"
        }
        if (page != "playlistSongs") selectedPlaylistId = null
        selectedName = ""
    }

    fun playlistSongs(playlist: Playlist): List<Music> {
        return playlist.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
    }

    fun playPlaylist(playlist: Playlist) {
        val selectedSongs = playlistSongs(playlist)
        val firstSong = selectedSongs.firstOrNull()
        if (firstSong == null) {
            playlistNotice = "Essa playlist ainda esta vazia."
            return
        }
        onSongClick(firstSong)
        selectedSongs.drop(1).forEach(onAddToQueue)
        playlistNotice = "Tocando ${playlist.name}. Proximas faixas foram enviadas para a fila."
    }

    Crossfade(
        targetState = page,
        label = "library-page-transition",
        modifier = modifier.fillMaxSize()
    ) { currentPage ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LibraryHeader(
                    title = titleForPage(currentPage, selectedName),
                    subtitle = subtitleForPage(currentPage, songs, playlists, likedIds),
                    showBack = currentPage != "root",
                    onBack = ::goBack
                )
            }

            playlistNotice?.let { notice ->
                item {
                    NoticeCard(
                        message = notice,
                        onDismiss = { playlistNotice = null }
                    )
                }
            }

            when (currentPage) {
                "root" -> {
                    items(sections, key = { it.target }) { section ->
                        EnteringItem {
                            LibrarySectionCard(
                                section = section,
                                onClick = { page = section.target }
                            )
                        }
                    }
                }

                "liked" -> {
                    val likedSongs = songs.filter { it.id in likedIds }
                    songItems(
                        songs = likedSongs,
                        emptyTitle = "Nenhuma musica curtida",
                        emptyMessage = "Toque no coracao das musicas para montar seus favoritos.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }

                "playlists" -> {
                    item {
                        PlaylistHero(
                            playlistCount = playlists.size,
                            onCreateClick = {
                                playlistForEdit = null
                                showPlaylistEditor = true
                            }
                        )
                    }

                    if (playlists.isEmpty()) {
                        item {
                            EmptyPlaylistState(
                                onCreateClick = {
                                    playlistForEdit = null
                                    showPlaylistEditor = true
                                }
                            )
                        }
                    } else {
                        items(playlists, key = { it.id }) { playlist ->
                            EnteringItem {
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = {
                                        selectedPlaylistId = playlist.id
                                        selectedName = playlist.name
                                        page = "playlistSongs"
                                    },
                                    onPlay = { playPlaylist(playlist) },
                                    onEdit = {
                                        playlistForEdit = playlist
                                        showPlaylistEditor = true
                                    },
                                    onDelete = {
                                        if (selectedPlaylistId == playlist.id) selectedPlaylistId = null
                                        onDeletePlaylist(playlist)
                                        playlistNotice = "Playlist excluida."
                                    }
                                )
                            }
                        }
                    }
                }

                "playlistSongs" -> {
                    val playlist = playlists.firstOrNull { it.id == selectedPlaylistId }
                    if (playlist == null) {
                        item {
                            EmptyMusicScreen(
                                title = "Playlist nao encontrada",
                                message = "Ela pode ter sido removida."
                            )
                        }
                    } else {
                        val selectedSongs = playlistSongs(playlist)
                        item {
                            PlaylistDetailsHero(
                                playlist = playlist,
                                songCount = selectedSongs.size,
                                onPlay = { playPlaylist(playlist) },
                                onEdit = {
                                    playlistForEdit = playlist
                                    showPlaylistEditor = true
                                },
                                onAddSongs = {
                                    playlistForEdit = playlist
                                    showPlaylistEditor = true
                                },
                                onDelete = {
                                    onDeletePlaylist(playlist)
                                    playlistNotice = "Playlist excluida."
                                    selectedPlaylistId = null
                                    page = "playlists"
                                }
                            )
                        }

                        if (selectedSongs.isEmpty()) {
                            item {
                                EmptyMusicScreen(
                                    title = "Playlist vazia",
                                    message = "Toque em adicionar musicas para escolher faixas do dispositivo."
                                )
                            }
                        } else {
                            items(selectedSongs, key = { it.id }) { music ->
                                EnteringItem {
                                    Column {
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
                                        TextButton(
                                            onClick = {
                                                onRemoveFromPlaylist(music, playlist)
                                                playlistNotice = "Musica removida da playlist."
                                            }
                                        ) {
                                            Text("Remover desta playlist", color = WavePink)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "albums" -> {
                    val albums = songs.groupBy { it.album.ifBlank { "Album desconhecido" } }
                    if (albums.isEmpty()) {
                        item { EmptyMusicScreen("Nenhum album encontrado", "Suas musicas locais aparecerao aqui.") }
                    } else {
                        items(albums.keys.sorted(), key = { it }) { album ->
                            CollectionRow(
                                title = album,
                                subtitle = "${albums[album].orEmpty().size} musicas",
                                icon = Icons.Rounded.Album,
                                colors = listOf(WaveBlue, WavePink),
                                onClick = {
                                    selectedName = album
                                    page = "albumSongs"
                                }
                            )
                        }
                    }
                }

                "albumSongs" -> {
                    val albumSongs = songs.filter { it.album.ifBlank { "Album desconhecido" } == selectedName }
                    songItems(
                        songs = albumSongs,
                        emptyTitle = "Album vazio",
                        emptyMessage = "Nenhuma faixa foi encontrada para esse album.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }

                "artists" -> {
                    val artists = songs.groupBy { it.artist.ifBlank { "Artista desconhecido" } }
                    if (artists.isEmpty()) {
                        item { EmptyMusicScreen("Nenhum artista encontrado", "Suas musicas locais aparecerao aqui.") }
                    } else {
                        items(artists.keys.sorted(), key = { it }) { artist ->
                            CollectionRow(
                                title = artist,
                                subtitle = "${artists[artist].orEmpty().size} musicas",
                                icon = Icons.Rounded.Person,
                                colors = listOf(WavePurple, WaveBlue),
                                onClick = {
                                    selectedName = artist
                                    page = "artistSongs"
                                }
                            )
                        }
                    }
                }

                "artistSongs" -> {
                    val artistSongs = songs.filter { it.artist.ifBlank { "Artista desconhecido" } == selectedName }
                    songItems(
                        songs = artistSongs,
                        emptyTitle = "Artista sem musicas",
                        emptyMessage = "Nenhuma faixa foi encontrada para esse artista.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }

                "folders" -> {
                    val folders = songs.groupBy { it.folder.ifBlank { "Musicas" } }
                    if (folders.isEmpty()) {
                        item { EmptyMusicScreen("Nenhuma pasta encontrada", "As pastas com audio aparecem aqui.") }
                    } else {
                        items(folders.keys.sorted(), key = { it }) { folder ->
                            CollectionRow(
                                title = folder,
                                subtitle = "${folders[folder].orEmpty().size} musicas",
                                icon = Icons.Rounded.Folder,
                                colors = listOf(WaveBlue, WavePurple),
                                onClick = {
                                    selectedName = folder
                                    page = "folderSongs"
                                }
                            )
                        }
                    }
                }

                "folderSongs" -> {
                    val folderSongs = songs.filter { it.folder.ifBlank { "Musicas" } == selectedName }
                    songItems(
                        songs = folderSongs,
                        emptyTitle = "Pasta vazia",
                        emptyMessage = "Nenhuma faixa foi encontrada nessa pasta.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }

                "history" -> {
                    val recentSongs = recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
                    songItems(
                        songs = recentSongs,
                        emptyTitle = "Historico vazio",
                        emptyMessage = "As musicas tocadas recentemente aparecerao aqui.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }

                "stats" -> {
                    item { StatsPanel(songs = songs, playbackStats = playbackStats) }
                    val topSongs = playbackStats.playCounts.entries
                        .sortedByDescending { it.value }
                        .mapNotNull { entry -> songs.firstOrNull { it.id == entry.key } }
                    songItems(
                        songs = topSongs,
                        emptyTitle = "Sem estatisticas ainda",
                        emptyMessage = "Toque algumas musicas para ver seus dados locais.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }
            }
        }
    }

    if (showPlaylistEditor) {
        PlaylistEditorSheet(
            playlist = playlistForEdit,
            songs = songs,
            onDismiss = {
                showPlaylistEditor = false
                playlistForEdit = null
            },
            onSave = { name, description, imageUri, selectedSongIds ->
                val editing = playlistForEdit
                if (editing == null) {
                    onCreatePlaylist(name, description, imageUri, selectedSongIds)
                    playlistNotice = if (selectedSongIds.isEmpty()) {
                        "Playlist criada vazia. Voce pode adicionar musicas depois."
                    } else {
                        "Playlist criada com ${selectedSongIds.size} musicas."
                    }
                    page = "playlists"
                } else {
                    onUpdatePlaylist(editing, name, description, imageUri, selectedSongIds)
                    if (selectedPlaylistId == editing.id) selectedName = name.trim()
                    playlistNotice = if (selectedSongIds.isEmpty()) {
                        "Playlist salva vazia."
                    } else {
                        "Playlist atualizada com ${selectedSongIds.size} musicas."
                    }
                }
                showPlaylistEditor = false
                playlistForEdit = null
            }
        )
    }
}

private fun LazyListScope.songItems(
    songs: List<Music>,
    emptyTitle: String,
    emptyMessage: String,
    currentMusicId: Long?,
    likedIds: Set<Long>,
    playlists: List<Playlist>,
    onSongClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    queuedIds: Set<Long>,
    onAddToQueue: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit
) {
    if (songs.isEmpty()) {
        item { EmptyMusicScreen(emptyTitle, emptyMessage) }
    } else {
        items(songs, key = { it.id }) { music ->
            EnteringItem {
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
    }
}

@Composable
private fun EnteringItem(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 5 })
    ) {
        content()
    }
}

@Composable
private fun LibraryHeader(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            AnimatedIconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Voltar",
                    tint = WaveTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibrarySectionCard(
    section: LibrarySection,
    onClick: () -> Unit
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = WaveSurface.copy(alpha = 0.78f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIcon(
                icon = section.icon,
                colors = section.colors,
                contentDescription = section.title
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = section.subtitle,
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
private fun PlaylistHero(
    playlistCount: Int,
    onCreateClick: () -> Unit
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = WaveSurface.copy(alpha = 0.76f),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientIcon(
                    icon = Icons.Rounded.LibraryMusic,
                    colors = listOf(WavePurple, WavePink, WaveBlue),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sua colecao premium",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$playlistCount playlists com capas, descricoes e musicas locais.",
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Criar playlist")
            }
        }
    }
}

@Composable
private fun EmptyPlaylistState(onCreateClick: () -> Unit) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = WaveSurface.copy(alpha = 0.72f),
        contentPadding = PaddingValues(22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WavePurple, WavePink, WaveBlue))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhuma playlist criada",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Crie uma playlist com capa personalizada e escolha suas musicas favoritas.",
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Criar playlist")
            }
        }
    }
}

@Composable
private fun PlaylistDetailsHero(
    playlist: Playlist,
    songCount: Int,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onAddSongs: () -> Unit,
    onDelete: () -> Unit
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = WaveSurface.copy(alpha = 0.8f),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column {
            PlaylistCover(
                imageUri = playlist.imageUri,
                seed = playlist.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                cornerRadius = 28.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = playlist.name,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (playlist.description.isNotBlank()) {
                Text(
                    text = playlist.description,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "$songCount musicas",
                color = WaveBlue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlay,
                    enabled = songCount > 0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Tocar")
                }
                Button(
                    onClick = onAddSongs,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Adicionar")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = WaveBlue)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Editar", color = WaveBlue)
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = WavePink)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Excluir", color = WavePink)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistEditorSheet(
    playlist: Playlist?,
    songs: List<Music>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, List<Long>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val draftId = remember(playlist?.id) { playlist?.id ?: System.currentTimeMillis() }
    var name by remember(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }
    var description by remember(playlist?.id) { mutableStateOf(playlist?.description.orEmpty()) }
    var imageUri by remember(playlist?.id) { mutableStateOf(playlist?.imageUri) }
    var selectedIds by remember(playlist?.id) { mutableStateOf(playlist?.songIds?.toSet().orEmpty()) }
    var showNameError by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WaveSurface,
        contentColor = WaveTextPrimary,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp)
        ) {
            Text(
                text = if (playlist == null) "Nova playlist" else "Editar playlist",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Escolha capa, nome, descricao e musicas.",
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(18.dp))

            PlaylistImagePicker(
                playlistId = draftId,
                imageUri = imageUri,
                onImageChanged = { imageUri = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    showNameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nome da playlist") },
                singleLine = true,
                isError = showNameError,
                supportingText = {
                    if (showNameError) Text("Informe um nome para salvar.")
                },
                shape = RoundedCornerShape(20.dp),
                colors = playlistTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Descricao opcional") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = playlistTextFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Musicas da playlist",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (selectedIds.isEmpty()) {
                    "Nenhuma selecionada. Voce pode salvar vazia."
                } else {
                    "${selectedIds.size} musicas selecionadas"
                },
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (songs.isEmpty()) {
                EmptyMusicScreen(
                    title = "Nenhuma musica disponivel",
                    message = "Quando houver arquivos de audio no dispositivo, eles aparecerao aqui."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(songs, key = { it.id }) { music ->
                        SelectableSongRow(
                            music = music,
                            selected = music.id in selectedIds,
                            onToggle = {
                                selectedIds = if (music.id in selectedIds) {
                                    selectedIds - music.id
                                } else {
                                    selectedIds + music.id
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = WaveTextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.trim().isBlank()) {
                            showNameError = true
                        } else {
                            onSave(name.trim(), description.trim(), imageUri, selectedIds.toList())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (selectedIds.isEmpty()) "Salvar vazia" else "Salvar playlist")
                }
            }
        }
    }
}

@Composable
private fun SelectableSongRow(
    music: Music,
    selected: Boolean,
    onToggle: () -> Unit
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) WavePurple.copy(alpha = 0.24f) else WaveSurfaceBright.copy(alpha = 0.36f),
        pressedScale = 0.985f,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = WavePink,
                    uncheckedColor = WaveTextSecondary,
                    checkmarkColor = WaveTextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            AlbumArtwork(
                music = music,
                modifier = Modifier.size(46.dp),
                cornerRadius = 14.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.artist,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = music.duration,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
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
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = WaveSurface.copy(alpha = 0.78f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientIcon(icon = icon, colors = colors, contentDescription = title)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
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
private fun EmptyMusicScreen(
    title: String,
    message: String
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = WaveSurface.copy(alpha = 0.7f),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GradientIcon(
                icon = Icons.Rounded.LibraryMusic,
                colors = listOf(WavePurple, WaveBlue),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = message,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun NoticeCard(
    message: String,
    onDismiss: () -> Unit
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = WaveSurfaceBright.copy(alpha = 0.58f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Ok", color = WaveBlue)
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

@Composable
private fun StatsPanel(
    songs: List<Music>,
    playbackStats: PlaybackStats
) {
    val topArtist = playbackStats.playCounts.entries
        .sortedByDescending { it.value }
        .mapNotNull { entry -> songs.firstOrNull { it.id == entry.key } }
        .groupingBy { it.artist }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: "Ainda sem dados"
    val minutes = playbackStats.totalListenMs / 60000L

    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = WaveSurface.copy(alpha = 0.78f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text(
                text = "Resumo local",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatLine("Musicas tocadas", playbackStats.playCounts.values.sum().toString())
            StatLine("Tempo ouvindo", if (minutes < 60) "${minutes} min" else "${minutes / 60} h ${minutes % 60} min")
            StatLine("Artista favorito", topArtist)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WaveTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = WaveTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun playlistTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WaveTextPrimary,
    unfocusedTextColor = WaveTextPrimary,
    focusedBorderColor = WaveBlue,
    unfocusedBorderColor = WaveSurfaceBright,
    errorBorderColor = WavePink,
    focusedContainerColor = WaveSurfaceBright.copy(alpha = 0.3f),
    unfocusedContainerColor = WaveSurfaceBright.copy(alpha = 0.24f),
    errorContainerColor = WaveSurfaceBright.copy(alpha = 0.24f),
    cursorColor = WaveBlue,
    focusedPlaceholderColor = WaveTextSecondary,
    unfocusedPlaceholderColor = WaveTextSecondary
)

private fun titleForPage(page: String, selectedName: String): String {
    return when (page) {
        "liked" -> "Musicas curtidas"
        "playlists" -> "Playlists"
        "playlistSongs" -> selectedName
        "albums" -> "Albuns"
        "albumSongs" -> selectedName
        "artists" -> "Artistas"
        "artistSongs" -> selectedName
        "folders" -> "Pastas"
        "folderSongs" -> selectedName
        "history" -> "Historico"
        "stats" -> "Estatisticas"
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
        "liked" -> "${likedIds.size} musicas curtidas"
        "playlists" -> "${playlists.size} playlists criadas"
        "playlistSongs" -> "Capa, descricao e faixas da playlist"
        "albums", "albumSongs" -> "${songs.map { it.album }.distinct().size} albuns no dispositivo"
        "artists", "artistSongs" -> "${songs.map { it.artist }.distinct().size} artistas encontrados"
        "folders", "folderSongs" -> "${songs.map { it.folder }.distinct().size} pastas com audio"
        "history" -> "Musicas que voce tocou recentemente"
        "stats" -> "Seus habitos salvos localmente"
        else -> "${songs.size} musicas carregadas do dispositivo"
    }
}
