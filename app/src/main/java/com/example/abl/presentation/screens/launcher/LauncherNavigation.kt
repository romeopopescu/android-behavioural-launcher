package com.example.abl.presentation.screens.launcher

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object LauncherDestination {
    const val route = "launcher_screen"
}

fun NavGraphBuilder.launcherScreen(
    onNavigateToUsageStats: () -> Unit,
    onNavigateToStatsPage: () -> Unit
) {
    composable(LauncherDestination.route) {
        LauncherScreen(
            onNavigateToUsageData = onNavigateToUsageStats,
            onNavigateToStatsPage = onNavigateToStatsPage
        )
    }
} 