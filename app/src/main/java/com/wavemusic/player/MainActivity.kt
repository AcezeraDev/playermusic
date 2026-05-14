package com.wavemusic.player

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.wavemusic.player.data.AccentTheme
import com.wavemusic.player.data.AudioQuality
import com.wavemusic.player.data.EqualizerPreset
import com.wavemusic.player.data.LibraryStorage
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.MusicStore
import com.wavemusic.player.data.PlaybackStats
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.data.SleepTimerOption
import com.wavemusic.player.data.UserPreferences
import com.wavemusic.player.notifications.MusicNotificationReceiver
import com.wavemusic.player.notifications.WaveMusicNotificationController
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
import com.wavemusic.player.widgets.WaveMusicWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
    val userPreferences = remember { UserPreferences(context.applicationContext) }
    val libraryStorage = remember { LibraryStorage(context.applicationContext) }
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
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var audioQuality by remember { mutableStateOf(userPreferences.loadAudioQuality()) }
    var equalizerPreset by remember { mutableStateOf(userPreferences.loadEqualizerPreset()) }
    var accentTheme by remember { mutableStateOf(userPreferences.loadAccentTheme()) }
    var crossfadeEnabled by remember { mutableStateOf(userPreferences.loadCrossfadeEnabled()) }
    var continueListeningEnabled by remember { mutableStateOf(userPreferences.loadContinueListeningEnabled()) }
    var notificationsEnabled by remember {
        mutableStateOf(userPreferences.loadMediaNotificationsEnabled() && hasNotificationPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
            notificationsEnabled = granted
            userPreferences.saveMediaNotificationsEnabled(granted)
            Toast.makeText(
                context,
                if (granted) "Controles de mídia ativados." else "Permissão negada para notificações.",
                Toast.LENGTH_SHORT
            ).show()
        }
    )

    var songs by remember { mutableStateOf<List<Music>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableStateOf(WaveTab.Home) }
    var currentMusic by remember { mutableStateOf<Music?>(null) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isLoadingSongs by remember { mutableStateOf(false) }
    var likedIds by remember { mutableStateOf(libraryStorage.loadLikedIds()) }
    var playlists by remember { mutableStateOf(libraryStorage.loadPlaylists()) }
    var queueIds by remember { mutableStateOf(libraryStorage.loadQueueIds()) }
    var recentIds by remember { mutableStateOf(libraryStorage.loadRecentIds()) }
    var playbackStats by remember { mutableStateOf(libraryStorage.loadPlaybackStats()) }
    var repeatEnabled by rememberSaveable { mutableStateOf(false) }
    var shuffleEnabled by rememberSaveable { mutableStateOf(false) }
    var sleepTimerEndAtMs by remember { mutableLongStateOf(0L) }
    var sleepTimerLabel by rememberSaveable { mutableStateOf<String?>(null) }

    val mediaPlayer = remember { MediaPlayer() }
    var equalizer by remember { mutableStateOf<Equalizer?>(null) }
    val notificationController = remember { WaveMusicNotificationController(context.applicationContext) }
    val mediaSession = remember { MediaSession(context.applicationContext, "WaveMusicSession") }
    val coroutineScope = rememberCoroutineScope()

    fun queueSongs(): List<Music> = queueIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }

    fun saveQueue(ids: List<Long>) {
        queueIds = ids
        libraryStorage.saveQueueIds(ids)
    }

    fun recordPlay(music: Music) {
        recentIds = (listOf(music.id) + recentIds.filterNot { it == music.id }).take(30)
        libraryStorage.saveRecentIds(recentIds)
        playbackStats = playbackStats.copy(
            playCounts = playbackStats.playCounts + (music.id to ((playbackStats.playCounts[music.id] ?: 0) + 1))
        )
        libraryStorage.savePlaybackStats(playbackStats)
    }

    fun applyEqualizerPreset(preset: EqualizerPreset = equalizerPreset) {
        val activeEqualizer = equalizer ?: return
        runCatching {
            val range = activeEqualizer.bandLevelRange
            val min = range[0]
            val max = range[1]
            val bandCount = activeEqualizer.numberOfBands.toInt().coerceAtLeast(1)
            repeat(bandCount) { band ->
                val sourceIndex = ((band.toFloat() / bandCount.toFloat()) * preset.bandGains.size)
                    .toInt()
                    .coerceIn(0, preset.bandGains.lastIndex)
                val level = preset.bandGains[sourceIndex].coerceIn(min, max)
                activeEqualizer.setBandLevel(band.toShort(), level)
            }
            activeEqualizer.enabled = true
        }
    }

    fun rebuildEqualizer() {
        runCatching {
            equalizer?.release()
            equalizer = Equalizer(0, mediaPlayer.audioSessionId)
            applyEqualizerPreset()
        }.onFailure {
            equalizer = null
        }
    }

    fun playMusic(music: Music) {
        runCatching {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, music.uri)
            mediaPlayer.prepare()
            rebuildEqualizer()
            if (crossfadeEnabled) mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.start()
            currentMusic = music
            durationMs = mediaPlayer.duration.toLong().takeIf { it > 0 } ?: music.durationMs
            positionMs = 0L
            isPlaying = true
            recordPlay(music)

            if (crossfadeEnabled) {
                coroutineScope.launch {
                    repeat(10) { step ->
                        val volume = (step + 1) / 10f
                        runCatching { mediaPlayer.setVolume(volume, volume) }
                        delay(45)
                    }
                }
            }
        }.onFailure {
            isPlaying = false
            Toast.makeText(
                context,
                "Não foi possível tocar esta música. Verifique se o arquivo ainda existe.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun playNext() {
        if (songs.isEmpty()) return
        val queuedId = queueIds.firstOrNull()
        if (queuedId != null) {
            saveQueue(queueIds.drop(1))
            songs.firstOrNull { it.id == queuedId }?.let(::playMusic)
            return
        }

        val currentIndex = songs.indexOfFirst { it.id == currentMusic?.id }.coerceAtLeast(0)
        val nextMusic = if (shuffleEnabled && songs.size > 1) {
            songs.filterNot { it.id == currentMusic?.id }.random(Random(System.currentTimeMillis()))
        } else {
            songs[(currentIndex + 1) % songs.size]
        }
        playMusic(nextMusic)
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

    fun reloadSongs() {
        if (!hasPermission) {
            songs = emptyList()
            currentMusic = null
            isLoadingSongs = false
            return
        }

        coroutineScope.launch {
            isLoadingSongs = true
            val loadedSongs = withContext(Dispatchers.IO) {
                MusicStore.loadDeviceSongs(context.applicationContext)
            }
            songs = loadedSongs
            if (continueListeningEnabled) {
                val recentMusic = recentIds.firstNotNullOfOrNull { id -> loadedSongs.firstOrNull { it.id == id } }
                if (currentMusic == null) currentMusic = recentMusic ?: loadedSongs.firstOrNull()
            } else if (currentMusic == null || loadedSongs.none { it.id == currentMusic?.id }) {
                currentMusic = loadedSongs.firstOrNull()
            }
            isLoadingSongs = false
        }
    }

    fun toggleLike(music: Music) {
        likedIds = if (music.id in likedIds) likedIds - music.id else likedIds + music.id
        libraryStorage.saveLikedIds(likedIds)
    }

    fun createPlaylist(name: String, songIds: List<Long>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        playlists = playlists + Playlist(System.currentTimeMillis(), cleanName, songIds.distinct())
        libraryStorage.savePlaylists(playlists)
    }

    fun deletePlaylist(playlist: Playlist) {
        playlists = playlists.filterNot { it.id == playlist.id }
        libraryStorage.savePlaylists(playlists)
    }

    fun updatePlaylist(playlist: Playlist, name: String, songIds: List<Long>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        playlists = playlists.map {
            if (it.id == playlist.id) it.copy(name = cleanName, songIds = songIds.distinct()) else it
        }
        libraryStorage.savePlaylists(playlists)
    }

    fun addToPlaylist(music: Music, playlist: Playlist) {
        playlists = playlists.map {
            if (it.id == playlist.id && music.id !in it.songIds) it.copy(songIds = it.songIds + music.id) else it
        }
        libraryStorage.savePlaylists(playlists)
    }

    fun removeFromPlaylist(music: Music, playlist: Playlist) {
        playlists = playlists.map {
            if (it.id == playlist.id) it.copy(songIds = it.songIds - music.id) else it
        }
        libraryStorage.savePlaylists(playlists)
    }

    fun addToQueue(music: Music) {
        saveQueue(queueIds + music.id)
        Toast.makeText(context, "Adicionado à fila.", Toast.LENGTH_SHORT).show()
    }

    fun removeFromQueue(music: Music) {
        val index = queueIds.indexOf(music.id)
        if (index >= 0) saveQueue(queueIds.toMutableList().also { it.removeAt(index) })
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from !in queueIds.indices || to !in queueIds.indices) return
        val mutable = queueIds.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        saveQueue(mutable)
    }

    fun clearQueue() {
        saveQueue(emptyList())
    }

    fun updateAudioQuality(quality: AudioQuality) {
        audioQuality = quality
        userPreferences.saveAudioQuality(quality)
        Toast.makeText(context, "Qualidade salva. Arquivos locais usam a qualidade original.", Toast.LENGTH_SHORT).show()
    }

    fun updateEqualizer(preset: EqualizerPreset) {
        equalizerPreset = preset
        userPreferences.saveEqualizerPreset(preset)
        applyEqualizerPreset(preset)
        Toast.makeText(context, "Equalizador: ${preset.label}", Toast.LENGTH_SHORT).show()
    }

    fun updateAccentTheme(theme: AccentTheme) {
        accentTheme = theme
        userPreferences.saveAccentTheme(theme)
    }

    fun updateCrossfade(enabled: Boolean) {
        crossfadeEnabled = enabled
        userPreferences.saveCrossfadeEnabled(enabled)
    }

    fun updateContinueListening(enabled: Boolean) {
        continueListeningEnabled = enabled
        userPreferences.saveContinueListeningEnabled(enabled)
    }

    fun setSleepTimer(option: SleepTimerOption?) {
        if (option == null) {
            sleepTimerEndAtMs = 0L
            sleepTimerLabel = null
            Toast.makeText(context, "Timer desligado.", Toast.LENGTH_SHORT).show()
            return
        }
        sleepTimerEndAtMs = System.currentTimeMillis() + option.durationMs
        sleepTimerLabel = option.label
        Toast.makeText(context, "Timer definido para ${option.label}.", Toast.LENGTH_SHORT).show()
    }

    fun setMediaNotificationsEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        notificationsEnabled = enabled
        userPreferences.saveMediaNotificationsEnabled(enabled)
        if (!enabled) notificationController.cancel()
    }

    fun exportBackup(): String {
        return libraryStorage.exportBackupJson(likedIds, playlists, queueIds, playbackStats)
    }

    fun importBackup(rawJson: String): Boolean {
        val backup = libraryStorage.importBackupJson(rawJson) ?: return false
        likedIds = backup.likedIds
        playlists = backup.playlists
        queueIds = backup.queueIds
        playbackStats = PlaybackStats(backup.playCounts, backup.totalListenMs)
        libraryStorage.saveLikedIds(likedIds)
        libraryStorage.savePlaylists(playlists)
        libraryStorage.saveQueueIds(queueIds)
        libraryStorage.savePlaybackStats(playbackStats)
        return true
    }

    LaunchedEffect(hasPermission) {
        reloadSongs()
    }

    LaunchedEffect(mediaPlayer, repeatEnabled, shuffleEnabled, queueIds, songs, currentMusic?.id) {
        mediaPlayer.setOnCompletionListener {
            if (repeatEnabled && currentMusic != null) {
                mediaPlayer.seekTo(0)
                mediaPlayer.start()
                isPlaying = true
            } else {
                playNext()
            }
        }
    }

    LaunchedEffect(isPlaying, currentMusic?.id) {
        var ticks = 0
        while (isPlaying) {
            delay(1000)
            runCatching {
                positionMs = mediaPlayer.currentPosition.toLong()
                if (mediaPlayer.duration > 0) durationMs = mediaPlayer.duration.toLong()
            }
            playbackStats = playbackStats.copy(totalListenMs = playbackStats.totalListenMs + 1000L)
            ticks += 1
            if (ticks % 5 == 0) libraryStorage.savePlaybackStats(playbackStats)
        }
        libraryStorage.savePlaybackStats(playbackStats)
    }

    LaunchedEffect(sleepTimerEndAtMs) {
        val endAt = sleepTimerEndAtMs
        if (endAt <= 0L) return@LaunchedEffect
        val waitMs = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
        delay(waitMs)
        if (sleepTimerEndAtMs == endAt && isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            sleepTimerEndAtMs = 0L
            sleepTimerLabel = null
            Toast.makeText(context, "Timer finalizado. Música pausada.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(notificationsEnabled, hasNotificationPermission, currentMusic?.id, currentMusic?.title, isPlaying) {
        val music = currentMusic
        if (notificationsEnabled && hasNotificationPermission && music != null) {
            notificationController.show(music, isPlaying)
        } else {
            notificationController.cancel()
        }
    }

    LaunchedEffect(currentMusic?.id, isPlaying) {
        val music = currentMusic
        WaveMusicWidgetProvider.updateNowPlaying(
            context.applicationContext,
            title = music?.title ?: "Wave Music",
            artist = music?.artist ?: "Escolha uma música",
            isPlaying = isPlaying
        )
    }

    LaunchedEffect(currentMusic?.id, isPlaying, positionMs, durationMs) {
        val music = currentMusic
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, music?.title ?: "Wave Music")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, music?.artist ?: "")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
                .build()
        )
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO
                )
                .setState(
                    if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    positionMs,
                    if (isPlaying) 1f else 0f
                )
                .build()
        )
    }

    DisposableEffect(songs, currentMusic?.id, isPlaying) {
        MusicNotificationReceiver.actionHandler = { action ->
            when (action) {
                MusicNotificationReceiver.ACTION_PLAY_PAUSE -> togglePlayPause()
                MusicNotificationReceiver.ACTION_NEXT -> playNext()
                MusicNotificationReceiver.ACTION_PREVIOUS -> playPrevious()
            }
        }
        mediaSession.isActive = true
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = togglePlayPause()
            override fun onPause() = togglePlayPause()
            override fun onSkipToNext() = playNext()
            override fun onSkipToPrevious() = playPrevious()
            override fun onSeekTo(pos: Long) {
                if (durationMs > 0L) seekTo(pos.toFloat() / durationMs.toFloat())
            }
        })

        onDispose {
            MusicNotificationReceiver.actionHandler = null
            mediaSession.setCallback(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            notificationController.cancel()
            runCatching { equalizer?.release() }
            runCatching { mediaPlayer.release() }
            runCatching { mediaSession.release() }
        }
    }

    val selectedMusic = currentMusic
    val activeQueueSongs = queueSongs()
    val backgroundColors = listOf(
        accentTheme.primary.copy(alpha = 0.34f),
        WaveBackground,
        accentTheme.secondary.copy(alpha = 0.18f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(backgroundColors))
    ) {
        if (showNowPlaying && selectedMusic != null) {
            NowPlayingScreen(
                music = selectedMusic,
                isPlaying = isPlaying,
                isLiked = selectedMusic.id in likedIds,
                playlists = playlists,
                queueSongs = activeQueueSongs,
                positionMs = positionMs,
                durationMs = durationMs.takeIf { it > 0 } ?: selectedMusic.durationMs,
                repeatEnabled = repeatEnabled,
                shuffleEnabled = shuffleEnabled,
                onSeek = ::seekTo,
                onBack = { showNowPlaying = false },
                onPlayPause = ::togglePlayPause,
                onPrevious = ::playPrevious,
                onNext = ::playNext,
                onToggleRepeat = { repeatEnabled = !repeatEnabled },
                onToggleShuffle = { shuffleEnabled = !shuffleEnabled },
                onToggleLike = ::toggleLike,
                onAddToPlaylist = ::addToPlaylist,
                onAddToQueue = ::addToQueue,
                onRemoveFromQueue = ::removeFromQueue,
                onMoveQueueItem = ::moveQueueItem,
                onClearQueue = ::clearQueue
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Column {
                        MiniPlayer(
                            music = selectedMusic,
                            isPlaying = isPlaying,
                            progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
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
                            recentIds = recentIds,
                            playbackStats = playbackStats,
                            queueCount = queueIds.size,
                            isLoading = isLoadingSongs,
                            hasPermission = hasPermission,
                            onRequestPermission = { permissionLauncher.launch(audioPermission) },
                            onRefresh = ::reloadSongs,
                            onSongClick = ::playMusic,
                            onToggleLike = ::toggleLike,
                            onAddToPlaylist = ::addToPlaylist,
                            onAddToQueue = ::addToQueue,
                            onRemoveFromQueue = ::removeFromQueue,
                            queuedIds = queueIds.toSet()
                        )

                        WaveTab.Search -> SearchScreen(
                            songs = songs,
                            currentMusicId = selectedMusic?.id,
                            likedIds = likedIds,
                            playlists = playlists,
                            queuedIds = queueIds.toSet(),
                            onSongClick = ::playMusic,
                            onToggleLike = ::toggleLike,
                            onAddToPlaylist = ::addToPlaylist,
                            onAddToQueue = ::addToQueue,
                            onRemoveFromQueue = ::removeFromQueue
                        )

                        WaveTab.Library -> LibraryScreen(
                            songs = songs,
                            currentMusicId = selectedMusic?.id,
                            likedIds = likedIds,
                            playlists = playlists,
                            recentIds = recentIds,
                            playbackStats = playbackStats,
                            queuedIds = queueIds.toSet(),
                            onSongClick = ::playMusic,
                            onToggleLike = ::toggleLike,
                            onCreatePlaylist = ::createPlaylist,
                            onUpdatePlaylist = ::updatePlaylist,
                            onDeletePlaylist = ::deletePlaylist,
                            onAddToPlaylist = ::addToPlaylist,
                            onRemoveFromPlaylist = ::removeFromPlaylist,
                            onAddToQueue = ::addToQueue,
                            onRemoveFromQueue = ::removeFromQueue
                        )

                        WaveTab.Settings -> SettingsScreen(
                            audioQuality = audioQuality,
                            equalizerPreset = equalizerPreset,
                            accentTheme = accentTheme,
                            notificationsEnabled = notificationsEnabled,
                            crossfadeEnabled = crossfadeEnabled,
                            continueListeningEnabled = continueListeningEnabled,
                            sleepTimerLabel = sleepTimerLabel,
                            appVersion = BuildConfig.VERSION_NAME,
                            onAudioQualitySelected = ::updateAudioQuality,
                            onEqualizerSelected = ::updateEqualizer,
                            onAccentThemeSelected = ::updateAccentTheme,
                            onNotificationsChanged = ::setMediaNotificationsEnabled,
                            onCrossfadeChanged = ::updateCrossfade,
                            onContinueListeningChanged = ::updateContinueListening,
                            onSleepTimerSelected = ::setSleepTimer,
                            onExportBackup = ::exportBackup,
                            onImportBackup = ::importBackup
                        )
                    }
                }
            }
        }
    }
}
