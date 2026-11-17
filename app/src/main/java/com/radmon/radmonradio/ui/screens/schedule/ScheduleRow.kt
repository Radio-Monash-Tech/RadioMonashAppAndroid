package com.radmon.radmonradio.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radmon.radmonradio.data.model.ScheduledShow
import com.radmon.radmonradio.util.MelbourneTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRow(
    show: ScheduledShow,
    isOnAir: Boolean,
    isFollowed: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onToggleFollow()
            }
            // never actually dismiss the row — this swipe toggles, it doesn't remove it
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            val tint = if (isFollowed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tint.copy(alpha = 0.15f))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Filled.NotificationsOff else Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = tint
                )
            }
        }
    ) {
        val cs = MaterialTheme.colorScheme
        val container = if (isOnAir) cs.primaryContainer else cs.surfaceContainer
        val content = if (isOnAir) cs.onPrimaryContainer else cs.onSurface
        val muted = if (isOnAir) cs.onPrimaryContainer else cs.onSurfaceVariant

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = container,
            contentColor = content,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = buildString {
                        append(show.series.title)
                        show.series.hostLine?.let { append(", $it") }
                        append(", ${show.timeRange}.")
                        if (isOnAir) append(" On air now.")
                        if (isFollowed) append(" Following.")
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time
                Column(Modifier.width(48.dp)) {
                    Text(
                        text = MelbourneTime.clock(show.start),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = MelbourneTime.clock(show.end),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = muted
                    )
                }

                // Details
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = show.series.title,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isOnAir) {
                            OnAirBadge()
                        }
                    }
                    show.series.hostLine?.let { hosts ->
                        Text(
                            text = hosts,
                            color = muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isFollowed) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Following",
                        tint = cs.primary
                    )
                }
            }
        }
    }
}
