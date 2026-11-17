package com.radmon.radmonradio.ui.screens.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.radmon.radmonradio.RadioUiState
import com.radmon.radmonradio.RadioViewModel

/** Volume slider, sleep timer / play / favorite row, and the utility icon row beneath it. */
@Composable
internal fun PlayerControlsSection(
    mediaController: MediaController?,
    viewModel: RadioViewModel,
    uiState: RadioUiState,
    onLiked: () -> Unit
) {
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
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VolumeControl(mediaController = mediaController)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SleepTimerButton(mediaController = mediaController)
                PlaybackControl(mediaController = mediaController)
                FavoriteToggle(viewModel = viewModel, onLiked = onLiked)
            }
            Spacer(modifier = Modifier.height(20.dp))
            UtilityRow(onRecentlyPlayedClick = { viewModel.loadHistory() })
        }
    }
}
