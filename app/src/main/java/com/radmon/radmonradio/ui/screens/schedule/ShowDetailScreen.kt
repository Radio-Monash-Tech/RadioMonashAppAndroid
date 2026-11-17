/** One show: hosts, follow, listen live, about, and this week's times. */
package com.radmon.radmonradio.ui.screens.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.radmon.radmonradio.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    seriesId: String,
    scheduleViewModel: ScheduleViewModel,
    mediaController: MediaController?,
    onNavigateBack: () -> Unit
) {
    val uiState by scheduleViewModel.uiState.collectAsStateWithLifecycle()
    val now by scheduleViewModel.now.collectAsStateWithLifecycle()

    val occurrences = scheduleViewModel.occurrencesOf(seriesId)
    val series = occurrences.firstOrNull()?.series
    val isFollowed = uiState.followedIds.contains(seriesId)
    val isOnAirNow = occurrences.any { it.isOnAir(now) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (series == null) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(text = series.title, style = MaterialTheme.typography.headlineSmall)
                series.hostLine?.let { hosts ->
                    Text(
                        text = "with $hosts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { scheduleViewModel.toggleFollow(seriesId) }) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFollowed) "Following" else "Follow")
                    }
                    if (isOnAirNow) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { mediaController?.play() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Listen Live")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isFollowed) "You'll get a reminder 10 minutes before each episode."
                    else "Follow to get a reminder 10 minutes before each episode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            series.summary?.let { summary ->
                item {
                    Text(text = "About", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = summary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Text(text = "This week", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (occurrences.isEmpty()) {
                item {
                    Text(
                        text = "Not on the schedule this week.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(occurrences, key = { it.id }) { show ->
                    val isPast = show.end <= now
                    val textColor = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = show.dayLabel, color = textColor)
                        if (show.isOnAir(now)) {
                            OnAirBadge()
                        } else {
                            Text(text = show.timeRange, color = textColor)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
