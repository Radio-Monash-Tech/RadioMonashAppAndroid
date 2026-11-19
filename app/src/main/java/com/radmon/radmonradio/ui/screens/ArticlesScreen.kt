/** Nav entry point for the Journalism tab. */
package com.radmon.radmonradio.ui.screens

import androidx.compose.runtime.Composable
import com.radmon.radmonradio.JournalismViewModel
import com.radmon.radmonradio.ui.screens.journalism.JournalismScreen

@Composable
fun ArticlesScreen(viewModel: JournalismViewModel, onOpenArticle: (String) -> Unit) {
    JournalismScreen(
        viewModel = viewModel,
        onOpenArticle = { onOpenArticle(it.id) }
    )
}
