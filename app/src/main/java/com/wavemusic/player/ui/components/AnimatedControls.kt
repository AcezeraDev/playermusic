package com.wavemusic.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    shape: Shape,
    color: Color,
    pressedScale: Float = 0.97f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
        label = "animated-card-scale"
    )
    val entryAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "animated-card-entry-alpha"
    )
    val entryOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 18f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
        label = "animated-card-entry-offset"
    )
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = entryAlpha
                translationY = entryOffset
                shadowElevation = if (pressed && enabled) 18f else 8f
            }
            .clip(shape)
            .then(clickableModifier),
        color = color,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = if (pressed) 3.dp else 9.dp
    ) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Brush? = null,
    pressedScale: Float = 0.88f,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 760f),
        label = "animated-icon-button-scale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (pressed && enabled) -4f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 560f),
        label = "animated-icon-button-rotation"
    )
    val decoratedModifier = if (background != null) {
        Modifier.background(background)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                shadowElevation = if (pressed && enabled) 18f else 9f
            }
            .clip(CircleShape)
            .then(decoratedModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

