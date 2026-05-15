package com.wavemusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.model.Music
import com.wavemusic.player.data.model.PlaybackStats
import com.wavemusic.player.data.model.Playlist
import com.wavemusic.player.domain.usecase.playableSongsFor
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.components.AnimatedCard
import com.wavemusic.player.ui.components.AnimatedIconButton
import com.wavemusic.player.ui.components.MusicIconCluster
import com.wavemusic.player.ui.components.MusicListItem
import com.wavemusic.player.ui.components.NeonIconOrb
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
    onPlaySongList: (List<Music>, Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onCreatePlaylist: (String, String, String?, List<Long>) -> Unit,
    onUpdatePlaylist: (Playlist, String, String, String?, List<Long>) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onRemoveFromPlaylist: (Music, Playlist) -> Unit,
    onAddToQueue: (Music) -> Unit,
    onPlayNext: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit,
    onCreatePlaylistFromFolder: (String) -> Unit = {},
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
                title = "Playlists inteligentes",
                subtitle = "Listas automaticas por habito",
                icon = Icons.Rounded.GraphicEq,
                colors = listOf(WaveBlue, WavePink),
                target = "smart"
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
            "smartSongs" -> "smart"
            else -> "root"
        }
        if (page != "playlistSongs") selectedPlaylistId = null
        selectedName = ""
    }

    fun playlistSongs(playlist: Playlist): List<Music> {
        return playableSongsFor(playlist, songs)
    }

    fun playPlaylist(playlist: Playlist) {
        val selectedSongs = playlistSongs(playlist)
        val firstSong = selectedSongs.firstOrNull()
        if (firstSong == null) {
            playlistNotice = "Essa playlist ainda esta vazia."
            return
        }
        onPlaySongList(selectedSongs, firstSong)
        playlistNotice = "Tocando ${playlist.name} na ordem da playlist."
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState != "root" && initialState == "root"
            val direction = if (forward) 1 else -1
            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it * direction / 5 }) togetherWith
                (fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it * direction / 6 })
        },
        label = "library-page-slide-transition",
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
                    item {
                        LibraryConstellationPanel(
                            sections = sections,
                            songsCount = songs.size,
                            playlistCount = playlists.size,
                            likedCount = likedIds.size,
                            recentCount = recentIds.size,
                            onSectionClick = { page = it.target }
                        )
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
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
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
                                    MusicListItem(
                                        music = music,
                                        isCurrent = music.id == currentMusicId,
                                        isLiked = music.id in likedIds,
                                        playlists = playlists,
                                        onClick = { selected -> onPlaySongList(selectedSongs, selected) },
                                        onToggleLike = onToggleLike,
                                        onAddToPlaylist = onAddToPlaylist,
                                        isQueued = music.id in queuedIds,
                                        onAddToQueue = onAddToQueue,
                                        onPlayNext = onPlayNext,
                                        onRemoveFromQueue = onRemoveFromQueue,
                                        onRemoveFromPlaylist = {
                                            onRemoveFromPlaylist(music, playlist)
                                            playlistNotice = "Musica removida da playlist."
                                        }
                                    )
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
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
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
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
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
                    if (folderSongs.isNotEmpty()) {
                        item {
                            Button(
                                onClick = { onCreatePlaylistFromFolder(selectedName) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WaveBlue),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = WaveTextPrimary)
                                Spacer(Modifier.size(8.dp))
                                Text("Criar playlist desta pasta")
                            }
                        }
                    }
                    songItems(
                        songs = folderSongs,
                        emptyTitle = "Pasta vazia",
                        emptyMessage = "Nenhuma faixa foi encontrada nessa pasta.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
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
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
                        onRemoveFromQueue = onRemoveFromQueue
                    )
                }

                "smart" -> {
                    val smartRows = listOf(
                        "mostPlayed" to Triple("Mais tocadas", "Ordenadas pelo seu historico", Icons.Rounded.GraphicEq),
                        "recentlyAdded" to Triple("Adicionadas recentemente", "Novidades do dispositivo", Icons.Rounded.Save),
                        "neverPlayed" to Triple("Nunca tocadas", "Faixas ainda sem reproducao", Icons.Rounded.MusicNote),
                        "longTracks" to Triple("Faixas longas", "Musicas e sets acima de 7 minutos", Icons.Rounded.QueryStats)
                    )
                    items(smartRows, key = { it.first }) { (key, info) ->
                        val smartSongs = smartSongsFor(key, songs, playbackStats)
                        CollectionRow(
                            title = info.first,
                            subtitle = "${smartSongs.size} itens",
                            icon = info.third,
                            colors = listOf(WaveBlue, WavePink),
                            onClick = {
                                selectedName = key
                                page = "smartSongs"
                            }
                        )
                    }
                }

                "smartSongs" -> {
                    val smartSongs = smartSongsFor(selectedName, songs, playbackStats)
                    songItems(
                        songs = smartSongs,
                        emptyTitle = "Lista vazia",
                        emptyMessage = "Esta playlist inteligente ainda nao tem faixas suficientes.",
                        currentMusicId = currentMusicId,
                        likedIds = likedIds,
                        playlists = playlists,
                        onSongClick = onSongClick,
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
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
                        onPlaySongList = onPlaySongList,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        queuedIds = queuedIds,
                        onAddToQueue = onAddToQueue,
                        onPlayNext = onPlayNext,
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
    onPlaySongList: (List<Music>, Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    queuedIds: Set<Long>,
    onAddToQueue: (Music) -> Unit,
    onPlayNext: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit
) {
    if (songs.isEmpty()) {
        item { EmptyMusicScreen(emptyTitle, emptyMessage) }
    } else {
        items(songs, key = { it.id }) { music ->
            EnteringItem {
                MusicListItem(
                    music = music,
                    isCurrent = music.id == currentMusicId,
                    isLiked = music.id in likedIds,
                    playlists = playlists,
                    onClick = { selected -> onPlaySongList(songs, selected) },
                    onToggleLike = onToggleLike,
                    onAddToPlaylist = onAddToPlaylist,
                    isQueued = music.id in queuedIds,
                    onAddToQueue = onAddToQueue,
                    onPlayNext = onPlayNext,
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
private fun LibraryConstellationPanel(
    sections: List<LibrarySection>,
    songsCount: Int,
    playlistCount: Int,
    likedCount: Int,
    recentCount: Int,
    onSectionClick: (LibrarySection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            WaveSurfaceBright.copy(alpha = 0.44f),
                            WavePurple.copy(alpha = 0.10f),
                            WaveSurface.copy(alpha = 0.82f)
                        )
                    )
                )
                .border(1.dp, WaveBlue.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(WaveSurfaceBright.copy(alpha = 0.36f))
                        .border(1.dp, WaveBlue.copy(alpha = 0.22f), RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = songsCount.toString(),
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "itens",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biblioteca",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Um mapa compacto para navegar por tudo sem poluir a tela.",
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CleanMetricChip(value = playlistCount.toString(), label = "playlists", modifier = Modifier.weight(1f))
            CleanMetricChip(value = likedCount.toString(), label = "curtidas", modifier = Modifier.weight(1f))
            CleanMetricChip(value = recentCount.toString(), label = "recentes", modifier = Modifier.weight(1f))
        }

        sections.forEachIndexed { index, section ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index % 2 == 1) {
                    Spacer(modifier = Modifier.weight(0.16f))
                }
                LibraryPortal(
                    section = section,
                    index = index + 1,
                    compact = index % 3 != 0,
                    onClick = { onSectionClick(section) },
                    modifier = Modifier.weight(1f)
                )
                if (index % 2 == 0) {
                    Spacer(modifier = Modifier.weight(0.16f))
                }
            }
        }
    }
}

@Composable
private fun CleanMetricChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WaveSurfaceBright.copy(alpha = 0.26f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = WaveTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, color = WaveTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibraryPortal(
    section: LibrarySection,
    index: Int,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(if (compact) 88.dp else 106.dp)
            .clip(shape)
            .background(WaveSurfaceBright.copy(alpha = 0.24f))
            .border(1.dp, section.colors.last().copy(alpha = 0.16f), shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            section.colors.first().copy(alpha = 0.18f),
                            WaveSurface.copy(alpha = 0.74f)
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 42.dp else 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(section.colors.first().copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString().padStart(2, '0'),
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = section.title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = section.subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = section.colors.last().copy(alpha = 0.72f),
                modifier = Modifier.size(if (compact) 25.dp else 30.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = WaveTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LibraryOverviewPanel(
    songsCount: Int,
    playlistCount: Int,
    likedCount: Int,
    recentCount: Int
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = WaveSurface.copy(alpha = 0.78f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            WavePurple.copy(alpha = 0.34f),
                            WaveSurface.copy(alpha = 0.92f),
                            WaveBlue.copy(alpha = 0.22f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WAVE LIBRARY",
                            color = WaveBlue,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Sua musica organizada em neon",
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$songsCount faixas locais prontas para tocar, favoritar e organizar.",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        MusicIconCluster(
                            modifier = Modifier.padding(top = 12.dp),
                            icons = listOf(
                                Icons.Rounded.LibraryMusic,
                                Icons.Rounded.Favorite,
                                Icons.Rounded.GraphicEq
                            ),
                            colors = listOf(WaveBlue, WavePink, WavePurple)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(WavePink, WavePurple, WaveBlue)))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = WaveTextPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryMetricChip(
                        value = playlistCount.toString(),
                        label = "playlists",
                        colors = listOf(WavePurple, WaveBlue),
                        modifier = Modifier.weight(1f)
                    )
                    LibraryMetricChip(
                        value = likedCount.toString(),
                        label = "curtidas",
                        colors = listOf(WavePink, WavePurple),
                        modifier = Modifier.weight(1f)
                    )
                    LibraryMetricChip(
                        value = recentCount.toString(),
                        label = "recentes",
                        colors = listOf(WaveBlue, WavePink),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryMetricChip(
    value: String,
    label: String,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(WaveSurfaceBright.copy(alpha = 0.34f))
            .border(
                width = 1.dp,
                color = colors.first().copy(alpha = 0.28f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = colors.first(),
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
private fun LibraryHeader(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit
) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = WaveSurface.copy(alpha = 0.58f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                AnimatedIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    background = Brush.linearGradient(
                        listOf(WaveSurfaceBright.copy(alpha = 0.9f), WaveSurface.copy(alpha = 0.6f))
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Voltar",
                        tint = WaveTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
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
        color = WaveSurface.copy(alpha = 0.72f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(section.colors.first().copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = section.subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MusicIconCluster(
                    modifier = Modifier.padding(top = 8.dp),
                    icons = listOf(section.icon, Icons.Rounded.MusicNote),
                    colors = section.colors
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = section.colors.last(),
                modifier = Modifier.size(26.dp)
            )
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
        shape = RoundedCornerShape(34.dp),
        color = WaveSurface.copy(alpha = 0.8f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            WavePurple.copy(alpha = 0.36f),
                            WaveSurface.copy(alpha = 0.96f),
                            WavePink.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PLAYLISTS",
                            color = WavePink,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Colecoes com capa e energia propria",
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$playlistCount playlists criadas no Wave Music.",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        MusicIconCluster(
                            modifier = Modifier.padding(top = 12.dp),
                            icons = listOf(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                Icons.Rounded.PlayArrow,
                                Icons.Rounded.GraphicEq
                            ),
                            colors = listOf(WavePink, WavePurple, WaveBlue)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(96.dp)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(58.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(WaveBlue, WavePurple)))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(66.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Brush.linearGradient(listOf(WavePink, WavePurple)))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = WaveTextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onCreateClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Criar playlist")
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistState(onCreateClick: () -> Unit) {
    AnimatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = WaveSurface.copy(alpha = 0.72f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(WaveBlue.copy(alpha = 0.18f), WaveSurface.copy(alpha = 0.92f))
                    )
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Brush.linearGradient(listOf(WavePurple, WavePink, WaveBlue)))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(34.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Comece uma colecao",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Crie uma playlist bonita, escolha uma capa e separe suas faixas favoritas.",
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )
            Button(
                onClick = onCreateClick,
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
        shape = RoundedCornerShape(36.dp),
        color = WaveSurface.copy(alpha = 0.84f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            WavePink.copy(alpha = 0.24f),
                            WaveSurface.copy(alpha = 0.96f),
                            WaveBlue.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Box {
                PlaylistCover(
                    imageUri = playlist.imageUri,
                    seed = playlist.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    cornerRadius = 30.dp
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(WaveSurface.copy(alpha = 0.68f))
                        .border(
                            width = 1.dp,
                            color = WaveBlue.copy(alpha = 0.32f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "PLAYLIST",
                        color = WaveBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
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
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    MusicIconCluster(
                        modifier = Modifier.padding(top = 10.dp),
                        icons = listOf(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            Icons.Rounded.MusicNote,
                            Icons.Rounded.PlayArrow
                        ),
                        colors = listOf(WavePink, WaveBlue, WavePurple)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(WaveSurfaceBright.copy(alpha = 0.44f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = songCount.toString(),
                            color = WavePink,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "faixas",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
    var songQuery by rememberSaveable(playlist?.id) { mutableStateOf("") }
    var showNameError by rememberSaveable { mutableStateOf(false) }
    val filteredSongs = remember(songs, songQuery) {
        val query = songQuery.trim()
        songs
            .sortedByDescending { it.dateAddedSeconds }
            .filter { music ->
                query.isBlank() ||
                    music.title.contains(query, ignoreCase = true) ||
                    music.artist.contains(query, ignoreCase = true) ||
                    music.album.contains(query, ignoreCase = true) ||
                    music.folder.contains(query, ignoreCase = true)
            }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                WavePurple.copy(alpha = 0.42f),
                                WaveSurfaceBright.copy(alpha = 0.42f),
                                WavePink.copy(alpha = 0.24f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = WaveBlue.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientIcon(
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        colors = listOf(WavePink, WavePurple, WaveBlue),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (playlist == null) "Nova playlist" else "Editar playlist",
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Capa, nome e musicas em um fluxo so.",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Capa e detalhes",
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Musicas da playlist",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (selectedIds.isEmpty()) {
                            "Salve vazia ou toque nas faixas para selecionar."
                        } else {
                            "Toque novamente para remover da selecao."
                        },
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(WaveSurfaceBright.copy(alpha = 0.48f))
                        .border(
                            width = 1.dp,
                            color = WavePink.copy(alpha = 0.26f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${selectedIds.size} selecionadas",
                        color = WavePink,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = songQuery,
                onValueChange = { songQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar musica ou video") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Pesquisar"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = playlistTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (songs.isEmpty()) {
                EmptyMusicScreen(
                    title = "Nenhuma musica disponivel",
                    message = "Quando houver arquivos de audio no dispositivo, eles aparecerao aqui."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredSongs.isEmpty()) {
                        item {
                            EmptyMusicScreen(
                                title = "Nenhum resultado encontrado",
                                message = "Tente pesquisar outro nome, artista, album ou pasta."
                            )
                        }
                    }
                    items(filteredSongs, key = { it.id }) { music ->
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
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.8f)
                ) {
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
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        when {
                            playlist == null -> "Criar playlist"
                            selectedIds.isEmpty() -> "Salvar vazia"
                            else -> "Salvar playlist"
                        }
                    )
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
    val shape = RoundedCornerShape(22.dp)
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "playlist-song-selected-scale"
    )
    AnimatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
                shadowElevation = if (selected) 16f else 6f
            }
            .border(
                width = 1.dp,
                color = if (selected) WavePink.copy(alpha = 0.42f) else WaveSurfaceBright.copy(alpha = 0.18f),
                shape = shape
            ),
        onClick = onToggle,
        shape = shape,
        color = if (selected) WavePurple.copy(alpha = 0.28f) else WaveSurfaceBright.copy(alpha = 0.32f),
        pressedScale = 0.985f,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.graphicsLayer {
                    scaleX = if (selected) 1.12f else 1f
                    scaleY = if (selected) 1.12f else 1f
                },
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
                Spacer(modifier = Modifier.height(2.dp))
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
                color = if (selected) WaveBlue else WaveTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
        shape = RoundedCornerShape(26.dp),
        color = WaveSurface.copy(alpha = 0.72f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(colors.first().copy(alpha = 0.14f), Color.Transparent)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.last(),
                modifier = Modifier.size(26.dp)
            )
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
        shape = RoundedCornerShape(30.dp),
        color = WaveSurface.copy(alpha = 0.7f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(WavePurple.copy(alpha = 0.16f), Color.Transparent)))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
        color = WaveSurfaceBright.copy(alpha = 0.62f),
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
    NeonIconOrb(
        icon = icon,
        contentDescription = contentDescription,
        size = 58.dp,
        iconSize = 28.dp,
        colors = colors,
        active = true
    )
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
        shape = RoundedCornerShape(30.dp),
        color = WaveSurface.copy(alpha = 0.78f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(WaveBlue.copy(alpha = 0.16f), Color.Transparent)))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientIcon(
                    icon = Icons.Rounded.QueryStats,
                    colors = listOf(WaveBlue, WavePurple),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Resumo local",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Dados salvos no dispositivo",
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
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
        "smart" -> "Playlists inteligentes"
        "smartSongs" -> smartTitle(selectedName)
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
        "smart" -> "Listas automaticas atualizadas pelo uso"
        "smartSongs" -> "Playlist inteligente"
        "albums", "albumSongs" -> "${songs.map { it.album }.distinct().size} albuns no dispositivo"
        "artists", "artistSongs" -> "${songs.map { it.artist }.distinct().size} artistas encontrados"
        "folders", "folderSongs" -> "${songs.map { it.folder }.distinct().size} pastas com audio"
        "history" -> "Musicas que voce tocou recentemente"
        "stats" -> "Seus habitos salvos localmente"
        else -> "${songs.size} musicas carregadas do dispositivo"
    }
}

private fun smartSongsFor(
    key: String,
    songs: List<Music>,
    playbackStats: PlaybackStats
): List<Music> {
    return when (key) {
        "mostPlayed" -> playbackStats.playCounts.entries
            .sortedByDescending { it.value }
            .mapNotNull { entry -> songs.firstOrNull { it.id == entry.key } }
        "recentlyAdded" -> songs.sortedByDescending { it.dateAddedSeconds }.take(50)
        "neverPlayed" -> songs.filter { it.id !in playbackStats.playCounts.keys }
            .sortedByDescending { it.dateAddedSeconds }
        "longTracks" -> songs.filter { it.durationMs >= 7 * 60_000L }
            .sortedByDescending { it.durationMs }
        else -> emptyList()
    }
}

private fun smartTitle(key: String): String {
    return when (key) {
        "mostPlayed" -> "Mais tocadas"
        "recentlyAdded" -> "Adicionadas recentemente"
        "neverPlayed" -> "Nunca tocadas"
        "longTracks" -> "Faixas longas"
        else -> "Playlist inteligente"
    }
}

