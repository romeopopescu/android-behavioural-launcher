package com.example.abl.presentation.screens.statistics

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object UserStatisticsDestination {
    const val route = "user_statistics_screen"
}

fun NavGraphBuilder.appUserStatisticsPage(
    onNavigateToMain: () -> Unit,
//    onNavigateBack: () -> Unit
) {
    composable(route=UserStatisticsDestination.route) {
        UserStatisticsPage(
            onNavigateToMain = onNavigateToMain
//            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToStatisticsPage() {
    this.navigate(UserStatisticsDestination.route)
}