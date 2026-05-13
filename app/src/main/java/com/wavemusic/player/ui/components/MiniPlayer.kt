package com.wavemusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun MiniPlayer(
    music: Music?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(enabled = music != null) { onClick() },
        color = WaveSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    music = music,
                    modifier = Modifier.size(54.dp),
                    cornerRadius = 16.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music?.title ?: "Escolha uma música",
                        color = WaveTextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = music?.artist ?: "Suas músicas baixadas aparecem aqui",
                        color = WaveTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPause, enabled = music != null) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(WavePurple, WavePink))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Tocar",
                            tint = WaveTextPrimary
                        )
                    }
                }

                IconButton(onClick = onNext, enabled = music != null) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Próxima música",
                        tint = WaveBlue
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = WavePink,
                trackColor = WavePurple.copy(alpha = 0.18f)
            )
        }
    }
}
