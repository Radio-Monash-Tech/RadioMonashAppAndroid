package com.radmon.radmonradio.ui.screens.live

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radmon.radmonradio.Connectivity
import com.radmon.radmonradio.R

/** The LIVE badge + station name bar pinned to the top of the Live screen. */
@Composable
internal fun ModernTopBar(connectivity: Connectivity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            ConnectionQualityBadge(quality = connectivity)
        }
        Text(
            text = stringResource(id = R.string.radio_name),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}

/** LIVE / BUFFERING / OFFLINE pill, with a pulsing dot while live. */
@Composable
private fun ConnectionQualityBadge(quality: Connectivity) {
    val isLive = quality == Connectivity.GOOD
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isLive) 0.5f else 1f,
        targetValue = if (isLive) 1f else 1f,
        animationSpec = if (isLive) {
            infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            infiniteRepeatable(animation = tween(0), repeatMode = RepeatMode.Restart)
        },
        label = "pulseAlpha"
    )
    AnimatedContent(
        targetState = quality,
        transitionSpec = {
            fadeIn(tween(300)) + scaleIn(initialScale = 0.9f) togetherWith
                    fadeOut(tween(300)) + scaleOut(targetScale = 0.9f)
        },
        label = "badgeContent"
    ) { currentQuality ->
        val (animatedColor, animatedText) = when (currentQuality) {
            Connectivity.GOOD -> Color(0xFF1DB954) to stringResource(id = R.string.status_live)
            Connectivity.WEAK -> Color(0xFFFFA726) to stringResource(id = R.string.status_buffering)
            Connectivity.OFFLINE -> Color(0xFFEF5350) to stringResource(id = R.string.status_offline)
        }
        val badgeColor by animateColorAsState(
            targetValue = animatedColor,
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "innerBadgeColor"
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, badgeColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = pulseAlpha))
                )
                Text(
                    text = animatedText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = badgeColor
                )
            }
        }
    }
}
