package com.wavemusic.player.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.data.formatDuration
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.components.AnimatedIconButton
import com.wavemusic.player.ui.components.NeonVisualizer
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
    positionMs: Long,
    durationMs: Long,
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
    onRemoveFromQueue: (Music) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playlistMenuExpanded by remember { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var dragAmount by remember { mutableFloatStateOf(0f) }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = tween(durationMillis = 320),
        label = "artwork-scale"
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
                            WavePurple.copy(alpha = 0.34f),
                            WaveBackground.copy(alpha = 0.92f),
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
            TopBar(onBack = onBack)

            Spacer(modifier = Modifier.height(18.dp))

            Box(contentAlignment = Alignment.Center) {
                NeonVisualizer(
                    isPlaying = isPlaying,
                    seed = music.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .alpha(0.72f)
                )
                AlbumArtwork(
                    music = music,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                            rotationZ = artworkSway.value
                        },
                    cornerRadius = 36.dp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = music.title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = music.artist,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeonVisualizer(
                isPlaying = isPlaying,
                seed = music.id + 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                bars = 34
            )

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

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToggleIcon(
                    selected = shuffleEnabled,
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Aleatório",
                    onClick = onToggleShuffle
                )
                AnimatedIconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Música anterior", tint = WaveTextPrimary, modifier = Modifier.size(36.dp))
                }
                AnimatedIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(78.dp),
                    background = Brush.linearGradient(listOf(WavePurple, WavePink))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Tocar",
                        tint = WaveTextPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                AnimatedIconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Próxima música", tint = WaveTextPrimary, modifier = Modifier.size(36.dp))
                }
                ToggleIcon(
                    selected = repeatEnabled,
                    icon = Icons.Rounded.Repeat,
                    contentDescription = "Repetir",
                    onClick = onToggleRepeat
                )
            }

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
                    icon = Icons.Rounded.QueueMusic,
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showQueue = true },
                    color = WaveSurface.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(22.dp),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.QueueMusic, null, tint = WaveBlue)
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
            onClear = onClearQueue
        )
    }

    if (showLyrics) {
        LyricsDialog(
            music = music,
            onDismiss = { showLyrics = false }
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
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
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun RoundAction(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedIconButton(
            onClick = onClick,
            modifier = Modifier.size(46.dp),
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
    onClear: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WaveSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
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
    Surface(
        color = WaveSurfaceBright.copy(alpha = 0.36f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WaveSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                        Text(
                            text = "Sem letra local encontrada.",
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "O Wave Music já tem a tela de letras preparada. Quando você adicionar letras locais no futuro, esta área pode fazer scroll automático junto com a reprodução.",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        listOf("♪ ${music.title}", "♪ ${music.artist}", "♪ Letra sincronizada pronta para receber conteúdo").forEach {
                            Text(it, color = WaveTextSecondary)
                        }
                    }
                }
            }
        }
    }
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
