package com.wavemusic.player

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.LibraryStorage
import com.wavemusic.player.data.MusicStore
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.components.BottomNavigationBar
import com.wavemusic.player.ui.components.MiniPlayer
import com.wavemusic.player.ui.components.WaveTab
import com.wavemusic.player.ui.screens.HomeScreen
import com.wavemusic.player.ui.screens.LibraryScreen
import com.wavemusic.player.ui.screens.NowPlayingScreen
import com.wavemusic.player.ui.screens.SearchScreen
import com.wavemusic.player.ui.screens.SettingsScreen
import com.wavemusic.player.ui.theme.WaveBackground
import com.wavemusic.player.ui.theme.WaveMusicTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaveMusicTheme {
                WaveMusicApp()
            }
        }
    }
}

@Composable
private fun WaveMusicApp() {
    val context = LocalContext.current
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    var songs by remember { mutableStateOf<List<Music>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableStateOf(WaveTab.Home) }
    var currentMusic by remember { mutableStateOf<Music?>(null) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    val mediaPlayer = remember { MediaPlayer() }
    val libraryStorage = remember { LibraryStorage(context.applicationContext) }
    var likedIds by remember { mutableStateOf(libraryStorage.loadLikedIds()) }
    var playlists by remember { mutableStateOf(libraryStorage.loadPlaylists()) }

    fun reloadSongs() {
        songs = if (hasPermission) MusicStore.loadDeviceSongs(context) else emptyList()
        if (currentMusic == null) {
            currentMusic = songs.firstOrNull()
        }
    }

    fun playMusic(music: Music) {
        runCatching {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, music.uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
            currentMusic = music
            durationMs = mediaPlayer.duration.toLong().takeIf { it > 0 } ?: music.durationMs
            positionMs = 0L
            isPlaying = true
        }.onFailure {
            isPlaying = false
        }
    }

    fun playNext() {
        if (songs.isEmpty()) return
        val currentIndex = songs.indexOfFirst { it.id == currentMusic?.id }.coerceAtLeast(0)
        playMusic(songs[(currentIndex + 1) % songs.size])
    }

    fun playPrevious() {
        if (songs.isEmpty()) return
        val currentIndex = songs.indexOfFirst { it.id == currentMusic?.id }.coerceAtLeast(0)
        playMusic(songs[(currentIndex - 1 + songs.size) % songs.size])
    }

    fun togglePlayPause() {
        val music = currentMusic ?: songs.firstOrNull() ?: return
        if (currentMusic?.id != music.id || durationMs == 0L) {
            playMusic(music)
            return
        }
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        } else {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    fun seekTo(progress: Float) {
        if (durationMs <= 0L) return
        val newPosition = (durationMs * progress.coerceIn(0f, 1f)).toLong()
        mediaPlayer.seekTo(newPosition.toInt())
        positionMs = newPosition
    }

    fun toggleLike(music: Music) {
        likedIds = if (music.id in likedIds) {
            likedIds - music.id
        } else {
            likedIds + music.id
        }
        libraryStorage.saveLikedIds(likedIds)
    }

    fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        playlists = playlists + Playlist(
            id = System.currentTimeMillis(),
            name = cleanName,
            songIds = emptyList()
        )
        libraryStorage.savePlaylists(playlists)
    }

    fun deletePlaylist(playlist: Playlist) {
        playlists = playlists.filterNot { it.id == playlist.id }
        libraryStorage.savePlaylists(playlists)
    }

    fun addToPlaylist(music: Music, playlist: Playlist) {
        playlists = playlists.map {
            if (it.id == playlist.id && music.id !in it.songIds) {
                it.copy(songIds = it.songIds + music.id)
            } else {
                it
            }
        }
        libraryStorage.savePlaylists(playlists)
    }

    fun removeFromPlaylist(music: Music, playlist: Playlist) {
        playlists = playlists.map {
            if (it.id == playlist.id) {
                it.copy(songIds = it.songIds - music.id)
            } else {
                it
            }
        }
        libraryStorage.savePlaylists(playlists)
    }

    LaunchedEffect(hasPermission) {
        reloadSongs()
    }

    LaunchedEffect(mediaPlayer, songs, currentMusic) {
        mediaPlayer.setOnCompletionListener {
            playNext()
        }
    }

    LaunchedEffect(isPlaying, currentMusic?.id) {
        while (isPlaying) {
            delay(500)
            runCatching {
                positionMs = mediaPlayer.currentPosition.toLong()
                if (mediaPlayer.duration > 0) durationMs = mediaPlayer.duration.toLong()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF11142A),
                        WaveBackground,
                        Color(0xFF140A26)
                    )
                )
            )
    ) {
        val selectedMusic = currentMusic
        if (showNowPlaying && selectedMusic != null) {
            NowPlayingScreen(
                music = selectedMusic,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs.takeIf { it > 0 } ?: selectedMusic.durationMs,
                onSeek = ::seekTo,
                onBack = { showNowPlaying = false },
                onPlayPause = ::togglePlayPause,
                onPrevious = ::playPrevious,
                onNext = ::playNext
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Column {
                        MiniPlayer(
                            music = selectedMusic,
                            isPlaying = isPlaying,
                            progress = if (durationMs > 0) {
                                positionMs.toFloat() / durationMs.toFloat()
                            } else {
                                0f
                            },
                            onPlayPause = ::togglePlayPause,
                            onNext = ::playNext,
                            onClick = { showNowPlaying = true }
                        )
                        BottomNavigationBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                }
            ) { innerPadding ->
                Crossfade(
                    targetState = selectedTab,
                    label = "tab-content",
                    modifier = Modifier.padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        WaveTab.Home -> HomeScreen(
                            songs = songs,
                            currentMusicId = selectedMusic?.id,
                            likedIds = likedIds,
                            playlists = playlists,
                            hasPermission = hasPermission,
                            onRequestPermission = { permissionLauncher.launch(audioPermission) },
                            onRefresh = ::reloadSongs,
                            onSongClick = ::playMusic,
                            onToggleLike = ::toggleLike,
                            onAddToPlaylist = ::addToPlaylist
                        )

                        WaveTab.Search -> SearchScreen(
                            songs = songs,
                            currentMusicId = selectedMusic?.id,
                            likedIds = likedIds,
                            playlists = playlists,
                            onSongClick = ::playMusic,
                            onToggleLike = ::toggleLike,
                            onAddToPlaylist = ::addToPlaylist
                        )

                        WaveTab.Library -> LibraryScreen(
                            songs = songs,
                            currentMusicId = selectedMusic?.id,
                            likedIds = likedIds,
                            playlists = playlists,
                            onSongClick = ::playMusic,
                            onToggleLike = ::toggleLike,
                            onCreatePlaylist = ::createPlaylist,
                            onDeletePlaylist = ::deletePlaylist,
                            onAddToPlaylist = ::addToPlaylist,
                            onRemoveFromPlaylist = ::removeFromPlaylist
                        )
                        WaveTab.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}
