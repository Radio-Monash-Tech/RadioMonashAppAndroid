package com.radmon.radmonradio.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.runtime.Composable

@Composable
fun LibraryScreen() {
    PlaceholderScreen(
        title = "Library",
        subtitle = "Liked songs, listening stats, saved articles and settings will live here.",
        icon = Icons.AutoMirrored.Filled.LibraryBooks
    )
}
