package com.radmon.radmonradio.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable

@Composable
fun SearchScreen() {
    PlaceholderScreen(
        title = "Search",
        subtitle = "Search shows, articles and recently played tracks.",
        icon = Icons.Filled.Search
    )
}
