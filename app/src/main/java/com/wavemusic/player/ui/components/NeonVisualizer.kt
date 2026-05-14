package com.wavemusic.player.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.absoluteValue
import kotlin.math.sin

@Composable
fun NeonVisualizer(
    isPlaying: Boolean,
    seed: Long,
    modifier: Modifier = Modifier,
    bars: Int = 22,
    primary: Color = Color(0xFFEC4899),
    secondary: Color = Color(0xFF22D3EE)
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920),
            repeatMode = RepeatMode.Reverse
        ),
        label = "visualizer-phase"
    )

    Canvas(modifier = modifier) {
        val spacing = size.width / bars.coerceAtLeast(1)
        val baseHeight = size.height * 0.22f
        val activeRange = size.height * if (isPlaying) 0.68f else 0.18f
        val stroke = spacing * 0.34f

        repeat(bars) { index ->
            val wave = sin((index + seed % 11) * 0.68f + phase * 6.28f).absoluteValue
            val barHeight = baseHeight + activeRange * wave
            val x = spacing * index + spacing / 2f
            val color = if (index % 2 == 0) primary else secondary
            drawLine(
                color = color.copy(alpha = 0.28f),
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = stroke * 2.1f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color.copy(alpha = if (isPlaying) 0.9f else 0.42f),
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
