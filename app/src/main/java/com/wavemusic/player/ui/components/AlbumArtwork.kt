package com.wavemusic.player.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wavemusic.player.data.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.sin

@Composable
fun AlbumArtwork(
    music: Music?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp
) {
    val context = LocalContext.current
    var bitmap by remember(music?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(music?.id) {
        bitmap = music?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, it.uri)
                    val picture = retriever.embeddedPicture
                    retriever.release()
                    picture?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
    ) {
        val art = bitmap
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "Capa do álbum",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            GeneratedArtwork(
                seed = music?.id ?: 1L,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GeneratedArtwork(seed: Long, modifier: Modifier = Modifier) {
    val colors = colorSet(seed)

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset.Infinite
            )
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val stroke = size.minDimension * 0.055f
            val baseline = size.height * 0.62f
            val spacing = size.width / 7f

            for (index in 0..6) {
                val energy = sin((seed % 17 + index) * 0.8f).absoluteValue
                val height = size.height * (0.24f + energy * 0.42f)
                val x = spacing * (index + 0.5f)
                drawLine(
                    color = Color.White.copy(alpha = 0.72f),
                    start = Offset(x, baseline - height / 2f),
                    end = Offset(x, baseline + height / 2f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = size.minDimension * 0.22f,
                center = Offset(size.width * 0.78f, size.height * 0.2f)
            )
        }
    }
}

private fun colorSet(seed: Long): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
        listOf(Color(0xFF22D3EE), Color(0xFF6366F1)),
        listOf(Color(0xFFF43F5E), Color(0xFF7C3AED)),
        listOf(Color(0xFF14B8A6), Color(0xFFA855F7)),
        listOf(Color(0xFF60A5FA), Color(0xFFF472B6))
    )
    return palettes[(seed.absoluteValue % palettes.size).toInt()]
}
