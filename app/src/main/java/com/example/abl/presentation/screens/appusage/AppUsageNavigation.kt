package com.example.abl.presentation.screens.appusage

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object DataUsageDestination {
    const val route = "data_usage_screen"
}

@RequiresApi(Build.VERSION_CODES.P)
fun NavGraphBuilder.appUsageScreen(
    onNavigateToMain: () -> Unit,
//    onNavigateBack: () -> Unit
) {
    composable(route= DataUsageDestination.route) {
        AppUsageScreen(
            onNavigateToMain = onNavigateToMain
//            onNavigateBack = onNavigateBack
        )
    }
}

fun NavController.navigateToDataUsage() {
    this.navigate(DataUsageDestination.route)
}