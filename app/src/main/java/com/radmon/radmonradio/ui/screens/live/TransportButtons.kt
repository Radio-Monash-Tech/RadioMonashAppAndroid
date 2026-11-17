package com.radmon.radmonradio.ui.screens.live

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.radmon.radmonradio.R
import com.radmon.radmonradio.RadioViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The big round play/pause button. */
@Composable
internal fun PlaybackControl(mediaController: MediaController?) {
    var isPlaying by remember { mutableStateOf(mediaController?.isPlaying == true) }
    var lastClickTs by remember { mutableStateOf(0L) }
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    DisposableEffect(mediaController) {
        if (mediaController == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        mediaController.addListener(listener)
        isPlaying = mediaController.isPlaying
        onDispose {
            mediaController.removeListener(listener)
        }
    }
    FloatingActionButton(
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastClickTs < 300) return@FloatingActionButton
            lastClickTs = now
            if (isPlaying) mediaController?.pause() else mediaController?.play()
        },
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = Color.Black,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        AnimatedContent(
            targetState = isPlaying,
            label = "play_pause",
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f) togetherWith
                        fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f)
            }
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/** The heart button beside play. */
@Composable
internal fun FavoriteToggle(viewModel: RadioViewModel, onLiked: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val isFav by viewModel.isCurrentFavoriteFlow().collectAsStateWithLifecycle(initialValue = false)
    val icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder
    val tint = if (isFav) Color(0xFF8B0000) else Color.Black
    var lastClickTs by remember { mutableStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "favPressScale"
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val now = System.currentTimeMillis()
                            if (now - lastClickTs < 300) return@detectTapGestures
                            lastClickTs = now
                            if (!isFav) onLiked()
                            viewModel.toggleCurrentFavorite()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(id = R.string.cd_toggle_favorite), tint = tint)
        }
    }
}

/** The alarm button with a menu of sleep-timer durations and a live countdown. */
@Composable
internal fun SleepTimerButton(mediaController: MediaController?) {
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var endsAtMillis by remember { mutableStateOf<Long?>(null) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // ticks every second while a timer is running, to keep the countdown text live
    LaunchedEffect(endsAtMillis) {
        while (endsAtMillis != null) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingMs = endsAtMillis?.minus(nowMillis)
    val isActive = remainingMs != null && remainingMs > 0
    val tint = Color.Black

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(56.dp)
            ) {
                if (isActive) {
                    // a countdown while the timer runs, matching iOS's SleepTimerButton
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = stringResource(id = R.string.cd_sleep_timer),
                            tint = tint,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = formatRemaining(remainingMs),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = stringResource(id = R.string.cd_sleep_timer),
                        tint = tint
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (isActive) {
                DropdownMenuItem(
                    text = { Text("Turn Off Timer") },
                    onClick = {
                        menuExpanded = false
                        timerJob?.cancel()
                        timerJob = null
                        endsAtMillis = null
                    }
                )
            }
            listOf(5, 15, 30, 45, 60).forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(if (minutes == 60) "1 hour" else "$minutes minutes") },
                    onClick = {
                        menuExpanded = false
                        timerJob?.cancel()
                        nowMillis = System.currentTimeMillis()
                        endsAtMillis = nowMillis + minutes * 60_000L
                        timerJob = scope.launch {
                            delay(minutes * 60_000L)
                            mediaController?.pause()
                            endsAtMillis = null
                        }
                    }
                )
            }
        }
    }
}

// "4:59" style countdown, mirroring iOS's Text(timerInterval:countsDown:)
private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
