package com.vttexplorer.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vttexplorer.app.presentation.history.HistoryScreen
import com.vttexplorer.app.presentation.map.MapScreen
import com.vttexplorer.app.presentation.navigation.NavigationScreen
import com.vttexplorer.app.presentation.route_generator.RouteGeneratorScreen
import com.vttexplorer.app.presentation.settings.SettingsScreen

object Routes {
    const val MAP = "map"
    const val LOOP = "loop"
    const val NAVIGATION = "navigation"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun VTTExplorerNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAP) {
        composable(Routes.MAP) {
            MapScreen(
                onCreateLoop = { navController.navigate(Routes.LOOP) },
                onDestination = { /* TODO search */ },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onStartNavigation = { navController.navigate(Routes.NAVIGATION) }
            )
        }
        composable(Routes.LOOP) {
            RouteGeneratorScreen(
                onBack = { navController.popBackStack() },
                onStart = { navController.navigate(Routes.NAVIGATION) }
            )
        }
        composable(Routes.NAVIGATION) {
            NavigationScreen(onStop = { navController.popBackStack(Routes.MAP, false) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
