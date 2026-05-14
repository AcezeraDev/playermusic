package com.wavemusic.player.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import kotlin.math.absoluteValue

@Composable
fun PlaylistCover(
    imageUri: String?,
    seed: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp
) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            imageUri?.let { rawUri ->
                runCatching {
                    val uri = Uri.parse(rawUri)
                    val input = if (uri.scheme == "file") {
                        FileInputStream(uri.path.orEmpty())
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                    input?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
    ) {
        val cover = bitmap
        if (cover != null) {
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = "Capa da playlist",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            DefaultPlaylistCover(
                seed = seed,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DefaultPlaylistCover(
    seed: Long,
    modifier: Modifier = Modifier
) {
    val palette = playlistPalette(seed)

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = palette,
                start = Offset.Zero,
                end = Offset.Infinite
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.78f, size.height * 0.16f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.12f, size.height * 0.86f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = null,
            tint = WaveTextPrimary.copy(alpha = 0.9f),
            modifier = Modifier
                .fillMaxSize(0.38f)
                .padding(8.dp)
        )
    }
}

private fun playlistPalette(seed: Long): List<Color> {
    val palettes = listOf(
        listOf(WavePurple, WavePink),
        listOf(WaveBlue, WavePurple),
        listOf(WavePink, Color(0xFF22D3EE)),
        listOf(Color(0xFF14B8A6), WavePurple),
        listOf(Color(0xFF60A5FA), Color(0xFFF472B6))
    )
    return palettes[(seed.absoluteValue % palettes.size).toInt()]
}

