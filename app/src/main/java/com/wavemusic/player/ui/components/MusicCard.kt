package com.wavemusic.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.Playlist
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WaveSurface
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
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

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

                    if (playlists.isNotEmpty()) {
                        HorizontalDivider()
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
                    }
                }
            }
        }
    }
}
