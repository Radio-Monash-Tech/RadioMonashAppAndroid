package com.radmon.radmonradio.ui.screens.live

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

internal data class ScreenHeart(
    val id: Long,
    val xPx: Float,
    val startYPx: Float,
    val endYPx: Float,
    val sizeDp: Float,
    val rotation: Float,
    val anim: Animatable<Float, AnimationVector1D>
)

/** Floating, fading hearts drawn when a track is liked. */
@Composable
internal fun HeartBurstOverlay(
    hearts: List<ScreenHeart>,
    density: Density,
    onSizeMeasured: (widthPx: Float, heightPx: Float) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LaunchedEffect(maxWidth, maxHeight) {
            onSizeMeasured(
                with(density) { maxWidth.toPx() },
                with(density) { maxHeight.toPx() }
            )
        }
        hearts.forEach { h ->
            val p = h.anim.value
            val y = h.startYPx + (h.endYPx - h.startYPx) * p
            val alpha = (1f - p).coerceIn(0f, 1f)
            val scale = 0.7f + 0.5f * (1f - p)
            Box(
                modifier = Modifier
                    .offset { IntOffset(h.xPx.toInt(), y.toInt()) }
                    .graphicsLayer {
                        rotationZ = h.rotation
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(with(density) { h.sizeDp.dp })
                )
            }
        }
    }
}
