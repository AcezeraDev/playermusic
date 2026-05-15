package com.wavemusic.player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.wavemusic.player.data.model.AccentTheme
import com.wavemusic.player.data.model.AudioQuality
import com.wavemusic.player.data.model.EqualizerPreset
import com.wavemusic.player.data.local.LibraryStorage
import com.wavemusic.player.data.model.LocalTagOverride
import com.wavemusic.player.data.model.Music
import com.wavemusic.player.data.repository.MusicStore
import com.wavemusic.player.data.model.PlaybackStats
import com.wavemusic.player.data.model.Playlist
import com.wavemusic.player.data.local.PlaylistImageStore
import com.wavemusic.player.data.model.SleepTimerOption
import com.wavemusic.player.data.preferences.UserPreferences
import com.wavemusic.player.notifications.MusicNotificationReceiver
import com.wavemusic.player.notifications.WaveMusicNotificationController
import com.wavemusic.player.domain.player.WaveMusicForegroundState
import com.wavemusic.player.domain.player.WaveMusicPlaybackService
import com.wavemusic.player.domain.player.WaveMusicPlayerHolder
import com.wavemusic.player.domain.usecase.addQueueId
import com.wavemusic.player.domain.usecase.consumeNextValidQueueId
import com.wavemusic.player.domain.usecase.moveQueueId
import com.wavemusic.player.domain.usecase.playNextQueueId
import com.wavemusic.player.domain.usecase.removeFirstQueueId
import com.wavemusic.player.ui.navigation.BottomNavigationBar
import com.wavemusic.player.ui.components.MiniPlayer
import com.wavemusic.player.ui.components.NeonVisualizer
import com.wavemusic.player.ui.navigation.WaveTab
import com.wavemusic.player.ui.screens.HomeScreen
import com.wavemusic.player.ui.screens.LibraryScreen
import com.wavemusic.player.ui.screens.NowPlayingScreen
import com.wavemusic.player.ui.screens.SearchScreen
import com.wavemusic.player.ui.screens.SettingsScreen
import com.wavemusic.player.ui.theme.WaveBackground
import com.wavemusic.player.ui.theme.WaveMusicTheme
import com.wavemusic.player.utils.extensions.isBluetoothOutputDevice
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
    val appContext = context.applicationContext
    val userPreferences = remember { UserPreferences(context.applicationContext) }
    val libraryStorage = remember { LibraryStorage(context.applicationContext) }
    val playlistImageStore = remember { PlaylistImageStore(context.applicationContext) }
    val audioPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val videoPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember {
        mutableStateOf(
            audioPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var hasVideoPermission by remember {
        mutableStateOf(
            videoPermissions.all { permission ->
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
    var customEqualizerGains by remember { mutableStateOf(userPreferences.loadCustomEqualizerGains()) }
    var accentTheme by remember { mutableStateOf(userPreferences.loadAccentTheme()) }
    var crossfadeEnabled by remember { mutableStateOf(userPreferences.loadCrossfadeEnabled()) }
    var continueListeningEnabled by remember { mutableStateOf(userPreferences.loadContinueListeningEnabled()) }
    var resumePlaybackPositionEnabled by remember { mutableStateOf(userPreferences.loadResumePlaybackPositionEnabled()) }
    var localVideosEnabled by remember { mutableStateOf(userPreferences.loadLocalVideosEnabled() && hasVideoPermission) }
    var notificationsEnabled by remember {
        mutableStateOf(userPreferences.loadMediaNotificationsEnabled() && hasNotificationPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            hasPermission = audioPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    )
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            hasVideoPermission = videoPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            localVideosEnabled = hasVideoPermission
            userPreferences.saveLocalVideosEnabled(hasVideoPermission)
            if (hasVideoPermission) {
                Toast.makeText(context, "Videos locais ativados.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permissao de videos negada.", Toast.LENGTH_SHORT).show()
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

    val initialPlayback = remember { WaveMusicForegroundState.snapshot() }
    var songs by remember { mutableStateOf<List<Music>>(emptyList()) }
    var selectedTab by rememberSaveable { mutableStateOf(WaveTab.Home) }
    var currentMusic by remember { mutableStateOf<Music?>(initialPlayback.music) }
    var isPlaying by rememberSaveable { mutableStateOf(initialPlayback.isPlaying) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(initialPlayback.positionMs) }
    var durationMs by remember { mutableLongStateOf(initialPlayback.durationMs) }
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
    var lyricsRevision by remember { mutableStateOf(0) }
    var tagRevision by remember { mutableStateOf(0) }

    val player = remember { WaveMusicPlayerHolder.player(appContext) }
    var equalizer by remember { mutableStateOf<Equalizer?>(null) }
    val notificationController = remember { WaveMusicNotificationController(context.applicationContext) }
    val mediaSession = remember { WaveMusicPlayerHolder.mediaSession(appContext) }
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
            val gains = if (preset == EqualizerPreset.Custom) customEqualizerGains else preset.bandGains
            repeat(bandCount) { band ->
                val sourceIndex = ((band.toFloat() / bandCount.toFloat()) * gains.size)
                    .toInt()
                    .coerceIn(0, gains.lastIndex)
                val level = gains[sourceIndex].coerceIn(min, max)
                activeEqualizer.setBandLevel(band.toShort(), level)
            }
            activeEqualizer.enabled = true
        }
    }

    fun rebuildEqualizer() {
        runCatching {
            equalizer?.release()
            equalizer = null
            val audioSessionId = player.audioSessionId
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
            equalizer = Equalizer(0, audioSessionId)
            applyEqualizerPreset()
        }.onFailure {
            equalizer = null
        }
    }

    fun saveCurrentPlaybackMemory() {
        if (!resumePlaybackPositionEnabled) return
        val music = currentMusic ?: return
        val currentPosition = runCatching { player.currentPosition }
            .getOrDefault(positionMs)
        val currentDuration = runCatching { player.duration }
            .getOrDefault(durationMs)
            .takeIf { it > 0L }
            ?: durationMs.takeIf { it > 0L }
            ?: music.durationMs

        libraryStorage.saveResumePosition(music.id, currentPosition, currentDuration)
    }

    fun isPlayerCurrentlyPlaying(): Boolean {
        return runCatching { player.isPlaying }.getOrDefault(false)
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
                    ?: player.videoSize.width.toFloat()
                val height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toFloatOrNull()
                    ?: player.videoSize.height.toFloat()
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
            (player.videoSize.width.toFloat() / player.videoSize.height.toFloat())
                .takeIf { it.isFinite() && it > 0f }
                ?: 16f / 9f
        )
    }

    fun playMusic(music: Music) {
        runCatching {
            saveCurrentPlaybackMemory()
            player.stop()
            player.clearMediaItems()
            if (music.isVideo && videoSurfaceHolder != null) {
                player.setVideoSurfaceHolder(videoSurfaceHolder)
            } else {
                player.clearVideoSurface()
            }
            player.setMediaItem(MediaItem.fromUri(music.uri))
            player.prepare()
            videoAspectRatio = resolveVideoAspectRatio(music)
            val preparedDuration = player.duration
                .takeIf { it > 0L && it != C.TIME_UNSET }
                ?: music.durationMs
            val savedPosition = if (resumePlaybackPositionEnabled) {
                libraryStorage.loadResumePosition(music.id)
                    .takeIf { saved ->
                        saved >= 5_000L && (preparedDuration <= 0L || saved < preparedDuration - 10_000L)
                    }
                    ?: 0L
            } else {
                0L
            }
            if (savedPosition > 0L) player.seekTo(savedPosition)
            player.volume = if (crossfadeEnabled) 0f else 1f
            player.play()
            rebuildEqualizer()
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
                        runCatching { player.volume = volume }
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
            if (currentMusic?.isVideo == true && holder != null) {
                player.setVideoSurfaceHolder(holder)
            } else {
                player.clearVideoSurface()
            }
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

    fun playPlaylistFromHome(playlist: Playlist) {
        val playlistSongs = playlist.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
        val firstSong = playlistSongs.firstOrNull()
        if (firstSong == null) {
            Toast.makeText(context, "Essa playlist ainda esta vazia.", Toast.LENGTH_SHORT).show()
            return
        }
        playMusicFromSource(playlistSongs, firstSong)
    }

    fun playNext() {
        if (songs.isEmpty()) return
        val (queuedId, remainingQueue) = consumeNextValidQueueId(queueIds, songs.map { it.id }.toSet())
        if (queuedId != null) {
            saveQueue(remainingQueue)
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
            runCatching { player.pause() }
            saveCurrentPlaybackMemory()
            isPlaying = false
        } else {
            runCatching {
                mediaSession.isActive = true
                mediaNotificationDismissed = false
                player.play()
                isPlaying = true
            }.onFailure {
                isPlaying = false
                Toast.makeText(context, "Não foi possível retomar a reprodução.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pauseForDisconnectedAudioDevice() {
        if (!isPlayerCurrentlyPlaying()) return
        runCatching { player.pause() }
        saveCurrentPlaybackMemory()
        isPlaying = false
        Toast.makeText(context, "Fone Bluetooth desconectado. Musica pausada.", Toast.LENGTH_SHORT).show()
    }

    fun seekTo(progress: Float) {
        if (durationMs <= 0L) return
        val newPosition = (durationMs * progress.coerceIn(0f, 1f)).toLong()
        runCatching {
            player.seekTo(newPosition)
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
                MusicStore.loadDeviceSongs(
                    context.applicationContext,
                    includeVideos = localVideosEnabled && hasVideoPermission
                )
            }
            val tagOverrides = libraryStorage.loadTagOverrides()
            val displaySongs = loadedSongs.map { music -> music.withTagOverride(tagOverrides[music.id]) }
            songs = displaySongs
            if (continueListeningEnabled) {
                val recentMusic = recentIds.firstNotNullOfOrNull { id -> displaySongs.firstOrNull { it.id == id } }
                if (currentMusic == null) currentMusic = recentMusic ?: displaySongs.firstOrNull()
            } else if (currentMusic == null || displaySongs.none { it.id == currentMusic?.id }) {
                currentMusic = displaySongs.firstOrNull()
            } else {
                currentMusic = displaySongs.firstOrNull { it.id == currentMusic?.id } ?: currentMusic
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
        saveQueue(addQueueId(queueIds, music.id))
        Toast.makeText(context, "Adicionado à fila.", Toast.LENGTH_SHORT).show()
    }

    fun playNextInQueue(music: Music) {
        if (music.id == currentMusic?.id) {
            Toast.makeText(context, "Essa faixa ja esta tocando.", Toast.LENGTH_SHORT).show()
            return
        }
        if (music.id in queueIds) {
            Toast.makeText(context, "Essa faixa ja esta na fila.", Toast.LENGTH_SHORT).show()
            return
        }
        saveQueue(playNextQueueId(queueIds, currentMusic?.id, music.id))
        Toast.makeText(context, "Vai tocar a seguir.", Toast.LENGTH_SHORT).show()
    }

    fun removeFromQueue(music: Music) {
        saveQueue(removeFirstQueueId(queueIds, music.id))
    }

    fun moveQueueItem(from: Int, to: Int) {
        saveQueue(moveQueueId(queueIds, from, to))
    }

    fun clearQueue() {
        saveQueue(emptyList())
    }

    fun saveQueueAsPlaylist(name: String) {
        val cleanName = name.trim().ifBlank { "Fila salva" }
        val ids = queueIds.filter { id -> songs.any { it.id == id } }.distinct()
        if (ids.isEmpty()) {
            Toast.makeText(context, "Fila vazia.", Toast.LENGTH_SHORT).show()
            return
        }
        createPlaylist(cleanName, "Criada a partir da fila de reproducao.", null, ids)
        Toast.makeText(context, "Fila salva como playlist.", Toast.LENGTH_SHORT).show()
    }

    fun createPlaylistFromFolder(folder: String) {
        val folderSongs = songs.filter { it.folder == folder }
        if (folderSongs.isEmpty()) return
        createPlaylist(folder, "Playlist automatica criada da pasta $folder.", null, folderSongs.map { it.id })
        Toast.makeText(context, "Playlist criada para $folder.", Toast.LENGTH_SHORT).show()
    }

    fun updateAudioQuality(quality: AudioQuality) {
        audioQuality = quality
        userPreferences.saveAudioQuality(quality)
        Toast.makeText(context, "Qualidade salva. Arquivos locais usam a qualidade original.", Toast.LENGTH_SHORT).show()
    }

    fun updateEqualizer(preset: EqualizerPreset, customGains: List<Short>) {
        equalizerPreset = preset
        customEqualizerGains = customGains
        userPreferences.saveEqualizerPreset(preset)
        userPreferences.saveCustomEqualizerGains(customGains)
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

    fun updateResumePlaybackPosition(enabled: Boolean) {
        resumePlaybackPositionEnabled = enabled
        userPreferences.saveResumePlaybackPositionEnabled(enabled)
        Toast.makeText(
            context,
            if (enabled) "Retomar minutagem ativado." else "Musicas vao comecar do inicio.",
            Toast.LENGTH_SHORT
        ).show()
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
            WaveMusicPlaybackService.stop(appContext)
        }
    }

    fun setLocalVideosEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasVideoPermission) {
            videoPermissionLauncher.launch(videoPermissions)
            return
        }
        localVideosEnabled = enabled
        userPreferences.saveLocalVideosEnabled(enabled)
        reloadSongs()
    }

    fun closeMediaNotification() {
        runCatching {
            if (isPlayerCurrentlyPlaying()) player.pause()
        }
        saveCurrentPlaybackMemory()
        isPlaying = false
        mediaNotificationDismissed = true
        mediaSession.isActive = false
        notificationController.cancel()
        WaveMusicForegroundState.clear()
        WaveMusicPlaybackService.stop(appContext)
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
        backup.lyrics.forEach { (id, text) -> libraryStorage.saveLyrics(id, text) }
        backup.tagOverrides.forEach { (id, override) -> libraryStorage.saveTagOverride(id, override) }
        lyricsRevision += 1
        tagRevision += 1
        reloadSongs()
        return true
    }

    fun saveLyrics(music: Music, lyrics: String) {
        libraryStorage.saveLyrics(music.id, lyrics)
        lyricsRevision += 1
        Toast.makeText(context, "Letra salva localmente.", Toast.LENGTH_SHORT).show()
    }

    fun saveTagOverride(music: Music, title: String, artist: String, album: String) {
        val override = LocalTagOverride(
            title = title.trim().ifBlank { music.title },
            artist = artist.trim().ifBlank { music.artist },
            album = album.trim().ifBlank { music.album }
        )
        libraryStorage.saveTagOverride(music.id, override)
        songs = songs.map { if (it.id == music.id) it.withTagOverride(override) else it }
        currentMusic = currentMusic?.let { if (it.id == music.id) it.withTagOverride(override) else it }
        tagRevision += 1
        Toast.makeText(context, "Informacoes atualizadas no app.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(hasPermission, hasVideoPermission, localVideosEnabled, tagRevision) {
        reloadSongs()
    }

    DisposableEffect(
        player,
        repeatEnabled,
        shuffleEnabled,
        queueIds,
        playbackSourceIds,
        songs,
        currentMusic?.id,
        resumePlaybackPositionEnabled
    ) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val resolvedDuration = player.duration
                    .takeIf { it > 0L && it != C.TIME_UNSET }
                    ?: durationMs
                if (resolvedDuration > 0L) durationMs = resolvedDuration
                if (playbackState != Player.STATE_ENDED) return

                if (resumePlaybackPositionEnabled) {
                    currentMusic?.let { libraryStorage.saveResumePosition(it.id, 0L, durationMs) }
                }
                positionMs = 0L
                if (repeatEnabled && currentMusic != null) {
                    runCatching {
                        player.seekTo(0L)
                        player.play()
                        isPlaying = true
                    }.onFailure {
                        isPlaying = false
                    }
                } else {
                    playNext()
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                rebuildEqualizer()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlaying = false
                Toast.makeText(
                    context,
                    "Nao foi possivel continuar a reproducao.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, currentMusic?.id, resumePlaybackPositionEnabled) {
        var ticks = 0
        while (isPlaying) {
            delay(1000)
            runCatching {
                positionMs = player.currentPosition
                if (player.duration > 0L && player.duration != C.TIME_UNSET) durationMs = player.duration
            }
            playbackStats = playbackStats.copy(totalListenMs = playbackStats.totalListenMs + 1000L)
            ticks += 1
            if (ticks % 5 == 0) {
                libraryStorage.savePlaybackStats(playbackStats)
                if (resumePlaybackPositionEnabled) {
                    currentMusic?.let { libraryStorage.saveResumePosition(it.id, positionMs, durationMs) }
                }
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
            runCatching { player.pause() }
            saveCurrentPlaybackMemory()
            isPlaying = false
            sleepTimerEndAtMs = 0L
            sleepTimerLabel = null
            Toast.makeText(context, "Timer finalizado. Música pausada.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val noisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    pauseForDisconnectedAudioDevice()
                }
            }
        }
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                if (removedDevices.any { it.isBluetoothOutputDevice() }) {
                    coroutineScope.launch {
                        pauseForDisconnectedAudioDevice()
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        audioManager.registerAudioDeviceCallback(deviceCallback, null)

        onDispose {
            runCatching { appContext.unregisterReceiver(noisyReceiver) }
            runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
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
        WaveMusicForegroundState.update(
            music = music,
            isPlaying = isPlaying,
            isLiked = music?.id?.let { it in likedIds } == true,
            positionMs = positionMs,
            durationMs = durationMs.takeIf { it > 0L } ?: music?.durationMs ?: 0L,
            sessionToken = mediaSession.sessionToken
        )
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

    LaunchedEffect(
        currentMusic?.id,
        isPlaying,
        mediaNotificationDismissed
    ) {
        val music = currentMusic
        if (music != null && isPlaying && !mediaNotificationDismissed) {
            WaveMusicPlaybackService.start(appContext)
        } else if (music == null || !isPlaying || mediaNotificationDismissed) {
            WaveMusicPlaybackService.stop(appContext)
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
            if (!isPlayerCurrentlyPlaying()) {
                MusicNotificationReceiver.actionHandler = null
                mediaSession.setCallback(null)
            }
        }
    }

    val latestCurrentMusic by rememberUpdatedState(currentMusic)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestLikedIds by rememberUpdatedState(likedIds)
    val latestPositionMs by rememberUpdatedState(positionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestMediaNotificationDismissed by rememberUpdatedState(mediaNotificationDismissed)

    DisposableEffect(Unit) {
        onDispose {
            val music = latestCurrentMusic
            val isStillPlaying = runCatching { player.isPlaying }
                .getOrDefault(latestIsPlaying)
            saveCurrentPlaybackMemory()
            if (music != null && isStillPlaying && !latestMediaNotificationDismissed) {
                WaveMusicForegroundState.update(
                    music = music,
                    isPlaying = true,
                    isLiked = music.id in latestLikedIds,
                    positionMs = latestPositionMs,
                    durationMs = latestDurationMs.takeIf { it > 0L } ?: music.durationMs,
                    sessionToken = mediaSession.sessionToken
                )
                WaveMusicPlaybackService.start(appContext)
            } else {
                notificationController.cancel()
                WaveMusicForegroundState.clear()
                WaveMusicPlaybackService.stop(appContext)
                runCatching { equalizer?.release() }
                WaveMusicPlayerHolder.releasePlayer(player)
                WaveMusicPlayerHolder.releaseMediaSession(mediaSession)
            }
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
        NeonVisualizer(
            isPlaying = isPlaying,
            seed = selectedMusic?.id ?: selectedTab.ordinal.toLong(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(170.dp)
                .alpha(0.16f),
            bars = 42
        )

        AnimatedContent(
            targetState = showNowPlaying && selectedMusic != null,
            transitionSpec = {
                val direction = if (targetState) 1 else -1
                (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it * direction / 3 }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { -it * direction / 4 })
            },
            label = "root-player-transition"
        ) { showingPlayer ->
            if (showingPlayer && selectedMusic != null) {
                NowPlayingScreen(
                    music = selectedMusic,
                    isPlaying = isPlaying,
                    isLiked = selectedMusic.id in likedIds,
                    playlists = playlists,
                    queueSongs = activeQueueSongs,
                    lyricsText = remember(selectedMusic.id, lyricsRevision) {
                        libraryStorage.loadLyrics(selectedMusic.id)
                    },
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
                    onPlayNext = ::playNextInQueue,
                    onRemoveFromQueue = ::removeFromQueue,
                    onMoveQueueItem = ::moveQueueItem,
                    onClearQueue = ::clearQueue,
                    onSaveQueueAsPlaylist = ::saveQueueAsPlaylist,
                    onSaveLyrics = ::saveLyrics,
                    onSaveMetadata = ::saveTagOverride,
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
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it * direction / 5 }) togetherWith
                                (fadeOut(tween(140)) + slideOutHorizontally(tween(220)) { -it * direction / 6 })
                        },
                        label = "tab-content-transition",
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
                            onRequestPermission = { permissionLauncher.launch(audioPermissions) },
                            onRefresh = ::reloadSongs,
                            onSongClick = ::playMusicFromLibrary,
                            onToggleLike = ::toggleLike,
                            onAddToPlaylist = ::addToPlaylist,
                            onPlaylistClick = ::playPlaylistFromHome,
                            onAddToQueue = ::addToQueue,
                            onPlayNext = ::playNextInQueue,
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
                            onPlayNext = ::playNextInQueue,
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
                            onPlayNext = ::playNextInQueue,
                            onRemoveFromQueue = ::removeFromQueue,
                            onCreatePlaylistFromFolder = ::createPlaylistFromFolder
                        )

                        WaveTab.Settings -> SettingsScreen(
                            audioQuality = audioQuality,
                            equalizerPreset = equalizerPreset,
                            customEqualizerGains = customEqualizerGains,
                            accentTheme = accentTheme,
                            notificationsEnabled = notificationsEnabled,
                            crossfadeEnabled = crossfadeEnabled,
                            continueListeningEnabled = continueListeningEnabled,
                            resumePlaybackPositionEnabled = resumePlaybackPositionEnabled,
                            localVideosEnabled = localVideosEnabled,
                            sleepTimerLabel = sleepTimerLabel,
                            appVersion = BuildConfig.VERSION_NAME,
                            onAudioQualitySelected = ::updateAudioQuality,
                            onEqualizerSelected = ::updateEqualizer,
                            onAccentThemeSelected = ::updateAccentTheme,
                            onNotificationsChanged = ::setMediaNotificationsEnabled,
                            onCrossfadeChanged = ::updateCrossfade,
                            onContinueListeningChanged = ::updateContinueListening,
                            onResumePlaybackPositionChanged = ::updateResumePlaybackPosition,
                            onLocalVideosChanged = ::setLocalVideosEnabled,
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
}

