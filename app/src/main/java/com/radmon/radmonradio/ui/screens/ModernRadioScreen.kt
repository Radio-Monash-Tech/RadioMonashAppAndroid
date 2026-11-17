package com.radmon.radmonradio.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.radmon.radmonradio.HistoryState
import com.radmon.radmonradio.LoadingState
import com.radmon.radmonradio.RadioUiState
import com.radmon.radmonradio.RadioViewModel
import com.radmon.radmonradio.SongFinderState
import com.radmon.radmonradio.ui.components.PlatformLinksBottomSheet
import com.radmon.radmonradio.ui.components.RecentlyPlayedSheet
import com.radmon.radmonradio.ui.screens.live.ArtworkBackgroundLayer
import com.radmon.radmonradio.ui.screens.live.ArtworkDisplayWithBuffering
import com.radmon.radmonradio.ui.screens.live.HeartBurstOverlay
import com.radmon.radmonradio.ui.screens.live.MetadataDisplay
import com.radmon.radmonradio.ui.screens.live.ModernTopBar
import com.radmon.radmonradio.ui.screens.live.PlayerControlsSection
import com.radmon.radmonradio.ui.screens.live.ScreenHeart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The Live tab: station artwork, title/artist, and playback controls.
 * The individual pieces live under ui/screens/live/ - this just lays them out.
 */
@Composable
fun ModernRadioScreen(
    uiState: RadioUiState,
    mediaController: MediaController?,
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
            ModernTopBar(connectivity = uiState.connectivity)
            // Scrollable so nothing (artist name, controls) ever gets silently clipped
            // behind a sibling when content is taller than the available screen space.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                Spacer(modifier = Modifier.height(4.dp))
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
        HeartBurstOverlay(
            hearts = hearts,
            density = density,
            onSizeMeasured = { w, h ->
                widthPx = w
                heightPx = h
            }
        )
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
        val historyState by viewModel.historyState.collectAsStateWithLifecycle()
        if (historyState != HistoryState.Idle) {
            RecentlyPlayedSheet(
                uiState = uiState,
                historyState = historyState,
                onDismiss = { viewModel.dismissHistory() }
            )
        }
    }
}
