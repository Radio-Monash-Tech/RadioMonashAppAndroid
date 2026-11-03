package com.radmon.radmonradio.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.runtime.Composable

@Composable
fun ArticlesScreen(onNavigateBack: () -> Unit) {
    PlaceholderScreen(
        title = "Journalism",
        subtitle = "Articles from Radio Monash will appear here.",
        icon = Icons.Filled.Article
    )
}
