package com.radmon.radmonradio

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.radmon.radmonradio.ui.screens.ArticlesScreen
import com.radmon.radmonradio.ui.screens.FavoritesScreen
import com.radmon.radmonradio.ui.screens.LibraryScreen
import com.radmon.radmonradio.ui.screens.ScheduleScreen
import com.radmon.radmonradio.ui.screens.SearchScreen
import com.radmon.radmonradio.ui.screens.StatisticsScreen
import com.radmon.radmonradio.ui.screens.ModernRadioScreen

@Composable
//creating navhost
fun MyNavHost(innerPadding: PaddingValues, navController: NavHostController, uiState: RadioUiState, mediaController: MediaController?, radioViewModel: RadioViewModel){
    NavHost(
        navController = navController,

        startDestination =

            "radio"
    ) {
        //composables for all the screens
        composable("radio") {
            ModernRadioScreen(
                uiState = uiState,
                mediaController = mediaController,
                onNavigateToStatistics = { navController.navigate("statistics") },
                onNavigateToFavorites = { navController.navigate("favorites") },
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
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("schedule") {
            ScheduleScreen()
        }
        composable("library") {
            LibraryScreen()
        }
        composable("search") {
            SearchScreen()
        }
    }
}
