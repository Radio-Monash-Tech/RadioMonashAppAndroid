package com.radmon.radmonradio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage

import com.radmon.radmonradio.ConnectionQuality
import com.radmon.radmonradio.LoadingState
import com.radmon.radmonradio.R
import com.radmon.radmonradio.RadioUiState
import com.radmon.radmonradio.RadioViewModel

import com.radmon.radmonradio.SongFinderState
import com.radmon.radmonradio.ui.components.PlatformLinksBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ModernRadioScreen(
    uiState: RadioUiState,
    mediaController: MediaController?,
    onNavigateToStatistics: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    viewModel: RadioViewModel
) {
    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(mediaController) {
        val controller = mediaController
        if (controller == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                }
            }
            controller.addListener(listener)
            onDispose { controller.removeListener(listener) }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ArtworkBackgroundLayer(artworkUrl = uiState.artworkUrl)
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val hearts = remember { mutableStateListOf<ScreenHeart>() }
        var widthPx by remember { mutableFloatStateOf(0f) }
        var heightPx by remember { mutableFloatStateOf(0f) }
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
            ModernTopBar(
                connectionQuality = uiState.connectionQuality,
                onStatisticsClick = onNavigateToStatistics,
                onFavoritesClick = onNavigateToFavorites,
                favoritesCount = favorites.size
            )
            Box(modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArtworkDisplayWithBuffering(
                        artworkUrl = uiState.artworkUrl,
                        isLoading = uiState.loadingState is LoadingState.Loading,
                        isBuffering = isBuffering,
                        onDoubleTap = { viewModel.findCurrentSongPlatforms() }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    MetadataDisplay(
                        uiState = uiState,
                        onRetry = { viewModel.retry() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val triggerBurst = remember(widthPx, heightPx) {
                        {
                            val safeW = if (widthPx > 0) widthPx else 1080f
                            val safeH = if (heightPx > 0) heightPx else 1920f
                            val count = 18
                            repeat(count) { i ->
                                val x = Random.nextFloat() * (safeW - 48f) + 24f
                                val startY = safeH * (0.65f + Random.nextFloat() * 0.2f)
                                val endY = -safeH * 0.1f
                                val size = Random.nextInt(16, 28).toFloat()
                                val rot = Random.nextFloat() * 30f - 15f
                                val anim = Animatable(0f)
                                val h = ScreenHeart(System.nanoTime(), x, startY, endY, size, rot, anim)
                                hearts.add(h)
                                scope.launch {
                                    if (i > 0) delay(i * 20L)
                                    anim.animateTo(1f, tween(durationMillis = Random.nextInt(900, 1400)))
                                    hearts.remove(h)
                                }
                            }
                        }
                    }
                    PlayerControlsSection(
                        mediaController = mediaController,
                        viewModel = viewModel,
                        uiState = uiState,
                        onLiked = { triggerBurst() }
                    )
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            LaunchedEffect(maxWidth, maxHeight) {
                widthPx = with(density) { maxWidth.toPx() }
                heightPx = with(density) { maxHeight.toPx() }
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
        val songFinderState by viewModel.songFinderState.collectAsStateWithLifecycle()
        when (val state = songFinderState) {
            is SongFinderState.Loading,
            is SongFinderState.Success,
            is SongFinderState.Error -> {
                PlatformLinksBottomSheet(
                    songData = (state as? SongFinderState.Success)?.songData,
                    isLoading = state is SongFinderState.Loading,
                    error = (state as? SongFinderState.Error)?.message,
                    onDismiss = { viewModel.dismissSongFinder() }
                )
            }
            SongFinderState.Idle -> {  }
        }
    }
}
@Composable
private fun ArtworkBackgroundLayer(artworkUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
                .scale(1.2f),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun ModernTopBar(
    connectionQuality: ConnectionQuality,
    onStatisticsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    favoritesCount: Int
) {
    Row(
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_title_caps),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = Color.White
            )
            ConnectionQualityBadge(quality = connectionQuality)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            var favPressed by remember { mutableStateOf(false) }
            val favScale by animateFloatAsState(
                targetValue = if (favPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "favScale"
            )
            Box(modifier = Modifier.size(40.dp)) {
                IconButton(
                    onClick = onFavoritesClick,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = favScale
                            scaleY = favScale
                        }
                        .background(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    favPressed = true
                                    tryAwaitRelease()
                                    favPressed = false
                                }
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(id = R.string.cd_favorites),
                        tint = Color.White
                    )
                }
                if (favoritesCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF5350),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    ) {
                        Text(
                            text = favoritesCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
            var statsPressed by remember { mutableStateOf(false) }
            val statsScale by animateFloatAsState(
                targetValue = if (statsPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "statsScale"
            )
            IconButton(
                onClick = onStatisticsClick,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = statsScale
                        scaleY = statsScale
                    }
                    .background(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                statsPressed = true
                                tryAwaitRelease()
                                statsPressed = false
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = stringResource(id = R.string.cd_statistics),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ConnectionQualityBadge(quality: ConnectionQuality) {
    val (targetColor, text) = when (quality) {
        ConnectionQuality.EXCELLENT -> Color(0xFF1DB954) to stringResource(id = R.string.status_live)
        ConnectionQuality.GOOD -> Color(0xFF1DB954) to stringResource(id = R.string.status_live)
        ConnectionQuality.FAIR -> Color(0xFFFFA726) to stringResource(id = R.string.status_buffering)
        ConnectionQuality.POOR -> Color(0xFFEF5350) to stringResource(id = R.string.status_poor)
        ConnectionQuality.DISCONNECTED -> Color(0xFFEF5350) to stringResource(id = R.string.status_offline)
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "badgeColor"
    )
    val isLive = quality == ConnectionQuality.EXCELLENT || quality == ConnectionQuality.GOOD
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
            ConnectionQuality.EXCELLENT -> Color(0xFF1DB954) to stringResource(id = R.string.status_live)
            ConnectionQuality.GOOD -> Color(0xFF1DB954) to stringResource(id = R.string.status_live)
            ConnectionQuality.FAIR -> Color(0xFFFFA726) to stringResource(id = R.string.status_buffering)
            ConnectionQuality.POOR -> Color(0xFFEF5350) to stringResource(id = R.string.status_poor)
            ConnectionQuality.DISCONNECTED -> Color(0xFFEF5350) to stringResource(id = R.string.status_offline)
        }
        val badgeColor by animateColorAsState(
            targetValue = animatedColor,
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "innerBadgeColor"
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeColor.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
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

@Composable
private fun ArtworkDisplayWithBuffering(
    artworkUrl: String,
    isLoading: Boolean,
    isBuffering: Boolean,
    onDoubleTap: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        Card(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = Color.White,
                strokeWidth = 4.dp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBuffering,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isBuffering,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = Color.White,
                    strokeWidth = 6.dp
                )
            }
        }
    }
}

@Composable
private fun MetadataDisplay(
    uiState: RadioUiState,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val loadingState = uiState.loadingState) {
            is LoadingState.Error -> {
                ErrorStateDisplay(
                    message = loadingState.message,
                    canRetry = loadingState.canRetry,
                    onRetry = onRetry
                )
            }
            else -> {
                AnimatedContent(
                    targetState = uiState.songTitle,
                    label = "title",
                    transitionSpec = {
                        slideInVertically { it / 2 } + fadeIn() togetherWith
                                slideOutVertically { -it / 2 } + fadeOut()
                    }
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedContent(
                    targetState = uiState.artistName,
                    label = "artist",
                    transitionSpec = {
                        slideInVertically { it / 2 } + fadeIn() togetherWith
                                slideOutVertically { -it / 2 } + fadeOut()
                    }
                ) { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorStateDisplay(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFEF5350),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        if (canRetry) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class ScreenHeart(
    val id: Long,
    val xPx: Float,
    val startYPx: Float,
    val endYPx: Float,
    val sizeDp: Float,
    val rotation: Float,
    val anim: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>
)

@Composable
private fun PlayerControlsSection(mediaController: MediaController?, viewModel: RadioViewModel, uiState: RadioUiState, onLiked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            VolumeControl(mediaController = mediaController)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaybackControl(mediaController = mediaController)
                FavoriteToggle(viewModel = viewModel, onLiked = onLiked)
            }
        }
    }
}
@Composable
private fun FavoriteToggle(viewModel: RadioViewModel, onLiked: () -> Unit) {
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

@Composable
private fun VolumeControl(mediaController: MediaController?) {
    var volume by remember { mutableFloatStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var lastVolume by remember { mutableFloatStateOf(1f) }
    DisposableEffect(mediaController) {
        if (mediaController == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onVolumeChanged(newVolume: Float) {
                volume = newVolume
                isMuted = newVolume == 0f
            }
        }
        mediaController.addListener(listener)
        volume = mediaController.volume
        isMuted = volume == 0f
        onDispose {
            mediaController.removeListener(listener)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = {
                if (isMuted) {
                    mediaController?.volume = lastVolume
                } else {
                    lastVolume = volume
                    mediaController?.volume = 0f
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = if (isMuted) stringResource(id = R.string.cd_unmute) else stringResource(id = R.string.cd_mute),
                tint = Color.White
            )
        }
        Slider(
            value = volume,
            onValueChange = { mediaController?.volume = it },
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun PlaybackControl(mediaController: MediaController?) {
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