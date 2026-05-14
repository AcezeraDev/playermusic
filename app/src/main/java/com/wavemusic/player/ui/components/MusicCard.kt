package com.wavemusic.player.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun MusicCard(
    music: Music,
    isCurrent: Boolean,
    isLiked: Boolean,
    playlists: List<Playlist>,
    onClick: (Music) -> Unit,
    onToggleLike: (Music) -> Unit,
    onAddToPlaylist: (Music, Playlist) -> Unit,
    isQueued: Boolean = false,
    onAddToQueue: (Music) -> Unit = {},
    onRemoveFromQueue: (Music) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick(music) },
        color = if (isCurrent) WaveSurface.copy(alpha = 0.98f) else WaveSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(
                music = music,
                modifier = Modifier.size(62.dp),
                cornerRadius = 18.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = music.title,
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLiked) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Música curtida",
                            tint = WavePink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = music.artist,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = music.album,
                    color = WaveTextSecondary.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = music.duration,
                color = if (isCurrent) WaveBlue else WaveTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Mais opções",
                        tint = WaveTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isLiked) "Remover curtida" else "Curtir música") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleLike(music)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Compartilhar música") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            shareMusic(context, music)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(if (isQueued) "Excluir da fila" else "Adicionar à fila") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isQueued) Icons.Rounded.PlaylistRemove else Icons.Rounded.QueueMusic,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (isQueued) onRemoveFromQueue(music) else onAddToQueue(music)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ver detalhes da música") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showDetails = true
                        }
                    )

                    HorizontalDivider()

                    if (playlists.isNotEmpty()) {
                        Text(
                            text = "Adicionar à playlist",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist(music, playlist)
                                }
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = { Text("Crie uma playlist na Biblioteca") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null
                                )
                            },
                            enabled = false,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }

    if (showDetails) {
        MusicDetailsDialog(
            music = music,
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
private fun MusicDetailsDialog(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(WavePurple, WavePink))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = WaveTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detalhes da música",
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Informações lidas do dispositivo",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                DetailRow("Música", music.title)
                DetailRow("Artista", music.artist)
                DetailRow("Álbum", music.album)
                DetailRow("Duração", music.duration)
                DetailRow("Pasta", music.folder)
                DetailRow("Tipo", music.mimeType)
                DetailRow("Tamanho", music.sizeLabel)
                DetailRow("ID local", music.id.toString())

                Spacer(modifier = Modifier.size(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WavePurple),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = WaveSurfaceBright.copy(alpha = 0.36f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
