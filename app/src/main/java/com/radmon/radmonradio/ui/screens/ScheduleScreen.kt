package com.radmon.radmonradio.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable

@Composable
fun ScheduleScreen() {
    PlaceholderScreen(
        title = "Schedule",
        subtitle = "This week's shows will appear here.",
        icon = Icons.Filled.CalendarMonth
    )
}
