package com.wavemusic.player.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.model.Music
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
    onClose: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 180f),
        label = "mini-player-progress"
    )
    val motion = rememberInfiniteTransition(label = "mini-player-cover-motion")
    val coverSway by motion.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mini-player-cover-sway"
    )

    NeonCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        enabled = music != null,
        onClick = if (music != null) onClick else null,
        shape = RoundedCornerShape(30.dp),
        colors = if (isPlaying) listOf(WavePink, WaveBlue) else listOf(WavePurple, WaveSurface),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AlbumArtwork(
                        music = music,
                        modifier = Modifier
                            .size(54.dp)
                            .graphicsLayer {
                                rotationZ = if (isPlaying) coverSway else 0f
                                scaleX = if (isPlaying) 1.03f else 1f
                                scaleY = if (isPlaying) 1.03f else 1f
                            },
                        cornerRadius = 16.dp
                    )
                    NeonIconOrb(
                        icon = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        size = 24.dp,
                        iconSize = 13.dp,
                        colors = if (isPlaying) listOf(WavePink, WaveBlue) else listOf(WavePurple, WaveSurface),
                        active = isPlaying
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Crossfade(
                    targetState = music,
                    label = "mini-player-track-crossfade",
                    modifier = Modifier.weight(1f)
                ) { track ->
                    Column {
                        Text(
                            text = track?.title ?: "Escolha uma musica",
                            color = WaveTextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artist ?: "Suas musicas baixadas aparecem aqui",
                            color = WaveTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AnimatedIconButton(
                    onClick = onPlayPause,
                    enabled = music != null,
                    modifier = Modifier.size(42.dp),
                    background = Brush.linearGradient(listOf(WavePurple, WavePink))
                ) {
                    Crossfade(targetState = isPlaying, label = "mini-player-play-icon") { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Pausar" else "Tocar",
                            tint = WaveTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedIconButton(
                    onClick = onNext,
                    enabled = music != null,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Proxima musica",
                        tint = WaveBlue
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                AnimatedIconButton(
                    onClick = onClose,
                    enabled = music != null,
                    modifier = Modifier.size(38.dp),
                    background = Brush.linearGradient(listOf(WaveSurface, WavePurple.copy(alpha = 0.72f)))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fechar player",
                        tint = WaveTextSecondary
                    )
                }
            }

            NeonVisualizer(
                isPlaying = isPlaying,
                seed = music?.id ?: 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .padding(horizontal = 14.dp),
                bars = 26
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = WavePink,
                trackColor = WavePurple.copy(alpha = 0.18f)
            )
        }
    }
}

