package com.wavemusic.player

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.wavemusic.player.data.PlaylistImageStore
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
    val playlistImageStore = remember { PlaylistImageStore(context.applicationContext) }
    val mediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember {
        mutableStateOf(
            mediaPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
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
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            hasPermission = mediaPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
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
    var playbackSourceIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var repeatEnabled by rememberSaveable { mutableStateOf(false) }
    var shuffleEnabled by rememberSaveable { mutableStateOf(false) }
    var sleepTimerEndAtMs by remember { mutableLongStateOf(0L) }
    var sleepTimerLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var mediaNotificationDismissed by rememberSaveable { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    val mediaPlayer = remember { MediaPlayer() }
    var equalizer by remember { mutableStateOf<Equalizer?>(null) }
    val notificationController = remember { WaveMusicNotificationController(context.applicationContext) }
    val mediaSession = remember { MediaSession(context.applicationContext, "WaveMusicSession") }
    var videoSurfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun queueSongs(): List<Music> = queueIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }

    fun playbackSongs(): List<Music> {
        val sourceSongs = playbackSourceIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
        return sourceSongs.ifEmpty { songs }
    }

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

    fun saveCurrentPlaybackMemory() {
        val music = currentMusic ?: return
        val currentPosition = runCatching { mediaPlayer.currentPosition.toLong() }
            .getOrDefault(positionMs)
        val currentDuration = runCatching { mediaPlayer.duration.toLong() }
            .getOrDefault(durationMs)
            .takeIf { it > 0L }
            ?: durationMs.takeIf { it > 0L }
            ?: music.durationMs

        libraryStorage.saveResumePosition(music.id, currentPosition, currentDuration)
    }

    fun isPlayerCurrentlyPlaying(): Boolean {
        return runCatching { mediaPlayer.isPlaying }.getOrDefault(false)
    }

    fun resolveVideoAspectRatio(music: Music): Float {
        if (!music.isVideo) return 16f / 9f

        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, music.uri)
                val width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toFloatOrNull()
                    ?: mediaPlayer.videoWidth.toFloat()
                val height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toFloatOrNull()
                    ?: mediaPlayer.videoHeight.toFloat()
                val rotation = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()
                    ?: 0
                val displayWidth = if (rotation % 180 == 0) width else height
                val displayHeight = if (rotation % 180 == 0) height else width

                (displayWidth / displayHeight)
                    .takeIf { it.isFinite() && it > 0f }
                    ?: 16f / 9f
            } finally {
                retriever.release()
            }
        }.getOrDefault(
            (mediaPlayer.videoWidth.toFloat() / mediaPlayer.videoHeight.toFloat())
                .takeIf { it.isFinite() && it > 0f }
                ?: 16f / 9f
        )
    }

    fun playMusic(music: Music) {
        runCatching {
            saveCurrentPlaybackMemory()
            mediaPlayer.reset()
            mediaPlayer.setDisplay(if (music.isVideo) videoSurfaceHolder else null)
            mediaPlayer.setDataSource(context, music.uri)
            mediaPlayer.prepare()
            rebuildEqualizer()
            videoAspectRatio = resolveVideoAspectRatio(music)
            val preparedDuration = mediaPlayer.duration.toLong().takeIf { it > 0 } ?: music.durationMs
            val savedPosition = libraryStorage.loadResumePosition(music.id)
                .takeIf { saved ->
                    saved >= 5_000L && (preparedDuration <= 0L || saved < preparedDuration - 10_000L)
                }
                ?: 0L
            if (savedPosition > 0L) mediaPlayer.seekTo(savedPosition.toInt())
            if (crossfadeEnabled) mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.start()
            mediaSession.isActive = true
            currentMusic = music
            durationMs = preparedDuration
            positionMs = savedPosition
            isPlaying = true
            mediaNotificationDismissed = false
            if (music.isVideo) showNowPlaying = true
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

    fun updateVideoSurface(holder: SurfaceHolder?) {
        videoSurfaceHolder = holder
        runCatching {
            mediaPlayer.setDisplay(if (currentMusic?.isVideo == true) holder else null)
        }
    }

    fun playMusicFromLibrary(music: Music) {
        playbackSourceIds = songs.map { it.id }
        saveQueue(emptyList())
        playMusic(music)
    }

    fun playMusicFromSource(sourceSongs: List<Music>, music: Music) {
        val cleanSource = sourceSongs.distinctBy { it.id }
        playbackSourceIds = cleanSource.map { it.id }
        saveQueue(emptyList())
        playMusic(music)
    }

    fun playNext() {
        if (songs.isEmpty()) return
        val validQueueIndex = queueIds.indexOfFirst { queuedId -> songs.any { it.id == queuedId } }
        if (validQueueIndex >= 0) {
            val queuedId = queueIds[validQueueIndex]
            saveQueue(queueIds.drop(validQueueIndex + 1))
            songs.firstOrNull { it.id == queuedId }?.let(::playMusic)
            return
        } else if (queueIds.isNotEmpty()) {
            saveQueue(emptyList())
        }

        val sourceSongs = playbackSongs()
        if (sourceSongs.isEmpty()) return
        val currentIndex = sourceSongs.indexOfFirst { it.id == currentMusic?.id }.coerceAtLeast(0)
        val nextMusic = if (shuffleEnabled && sourceSongs.size > 1) {
            sourceSongs.filterNot { it.id == currentMusic?.id }.random(Random(System.currentTimeMillis()))
        } else {
            sourceSongs[(currentIndex + 1) % sourceSongs.size]
        }
        playMusic(nextMusic)
    }

    fun playPrevious() {
        if (songs.isEmpty()) return
        val sourceSongs = playbackSongs()
        if (sourceSongs.isEmpty()) return
        val currentIndex = sourceSongs.indexOfFirst { it.id == currentMusic?.id }.coerceAtLeast(0)
        playMusic(sourceSongs[(currentIndex - 1 + sourceSongs.size) % sourceSongs.size])
    }

    fun togglePlayPause() {
        val music = currentMusic ?: songs.firstOrNull() ?: return
        if (currentMusic?.id != music.id || durationMs == 0L) {
            playMusicFromLibrary(music)
            return
        }
        if (isPlayerCurrentlyPlaying()) {
            runCatching { mediaPlayer.pause() }
            saveCurrentPlaybackMemory()
            isPlaying = false
        } else {
            runCatching {
                mediaSession.isActive = true
                mediaNotificationDismissed = false
                mediaPlayer.start()
                isPlaying = true
            }.onFailure {
                isPlaying = false
                Toast.makeText(context, "Não foi possível retomar a reprodução.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun seekTo(progress: Float) {
        if (durationMs <= 0L) return
        val newPosition = (durationMs * progress.coerceIn(0f, 1f)).toLong()
        runCatching {
            mediaPlayer.seekTo(newPosition.toInt())
            positionMs = newPosition
        }
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

    fun createPlaylist(name: String, description: String, imageUri: String?, songIds: List<Long>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val createdAt = System.currentTimeMillis()
        playlists = playlists + Playlist(
            id = createdAt,
            name = cleanName,
            songIds = songIds.distinct(),
            description = description.trim(),
            imageUri = imageUri,
            createdAt = createdAt
        )
        libraryStorage.savePlaylists(playlists)
    }

    fun deletePlaylist(playlist: Playlist) {
        playlistImageStore.deletePlaylistCover(playlist.imageUri)
        playlists = playlists.filterNot { it.id == playlist.id }
        libraryStorage.savePlaylists(playlists)
    }

    fun updatePlaylist(
        playlist: Playlist,
        name: String,
        description: String,
        imageUri: String?,
        songIds: List<Long>
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        if (playlist.imageUri != null && playlist.imageUri != imageUri) {
            playlistImageStore.deletePlaylistCover(playlist.imageUri)
        }
        playlists = playlists.map {
            if (it.id == playlist.id) {
                it.copy(
                    name = cleanName,
                    songIds = songIds.distinct(),
                    description = description.trim(),
                    imageUri = imageUri
                )
            } else {
                it
            }
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
        if (music.id in queueIds) {
            Toast.makeText(context, "Essa faixa já está na fila.", Toast.LENGTH_SHORT).show()
            return
        }
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
        if (enabled) {
            mediaNotificationDismissed = false
        } else {
            notificationController.cancel()
        }
    }

    fun closeMediaNotification() {
        runCatching {
            if (isPlayerCurrentlyPlaying()) mediaPlayer.pause()
        }
        saveCurrentPlaybackMemory()
        isPlaying = false
        mediaNotificationDismissed = true
        mediaSession.isActive = false
        notificationController.cancel()
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
        libraryStorage.replaceResumePositions(backup.resumePositions)
        return true
    }

    LaunchedEffect(hasPermission) {
        reloadSongs()
    }

    LaunchedEffect(mediaPlayer, repeatEnabled, shuffleEnabled, queueIds, playbackSourceIds, songs, currentMusic?.id) {
        mediaPlayer.setOnCompletionListener {
            currentMusic?.let { libraryStorage.saveResumePosition(it.id, 0L, durationMs) }
            positionMs = 0L
            if (repeatEnabled && currentMusic != null) {
                runCatching {
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                    isPlaying = true
                }.onFailure {
                    isPlaying = false
                }
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
            if (ticks % 5 == 0) {
                libraryStorage.savePlaybackStats(playbackStats)
                currentMusic?.let { libraryStorage.saveResumePosition(it.id, positionMs, durationMs) }
            }
        }
        libraryStorage.savePlaybackStats(playbackStats)
        saveCurrentPlaybackMemory()
    }

    LaunchedEffect(sleepTimerEndAtMs) {
        val endAt = sleepTimerEndAtMs
        if (endAt <= 0L) return@LaunchedEffect
        val waitMs = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
        delay(waitMs)
        if (sleepTimerEndAtMs == endAt && isPlaying) {
            runCatching { mediaPlayer.pause() }
            saveCurrentPlaybackMemory()
            isPlaying = false
            sleepTimerEndAtMs = 0L
            sleepTimerLabel = null
            Toast.makeText(context, "Timer finalizado. Música pausada.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(
        notificationsEnabled,
        hasNotificationPermission,
        currentMusic?.id,
        currentMusic?.title,
        isPlaying,
        positionMs,
        durationMs,
        likedIds,
        mediaNotificationDismissed
    ) {
        val music = currentMusic
        if (notificationsEnabled && hasNotificationPermission && music != null && !mediaNotificationDismissed) {
            notificationController.show(
                music = music,
                isPlaying = isPlaying,
                isLiked = music.id in likedIds,
                positionMs = positionMs,
                durationMs = durationMs.takeIf { it > 0L } ?: music.durationMs,
                sessionToken = mediaSession.sessionToken
            )
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

    LaunchedEffect(currentMusic?.id, durationMs) {
        val music = currentMusic
        val artwork = withContext(Dispatchers.IO) {
            music?.let { notificationController.artworkForSession(it) }
        }
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, music?.title ?: "Wave Music")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, music?.artist ?: "")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs.takeIf { it > 0L } ?: music?.durationMs ?: 0L)

        artwork?.let {
            metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, it)
            metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadata.build())
    }

    LaunchedEffect(currentMusic?.id, isPlaying, positionMs, durationMs, mediaNotificationDismissed) {
        val playbackState = when {
            currentMusic == null || mediaNotificationDismissed -> PlaybackState.STATE_STOPPED
            isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO or
                        PlaybackState.ACTION_STOP
                )
                .setState(
                    playbackState,
                    positionMs,
                    if (isPlaying) 1f else 0f
                )
                .build()
        )
    }

    DisposableEffect(songs, currentMusic?.id, isPlaying, mediaNotificationDismissed) {
        MusicNotificationReceiver.actionHandler = { action ->
            when (action) {
                MusicNotificationReceiver.ACTION_FAVORITE -> currentMusic?.let(::toggleLike)
                MusicNotificationReceiver.ACTION_PLAY_PAUSE -> togglePlayPause()
                MusicNotificationReceiver.ACTION_NEXT -> playNext()
                MusicNotificationReceiver.ACTION_PREVIOUS -> playPrevious()
                MusicNotificationReceiver.ACTION_CLOSE -> closeMediaNotification()
            }
        }
        mediaSession.isActive = !mediaNotificationDismissed
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = togglePlayPause()
            override fun onPause() = togglePlayPause()
            override fun onSkipToNext() = playNext()
            override fun onSkipToPrevious() = playPrevious()
            override fun onStop() = closeMediaNotification()
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
            saveCurrentPlaybackMemory()
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
                videoAspectRatio = videoAspectRatio,
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
                onClearQueue = ::clearQueue,
                onVideoSurfaceReady = ::updateVideoSurface
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
                            onRequestPermission = { permissionLauncher.launch(mediaPermissions) },
                            onRefresh = ::reloadSongs,
                            onSongClick = ::playMusicFromLibrary,
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
                            onSongClick = ::playMusicFromLibrary,
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
                            onSongClick = ::playMusicFromLibrary,
                            onPlaySongList = ::playMusicFromSource,
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
