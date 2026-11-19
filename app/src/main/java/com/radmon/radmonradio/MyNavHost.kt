package com.radmon.radmonradio

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.radmon.radmonradio.ui.screens.ArticlesScreen
import com.radmon.radmonradio.ui.screens.FavoritesScreen
import com.radmon.radmonradio.ui.screens.LibraryScreen
import com.radmon.radmonradio.ui.screens.journalism.article.ArticleScreen
import com.radmon.radmonradio.ui.screens.schedule.ScheduleScreen
import com.radmon.radmonradio.ui.screens.schedule.ShowDetailScreen
import com.radmon.radmonradio.ui.screens.SearchScreen
import com.radmon.radmonradio.ui.screens.StatisticsScreen
import com.radmon.radmonradio.ui.screens.ModernRadioScreen

@Composable
//creating navhost
fun MyNavHost(
    innerPadding: PaddingValues,
    navController: NavHostController,
    uiState: RadioUiState,
    mediaController: MediaController?,
    radioViewModel: RadioViewModel,
    scheduleViewModel: ScheduleViewModel,
    journalismViewModel: JournalismViewModel
){
    NavHost(
        navController = navController,
        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        startDestination =

            "radio"
    ) {
        //composables for all the screens
        composable("radio") {
            ModernRadioScreen(
                uiState = uiState,
                mediaController = mediaController,
                viewModel = radioViewModel
            )
        }
        composable("statistics") {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("favorites") {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = radioViewModel
            )
        }
        composable("articles") {
            ArticlesScreen(
                viewModel = journalismViewModel,
                onOpenArticle = { articleId -> navController.navigate("article/$articleId") }
            )
        }
        composable(
            "article/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId").orEmpty()
            ArticleScreen(
                articleId = articleId,
                viewModel = journalismViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("schedule") {
            ScheduleScreen(
                viewModel = scheduleViewModel,
                onShowSelected = { seriesId -> navController.navigate("show/$seriesId") }
            )
        }
        composable(
            "show/{seriesId}",
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId").orEmpty()
            ShowDetailScreen(
                seriesId = seriesId,
                scheduleViewModel = scheduleViewModel,
                mediaController = mediaController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("library") {
            LibraryScreen()
        }
        composable("search") {
            SearchScreen()
        }
    }
}
