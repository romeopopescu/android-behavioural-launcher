package com.example.abl.presentation.screens.appusage

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object DataUsageDestination

fun NavGraphBuilder.appUsageScreen(
    onNavigateToMain: () -> Unit,
//    onNavigateBack: () -> Unit
) {
    composable<DataUsageDestination> {
        AppUsageScreen(
            onNavigateToMain = onNavigateToMain
//            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToDataUsage() {
    navigate(DataUsageDestination)
}