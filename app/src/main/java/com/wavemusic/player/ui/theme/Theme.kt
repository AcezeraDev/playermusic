package com.wavemusic.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = WavePurple,
    secondary = WaveBlue,
    tertiary = WavePink,
    background = WaveBackground,
    surface = WaveSurface,
    surfaceVariant = WaveSurfaceBright,
    onPrimary = WaveTextPrimary,
    onSecondary = WaveBackground,
    onTertiary = WaveTextPrimary,
    onBackground = WaveTextPrimary,
    onSurface = WaveTextPrimary,
    onSurfaceVariant = WaveTextSecondary
)

@Composable
fun WaveMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

