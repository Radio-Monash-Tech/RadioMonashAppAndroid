package com.radmon.radmonradio.ui.screens.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radmon.radmonradio.R

/** Captions / cast / recently-played row below the main transport controls. */
@Composable
internal fun UtilityRow(onRecentlyPlayedClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Live captions aren't implemented yet; shown to match the layout, disabled for now.
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Subtitles,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
        // Casting to another output isn't implemented yet; shown to match the layout, disabled for now.
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(onClick = onRecentlyPlayedClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = stringResource(id = R.string.cd_recently_played),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
