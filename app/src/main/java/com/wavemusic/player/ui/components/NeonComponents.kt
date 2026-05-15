package com.wavemusic.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.wavemusic.player.ui.theme.WaveBlue
import com.wavemusic.player.ui.theme.WavePink
import com.wavemusic.player.ui.theme.WavePurple
import com.wavemusic.player.ui.theme.WaveSurface
import com.wavemusic.player.ui.theme.WaveSurfaceBright
import com.wavemusic.player.ui.theme.WaveTextPrimary
import com.wavemusic.player.ui.theme.WaveTextSecondary

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(30.dp),
    colors: List<Color> = listOf(WavePurple, WaveBlue),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    AnimatedCard(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = shape,
        color = WaveSurface.copy(alpha = 0.86f),
        contentPadding = PaddingValues(0.dp),
        pressedScale = 0.985f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.firstOrNull()?.copy(alpha = 0.10f) ?: WavePurple.copy(alpha = 0.10f),
                            WaveSurface.copy(alpha = 0.96f),
                            colors.lastOrNull()?.copy(alpha = 0.07f) ?: WaveBlue.copy(alpha = 0.07f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = (colors.lastOrNull() ?: WaveBlue).copy(alpha = 0.14f),
                    shape = shape
                )
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable BoxScope.() -> Unit
) {
    AnimatedCard(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        color = WaveSurface.copy(alpha = 0.74f),
        contentPadding = PaddingValues(0.dp),
        pressedScale = 0.985f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            WaveSurfaceBright.copy(alpha = 0.22f),
                            WaveSurface.copy(alpha = 0.84f)
                        )
                    )
                )
                .border(1.dp, WaveTextPrimary.copy(alpha = 0.08f), shape)
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun NeonIconOrb(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 25.dp,
    colors: List<Color> = listOf(WavePurple, WavePink),
    active: Boolean = false
) {
    val pulseTransition = rememberInfiniteTransition(label = "neon-icon-orb-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon-icon-orb-pulse-scale"
    )
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon-icon-orb-glow-alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) pulse else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "neon-icon-orb-scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (active) 22f * pulse else 10f
            }
            .clip(CircleShape)
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = if (active) glowAlpha else 0.04f))
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = WaveTextPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun MusicIconCluster(
    modifier: Modifier = Modifier,
    icons: List<ImageVector> = listOf(
        Icons.Rounded.MusicNote,
        Icons.Rounded.GraphicEq,
        Icons.Rounded.AutoAwesome
    ),
    colors: List<Color> = listOf(WavePurple, WaveBlue, WavePink)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.take(4).forEachIndexed { index, icon ->
            val color = colors.getOrElse(index % colors.size) { WaveBlue }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f))
                    .border(1.dp, color.copy(alpha = 0.34f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            kotlinx.coroutines.delay(delayMillis.toLong())
        }
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(260)) +
            slideInVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f)) { it / 5 } +
            scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f)),
        exit = fadeOut(animationSpec = tween(160)) +
            slideOutVertically(animationSpec = tween(160)) { it / 8 },
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Rounded.AutoAwesome,
    enabled: Boolean = true,
    colors: List<Color> = listOf(WavePurple, WavePink)
) {
    val pulseTransition = rememberInfiniteTransition(label = "neon-button-pulse")
    val glow by pulseTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon-button-glow"
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer { shadowElevation = 16f * glow }
                .background(Brush.linearGradient(colors), RoundedCornerShape(100.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = WaveTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, color = WaveTextPrimary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun GradientHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "WAVE MUSIC",
    icon: ImageVector = Icons.Rounded.AutoAwesome,
    colors: List<Color> = listOf(WavePurple, WavePink, WaveBlue),
    trailing: (@Composable BoxScope.() -> Unit)? = null
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        colors = colors,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    color = colors.lastOrNull() ?: WaveBlue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = title,
                    color = WaveTextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = WaveTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                MusicIconCluster(
                    modifier = Modifier.padding(top = 12.dp),
                    colors = colors
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(colors))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = WaveTextPrimary, modifier = Modifier.size(34.dp))
                trailing?.invoke(this)
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.AutoAwesome,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = listOf(WaveBlue, WavePurple),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WavePurple, WavePink, WaveBlue)))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                NeonIconOrb(
                    icon = icon,
                    contentDescription = null,
                    size = 72.dp,
                    iconSize = 34.dp,
                    colors = listOf(WavePurple, WavePink, WaveBlue),
                    active = true
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                NeonButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Color = WaveBlue,
    icon: ImageVector = Icons.Rounded.MusicNote
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeonIconOrb(
            icon = icon,
            contentDescription = null,
            size = 42.dp,
            iconSize = 21.dp,
            colors = listOf(accent, WavePink)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = WaveTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = WaveTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

