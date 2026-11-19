/** The Schedule tab: a day picker and that day's shows. */
package com.radmon.radmonradio.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radmon.radmonradio.ScheduleUiState
import com.radmon.radmonradio.SchedulePhase
import com.radmon.radmonradio.ScheduleViewModel
import com.radmon.radmonradio.util.MelbourneTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onShowSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    var showFollowedOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScheduleTopBar(
                isFollowedOnly = showFollowedOnly,
                onToggleFollowedOnly = { showFollowedOnly = !showFollowedOnly }
            )

            PullToRefreshBox(
                isRefreshing = uiState.phase == SchedulePhase.Loading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DayStrip(
                        days = viewModel.week,
                        selectedDay = uiState.selectedDay,
                        onDaySelected = viewModel::selectDay,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    uiState.offlineCopyAt?.let { savedAt ->
                        OfflineNote(savedAt = savedAt, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }

                    ScheduleContent(
                        uiState = uiState,
                        now = now,
                        showFollowedOnly = showFollowedOnly,
                        onRetry = { viewModel.refresh() },
                        onShowSelected = onShowSelected,
                        onToggleFollow = viewModel::toggleFollow
                    )
                }
            }
        }
    }
}

/** Large title + subtitle, with a "Following" filter pill at the far right of the same row. */
@Composable
private fun ScheduleTopBar(isFollowedOnly: Boolean, onToggleFollowedOnly: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Schedule",
                    color = cs.onBackground,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Australia/Melbourne",
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            val pillContainer = if (isFollowedOnly) cs.primary else cs.secondaryContainer
            val pillContent = if (isFollowedOnly) cs.onPrimary else cs.onSecondaryContainer
            Row(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillContainer)
                    .clickable(onClickLabel = "Show followed shows", onClick = onToggleFollowedOnly)
                    .padding(start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = pillContent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Following",
                    color = pillContent,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ScheduleContent(
    uiState: ScheduleUiState,
    now: Instant,
    showFollowedOnly: Boolean,
    onRetry: () -> Unit,
    onShowSelected: (String) -> Unit,
    onToggleFollow: (String) -> Unit
) {
    val shows = uiState.shows
        .filter { MelbourneTime.isSameDay(it.start, uiState.selectedDay) }
        .filter { !showFollowedOnly || uiState.followedIds.contains(it.series.id) }

    if (shows.isEmpty()) {
        when (val phase = uiState.phase) {
            is SchedulePhase.Idle, is SchedulePhase.Loading ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(6) { SkeletonRow() }
                }
            is SchedulePhase.Failed ->
                EmptyState(
                    icon = Icons.Filled.WifiOff,
                    title = "Schedule unavailable",
                    description = phase.message,
                    actionLabel = "Try again",
                    onAction = onRetry
                )
            is SchedulePhase.Loaded ->
                EmptyState(
                    icon = Icons.Filled.MusicNote,
                    title = if (showFollowedOnly) "No followed shows today" else "Nothing scheduled",
                    description = if (showFollowedOnly)
                        "None of the shows you follow are on today. Try another day."
                    else
                        "No shows are on this day. The Radio Monash playlist keeps playing on the Live tab."
                )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shows, key = { it.id }) { show ->
                val isOnAir = show.isOnAir(now)
                val isFollowed = uiState.followedIds.contains(show.series.id)
                ScheduleRow(
                    show = show,
                    isOnAir = isOnAir,
                    isFollowed = isFollowed,
                    onClick = { onShowSelected(show.series.id) },
                    onToggleFollow = { onToggleFollow(show.series.id) }
                )
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(cs.surfaceContainerHighest, RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Box(
                modifier = Modifier
                    .size(width = 180.dp, height = 16.dp)
                    .background(cs.surfaceContainerHighest, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 12.dp)
                    .background(cs.surfaceContainerHighest, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun OfflineNote(savedAt: Instant, modifier: Modifier = Modifier) {
    val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Showing a saved copy from ${formatter.format(savedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
