package com.wavemusic.player.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wavemusic.player.data.model.Music
import com.wavemusic.player.data.model.Playlist
import com.wavemusic.player.data.model.activeLyricsIndex
import com.wavemusic.player.data.model.formatDuration
import com.wavemusic.player.data.model.parseLyrics
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.components.AnimatedIconButton
import com.wavemusic.player.ui.components.GlassCard
import com.wavemusic.player.ui.components.MusicIconCluster
import com.wavemusic.player.ui.components.NeonVisualizer
import com.wavemusic.player.ui.components.NeonCard
import com.wavemusic.player.ui.theme.WaveBackground
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun NowPlayingScreen(
    music: Music,
    isPlaying: Boolean,
    isLiked: Boolean,
    playlists: List<Playlist>,
    queueSongs: List<Music>,
    lyricsText: String,
    positionMs: Long,
    durationMs: Long,
    videoAspectRatio: Float = 16f / 9f,
    repeatEnabled: Boolean,
    shuffleEnabled: Boolean,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    onAddToQueue: (Music) -> Unit,
    onPlayNext: (Music) -> Unit,
    onRemoveFromQueue: (Music) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onSaveLyrics: (Music, String) -> Unit,
    onSaveMetadata: (Music, String, String, String) -> Unit,
    onVideoSurfaceReady: (SurfaceHolder?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playlistMenuExpanded by remember { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showMetadataEditor by rememberSaveable { mutableStateOf(false) }
    var showFullscreenVideo by rememberSaveable(music.id) { mutableStateOf(false) }
    var dragAmount by remember { mutableFloatStateOf(0f) }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = tween(durationMillis = 320),
        label = "artwork-scale"
    )
    val ambientTransition = rememberInfiniteTransition(label = "now-playing-ambient")
    val ambientAlpha by ambientTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "now-playing-ambient-alpha"
    )
    val artworkRotation by ambientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 70000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "now-playing-cover-rotation"
    )
    val artworkSway = remember(music.id) { Animatable(0f) }

    LaunchedEffect(isPlaying, music.id) {
        if (isPlaying) {
            while (true) {
                artworkSway.animateTo(
                    targetValue = 6f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
                )
                artworkSway.animateTo(
                    targetValue = -6f,
                    animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
                )
            }
        } else {
            artworkSway.stop()
            artworkSway.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            )
        }
    }

    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
            .pointerInput(music.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onHorizontalDrag = { _, amount -> dragAmount += amount },
                    onDragEnd = {
                        when {
                            dragAmount > 120f -> onPrevious()
                            dragAmount < -120f -> onNext()
                        }
                    }
                )
            }
    ) {
        AlbumArtwork(
            music = music,
            modifier = Modifier
                .fillMaxSize()
                .blur(42.dp)
                .alpha(0.22f),
            cornerRadius = 0.dp
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            WavePurple.copy(alpha = ambientAlpha),
                            WaveBackground.copy(alpha = 0.92f),
                            WaveBlue.copy(alpha = ambientAlpha * 0.34f),
                            WaveBackground
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(
                onBack = onBack,
                onEdit = { showMetadataEditor = true }
            )

            Spacer(modifier = Modifier.height(18.dp))

        Box(contentAlignment = Alignment.Center) {
                if (music.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(WaveSurface.copy(alpha = 0.78f), RoundedCornerShape(36.dp))
                            .clip(RoundedCornerShape(36.dp))
                    ) {
                        if (!showFullscreenVideo) {
                            AspectVideoSurface(
                                onSurfaceReady = onVideoSurfaceReady,
                                aspectRatio = videoAspectRatio,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        IconButton(
                            onClick = { showFullscreenVideo = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(WaveSurface.copy(alpha = 0.82f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Fullscreen, "Tela cheia", tint = WaveTextPrimary)
                        }
                        if (showFullscreenVideo) {
                            Text(
                                text = "Abrindo tela cheia",
                                color = WaveTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                } else {
                    AlbumArtwork(
                        music = music,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                                rotationZ = artworkSway.value + if (isPlaying) artworkRotation else 0f
                            },
                        cornerRadius = 36.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Crossfade(targetState = music, label = "now-playing-track-crossfade") { displayedMusic ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayedMusic.title,
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayedMusic.artist,
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
            Slider(
                value = progress,
                onValueChange = onSeek,
                colors = SliderDefaults.colors(
                    thumbColor = WavePink,
                    activeTrackColor = WavePink,
                    inactiveTrackColor = WaveSurfaceBright
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(positionMs), color = WaveTextSecondary)
                Text(formatDuration(durationMs), color = WaveTextSecondary)
            }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            CleanPlaybackControls(
                isPlaying = isPlaying,
                shuffleEnabled = shuffleEnabled,
                repeatEnabled = repeatEnabled,
                onToggleShuffle = onToggleShuffle,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onToggleRepeat = onToggleRepeat
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundAction(
                    selected = isLiked,
                    icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = "Curtir",
                    onClick = { onToggleLike(music) }
                )
                Box {
                    RoundAction(
                        selected = false,
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        label = "Playlist",
                        onClick = { playlistMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = playlistMenuExpanded,
                        onDismissRequest = { playlistMenuExpanded = false }
                    ) {
                        if (playlists.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Crie uma playlist na Biblioteca") },
                                enabled = false,
                                onClick = {}
                            )
                        } else {
                            playlists.forEach { playlist ->
                                DropdownMenuItem(
                                    text = { Text(playlist.name) },
                                    onClick = {
                                        playlistMenuExpanded = false
                                        onAddToPlaylist(music, playlist)
                                    }
                                )
                            }
                        }
                    }
                }
                RoundAction(
                    selected = false,
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "Fila",
                    onClick = { showQueue = true }
                )
                RoundAction(
                    selected = false,
                    icon = Icons.Rounded.Subtitles,
                    label = "Letra",
                    onClick = { showLyrics = true }
                )
                RoundAction(
                    selected = false,
                    icon = Icons.Rounded.Share,
                    label = "Enviar",
                    onClick = { shareMusic(context, music) }
                )
            }

            AnimatedVisibility(visible = queueSongs.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showQueue = true },
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = WaveBlue)
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Próxima na fila", color = WaveTextPrimary, fontWeight = FontWeight.Bold)
                            Text(queueSongs.first().title, color = WaveTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    if (showQueue) {
        QueueDialog(
            queueSongs = queueSongs,
            onDismiss = { showQueue = false },
            onAddCurrent = { onAddToQueue(music) },
            onRemove = onRemoveFromQueue,
            onMove = onMoveQueueItem,
            onClear = onClearQueue,
            onSaveAsPlaylist = onSaveQueueAsPlaylist
        )
    }

    if (music.isVideo && showFullscreenVideo) {
        FullscreenVideoDialog(
            title = music.title,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            videoAspectRatio = videoAspectRatio,
            onSeek = onSeek,
            onPlayPause = onPlayPause,
            onDismiss = { showFullscreenVideo = false },
            onVideoSurfaceReady = onVideoSurfaceReady
        )
    }

    if (showLyrics) {
        LyricsDialog(
            music = music,
            lyricsText = lyricsText,
            positionMs = positionMs,
            onSaveLyrics = { onSaveLyrics(music, it) },
            onDismiss = { showLyrics = false }
        )
    }

    if (showMetadataEditor) {
        MetadataEditorDialog(
            music = music,
            onDismiss = { showMetadataEditor = false },
            onSave = { title, artist, album ->
                onSaveMetadata(music, title, artist, album)
                showMetadataEditor = false
            }
        )
    }
}

@Composable
private fun FullscreenVideoDialog(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    videoAspectRatio: Float,
    onSeek: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onDismiss: () -> Unit,
    onVideoSurfaceReady: (SurfaceHolder?) -> Unit
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AspectVideoSurface(
                onSurfaceReady = onVideoSurfaceReady,
                aspectRatio = videoAspectRatio,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(WaveBackground.copy(alpha = 0.76f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Sair da tela cheia", tint = WaveTextPrimary)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 12.dp)
            ) {
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = WavePink,
                        activeTrackColor = WavePink,
                        inactiveTrackColor = WaveTextSecondary.copy(alpha = 0.34f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Tocar",
                            tint = WaveTextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        text = "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = WaveTextPrimary)
        }
        Text(
            text = "Tocando agora",
            color = WaveTextSecondary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, "Editar informacoes", tint = WaveTextPrimary)
        }
    }
}

@Composable
private fun CleanPlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WaveSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompactControlButton(
                selected = shuffleEnabled,
                icon = Icons.Rounded.Shuffle,
                contentDescription = "Aleatorio",
                onClick = onToggleShuffle
            )
            MainControlButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Musica anterior",
                onClick = onPrevious
            )
            MainControlButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Tocar",
                onClick = onPlayPause,
                primary = true
            )
            MainControlButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Proxima musica",
                onClick = onNext
            )
            CompactControlButton(
                selected = repeatEnabled,
                icon = Icons.Rounded.Repeat,
                contentDescription = "Repetir",
                onClick = onToggleRepeat
            )
        }
    }
}

@Composable
private fun MainControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val size = if (primary) 72.dp else 54.dp
    AnimatedIconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        background = if (primary) {
            Brush.linearGradient(listOf(WavePink, WavePurple))
        } else {
            Brush.linearGradient(listOf(WaveSurfaceBright.copy(alpha = 0.92f), WaveSurfaceBright.copy(alpha = 0.58f)))
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = WaveTextPrimary,
            modifier = Modifier.size(if (primary) 38.dp else 30.dp)
        )
    }
}

@Composable
private fun CompactControlButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    AnimatedIconButton(
        onClick = onClick,
        modifier = Modifier.size(46.dp),
        background = if (selected) {
            Brush.linearGradient(listOf(WaveBlue, WavePurple))
        } else {
            Brush.linearGradient(listOf(WaveSurfaceBright.copy(alpha = 0.72f), WaveSurface.copy(alpha = 0.72f)))
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) WaveTextPrimary else WaveTextSecondary,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun AspectVideoSurface(
    onSurfaceReady: (SurfaceHolder?) -> Unit,
    aspectRatio: Float,
    modifier: Modifier = Modifier
) {
    val safeAspectRatio = aspectRatio
        .takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(0.45f, 2.4f)
        ?: 16f / 9f

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val containerAspectRatio = (maxWidth.value / maxHeight.value)
            .takeIf { it.isFinite() && it > 0f }
            ?: safeAspectRatio
        val videoModifier = if (containerAspectRatio > safeAspectRatio) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(safeAspectRatio)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(safeAspectRatio)
        }

        VideoSurface(
            onSurfaceReady = onSurfaceReady,
            modifier = videoModifier
        )
    }
}

@Composable
private fun VideoSurface(
    onSurfaceReady: (SurfaceHolder?) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.background(WaveBackground),
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        onSurfaceReady(holder)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        onSurfaceReady(holder)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        onSurfaceReady(null)
                    }
                })
            }
        }
    )
}

@Composable
private fun RoundAction(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "round-action-selected-scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    shadowElevation = if (selected) 20f else 8f
                },
            background = if (selected) {
                Brush.linearGradient(listOf(WavePink, WavePurple))
            } else {
                Brush.linearGradient(listOf(WaveSurfaceBright, WaveSurface.copy(alpha = 0.9f)))
            }
        ) {
            Icon(icon, label, tint = if (selected) WaveTextPrimary else WaveTextSecondary)
        }
        Text(label, color = WaveTextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ToggleIcon(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    AnimatedIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        background = if (selected) {
            Brush.linearGradient(listOf(WaveBlue, WavePurple))
        } else {
            Brush.linearGradient(listOf(WaveSurfaceBright.copy(alpha = 0.88f), WaveSurfaceBright.copy(alpha = 0.6f)))
        }
    ) {
        Icon(icon, contentDescription, tint = if (selected) WaveTextPrimary else WaveTextSecondary)
    }
}

@Composable
private fun QueueDialog(
    queueSongs: List<Music>,
    onDismiss: () -> Unit,
    onAddCurrent: () -> Unit,
    onRemove: (Music) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    onSaveAsPlaylist: (String) -> Unit
) {
    var playlistName by rememberSaveable { mutableStateOf("Fila salva") }
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = listOf(WavePurple, WaveBlue),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                DialogHeader("Fila de reprodução", "${queueSongs.size} músicas na fila", onDismiss)

                Spacer(modifier = Modifier.size(12.dp))

                if (queueSongs.isEmpty()) {
                    EmptyMusicScreen(
                        title = "Fila vazia",
                        message = "Adicione músicas pelo menu de três pontos ou pelo player principal.",
                        actionText = "Adicionar atual",
                        onAction = onAddCurrent
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(queueSongs, key = { index, item -> "${item.id}-$index" }) { index, item ->
                            QueueRow(
                                music = item,
                                index = index,
                                count = queueSongs.size,
                                onMove = onMove,
                                onRemove = onRemove
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Nome da playlist") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WaveTextPrimary,
                            unfocusedTextColor = WaveTextPrimary,
                            focusedBorderColor = WaveBlue,
                            unfocusedBorderColor = WaveSurfaceBright,
                            focusedContainerColor = WaveSurface.copy(alpha = 0.48f),
                            unfocusedContainerColor = WaveSurface.copy(alpha = 0.36f),
                            cursorColor = WaveBlue
                        )
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onSaveAsPlaylist(playlistName) }) {
                            Text("Salvar como playlist", color = WaveBlue)
                        }
                        TextButton(onClick = onClear) {
                            Text("Limpar fila", color = WavePink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    music: Music,
    index: Int,
    count: Int,
    onMove: (Int, Int) -> Unit,
    onRemove: (Music) -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    GlassCard(
        modifier = Modifier.pointerInput(index, count) {
            detectVerticalDragGestures(
                onDragStart = { dragOffset = 0f },
                onVerticalDrag = { _, amount -> dragOffset += amount },
                onDragEnd = {
                    when {
                        dragOffset < -44f -> onMove(index, index - 1)
                        dragOffset > 44f -> onMove(index, index + 1)
                    }
                    dragOffset = 0f
                }
            )
        },
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(music, modifier = Modifier.size(44.dp), cornerRadius = 12.dp)
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(music.title, color = WaveTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(music.artist, color = WaveTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                Icon(Icons.Rounded.KeyboardArrowUp, "Subir", tint = WaveTextSecondary)
            }
            IconButton(onClick = { onMove(index, index + 1) }, enabled = index < count - 1) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Descer", tint = WaveTextSecondary)
            }
            IconButton(onClick = { onRemove(music) }) {
                Icon(Icons.Rounded.Delete, "Remover da fila", tint = WavePink)
            }
        }
    }
}

@Composable
private fun LyricsDialog(
    music: Music,
    lyricsText: String,
    positionMs: Long,
    onSaveLyrics: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editing by rememberSaveable(music.id) { mutableStateOf(lyricsText.isBlank()) }
    var draft by remember(music.id, lyricsText) { mutableStateOf(lyricsText) }
    val lines = remember(draft) { parseLyrics(draft) }
    val activeIndex = activeLyricsIndex(lines, positionMs)
    val importLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.onSuccess { text ->
                if (text.isNotBlank()) {
                    draft = text
                    editing = true
                    Toast.makeText(context, "Letra importada.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Arquivo de letra vazio.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Nao foi possivel importar a letra.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = listOf(WavePink, WavePurple),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column {
                DialogHeader("Letra", music.title, onDismiss)
                Spacer(modifier = Modifier.size(12.dp))
                Surface(
                    color = WavePurple.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(22.dp),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (editing) {
                            Text(
                                text = "Cole uma letra comum ou .lrc com tempos como [01:23.45].",
                                color = WaveTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { importLyricsLauncher.launch(arrayOf("text/*", "application/octet-stream")) }) {
                                Text("Importar .lrc/.txt", color = WaveBlue)
                            }
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 220.dp, max = 360.dp),
                                placeholder = { Text("Letra local") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = WaveTextPrimary,
                                    unfocusedTextColor = WaveTextPrimary,
                                    focusedBorderColor = WavePink,
                                    unfocusedBorderColor = WaveSurfaceBright,
                                    focusedContainerColor = WaveSurface.copy(alpha = 0.48f),
                                    unfocusedContainerColor = WaveSurface.copy(alpha = 0.36f),
                                    cursorColor = WavePink
                                )
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = false }) {
                                    Text("Cancelar", color = WaveTextSecondary)
                                }
                                Button(
                                    onClick = {
                                        onSaveLyrics(draft)
                                        editing = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text("Salvar")
                                }
                            }
                        } else if (lines.isEmpty()) {
                            Text(
                                text = "Sem letra local encontrada.",
                                color = WaveTextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cole uma letra para salvar no aparelho.",
                                color = WaveTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { editing = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text("Adicionar letra")
                            }
                            TextButton(onClick = { importLyricsLauncher.launch(arrayOf("text/*", "application/octet-stream")) }) {
                                Text("Importar .lrc/.txt", color = WaveBlue)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(lines, key = { index, item -> "${item.timeMs}-$index-${item.text}" }) { index, line ->
                                    val active = index == activeIndex
                                    Text(
                                        text = line.text,
                                        color = if (active) WaveTextPrimary else WaveTextSecondary,
                                        style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (active) WavePink.copy(alpha = 0.18f) else Color.Transparent)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { importLyricsLauncher.launch(arrayOf("text/*", "application/octet-stream")) }) {
                                    Text("Importar", color = WaveTextSecondary)
                                }
                                TextButton(onClick = { editing = true }) {
                                    Text("Editar letra", color = WaveBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataEditorDialog(
    music: Music,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by rememberSaveable(music.id) { mutableStateOf(music.title) }
    var artist by rememberSaveable(music.id) { mutableStateOf(music.artist) }
    var album by rememberSaveable(music.id) { mutableStateOf(music.album) }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = listOf(WaveBlue, WavePurple),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogHeader("Editar informacoes", "Mudanca local dentro do Wave Music.", onDismiss)
                MetadataField("Titulo", title, onValueChange = { title = it })
                MetadataField("Artista", artist, onValueChange = { artist = it })
                MetadataField("Album", album, onValueChange = { album = it })
                Text(
                    text = "O arquivo original nao e regravado. Isso evita pedir permissao especial de escrita no Android.",
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(WaveSurfaceBright.copy(alpha = 0.28f))
                        .padding(12.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = WaveTextSecondary)
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = { onSave(title, artist, album) },
                        colors = ButtonDefaults.buttonColors(containerColor = WavePink),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WaveTextPrimary,
            unfocusedTextColor = WaveTextPrimary,
            focusedBorderColor = WaveBlue,
            unfocusedBorderColor = WaveSurfaceBright,
            focusedContainerColor = WaveSurface.copy(alpha = 0.48f),
            unfocusedContainerColor = WaveSurface.copy(alpha = 0.36f),
            cursorColor = WaveBlue,
            focusedLabelColor = WaveBlue,
            unfocusedLabelColor = WaveTextSecondary
        )
    )
}

@Composable
private fun DialogHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WaveTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = WaveTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, "Fechar", tint = WaveTextSecondary)
        }
    }
}

private fun shareMusic(context: Context, music: Music) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, music.uri)
        putExtra(Intent.EXTRA_TEXT, "Estou ouvindo ${music.title} - ${music.artist} no Wave Music.")
        clipData = ClipData.newUri(context.contentResolver, music.title, music.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar música"))
    }.onFailure { error ->
        if (error is ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app disponível para compartilhar.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Não foi possível compartilhar esta música.", Toast.LENGTH_SHORT).show()
        }
    }
}

