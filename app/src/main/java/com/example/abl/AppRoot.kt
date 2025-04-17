package com.example.abl

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.abl.presentation.screens.appusage.appUsageScreen
import com.example.abl.presentation.screens.appusage.navigateToDataUsage
import com.example.abl.presentation.screens.launcher.LauncherDestination
import com.example.abl.presentation.screens.launcher.launcherScreen

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LauncherDestination
    ) {
        launcherScreen(
            onNavigateToUsageStats = { navController.navigateToDataUsage() }
        )
        appUsageScreen(
            onNavigateToMain = { navController.popBackStack() }
        )
    }
}

