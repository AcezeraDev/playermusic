package com.wavemusic.player.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import com.wavemusic.player.data.formatDuration
import com.wavemusic.player.ui.components.AlbumArtwork
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurfaceBright
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun NowPlayingScreen(
    music: Music,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var repeatEnabled by rememberSaveable { mutableStateOf(false) }
    var shuffleEnabled by rememberSaveable { mutableStateOf(false) }
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = tween(durationMillis = 320),
        label = "artwork-scale"
    )
    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Voltar",
                    tint = WaveTextPrimary
                )
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

        Spacer(modifier = Modifier.height(28.dp))

        AlbumArtwork(
            music = music,
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                },
            cornerRadius = 34.dp
        )

        Spacer(modifier = Modifier.height(30.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

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
            Text(
                text = formatDuration(positionMs),
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatDuration(durationMs),
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ToggleIcon(
                selected = shuffleEnabled,
                icon = Icons.Rounded.Shuffle,
                contentDescription = "Aleatório",
                onClick = { shuffleEnabled = !shuffleEnabled }
            )
            IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Música anterior",
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WavePurple, WavePink)))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Tocar",
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Próxima música",
                    tint = WaveTextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            ToggleIcon(
                selected = repeatEnabled,
                icon = Icons.Rounded.Repeat,
                contentDescription = "Repetir",
                onClick = { repeatEnabled = !repeatEnabled }
            )
        }
    }
}

@Composable
private fun ToggleIcon(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(WaveBlue, WavePurple))
                } else {
                    Brush.linearGradient(
                        listOf(
                            WaveSurfaceBright.copy(alpha = 0.88f),
                            WaveSurfaceBright.copy(alpha = 0.6f)
                        )
                    )
                }
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) WaveTextPrimary else WaveTextSecondary
        )
    }
}
