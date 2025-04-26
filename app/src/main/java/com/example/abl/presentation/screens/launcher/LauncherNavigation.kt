package com.example.abl.presentation.screens.launcher

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object LauncherDestination

fun NavGraphBuilder.launcherScreen(
    onNavigateToUsageStats: () -> Unit
) {
    composable<LauncherDestination> {
        LauncherScreen(
            onNavigateToUsageData = onNavigateToUsageStats
        )
    }
} 