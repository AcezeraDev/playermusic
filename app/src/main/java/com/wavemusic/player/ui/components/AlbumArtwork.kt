package com.wavemusic.player.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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

private data class ArtworkFrame(
    val musicId: Long,
    val bitmap: Bitmap?
)

@Composable
fun AlbumArtwork(
    music: Music?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp
) {
    val context = LocalContext.current
    var bitmap by remember(music?.id) { mutableStateOf<Bitmap?>(null) }
    val transitionBurst = remember { Animatable(0f) }

    LaunchedEffect(music?.id) {
        transitionBurst.snapTo(1f)
        transitionBurst.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(music?.id, music?.uri) {
        bitmap = music?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, it.uri)
                    val picture = if (it.isVideo) {
                        retriever.getFrameAtTime(1_000_000)
                    } else {
                        retriever.embeddedPicture?.let { bytes ->
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    }
                    retriever.release()
                    picture
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
    ) {
        AnimatedContent(
            targetState = ArtworkFrame(music?.id ?: -1L, bitmap),
            transitionSpec = {
                val direction = if (targetState.musicId >= initialState.musicId) 1 else -1
                (
                    fadeIn(animationSpec = tween(durationMillis = 260, delayMillis = 70)) +
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing),
                            initialOffsetX = { width -> direction * width / 4 }
                        ) +
                        scaleIn(
                            initialScale = 0.84f,
                            animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing)
                        )
                    ).togetherWith(
                    fadeOut(animationSpec = tween(durationMillis = 180)) +
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                            targetOffsetX = { width -> -direction * width / 6 }
                        ) +
                        scaleOut(
                            targetScale = 1.08f,
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                        )
                ).using(SizeTransform(clip = false))
            },
            label = "artwork-sprite-transition"
        ) { frame ->
            ArtworkFrameContent(
                frame = frame,
                modifier = Modifier.fillMaxSize()
            )
        }

        SpriteTransitionBurst(
            progress = transitionBurst.value,
            seed = music?.id ?: 1L,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ArtworkFrameContent(
    frame: ArtworkFrame,
    modifier: Modifier = Modifier
) {
    val art = frame.bitmap
    if (art != null) {
        Image(
            bitmap = art.asImageBitmap(),
            contentDescription = "Capa do album",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        GeneratedArtwork(
            seed = frame.musicId.takeIf { it > 0 } ?: 1L,
            modifier = modifier
        )
    }
}

@Composable
private fun SpriteTransitionBurst(
    progress: Float,
    seed: Long,
    modifier: Modifier = Modifier
) {
    if (progress <= 0.01f) return

    Canvas(modifier = modifier) {
        val strength = progress.coerceIn(0f, 1f)
        val reveal = 1f - strength
        val sweepCenter = size.width * reveal
        val sweepWidth = size.width * 0.52f

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.22f * strength),
                    Color.Transparent
                ),
                start = Offset(sweepCenter - sweepWidth, 0f),
                end = Offset(sweepCenter + sweepWidth, size.height)
            ),
            size = size
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.12f * strength),
            radius = size.minDimension * (0.24f + reveal * 0.72f),
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )

        repeat(9) { index ->
            val wave = sin((seed % 31 + index * 7) * 0.42f).absoluteValue
            val x = size.width * ((index + 1) / 10f)
            val y = size.height * (0.18f + wave * 0.64f)
            drawCircle(
                color = Color.White.copy(alpha = 0.2f * strength),
                radius = size.minDimension * (0.012f + wave * 0.018f),
                center = Offset(x, y)
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
