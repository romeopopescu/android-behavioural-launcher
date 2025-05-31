package com.example.abl.presentation.screens.statistics

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object UserStatisticsDestination

fun NavGraphBuilder.appUserStatisticsPage(
    onNavigateToMain: () -> Unit,
//    onNavigateBack: () -> Unit
) {
    composable<UserStatisticsDestination> {
        UserStatisticsPage(
            onNavigateToMain = onNavigateToMain
//            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToStatisticsPage() {
    navigate(UserStatisticsDestination)
}