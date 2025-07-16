package com.example.abl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.abl.presentation.screens.appusage.appUsageScreen
import com.example.abl.presentation.screens.appusage.navigateToDataUsage
import com.example.abl.presentation.screens.launcher.LauncherDestination
import com.example.abl.presentation.screens.launcher.launcherScreen
import com.example.abl.presentation.screens.statistics.appUserStatisticsPage
import com.example.abl.presentation.screens.statistics.navigateToStatisticsPage

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LauncherDestination.route
    ) {
        launcherScreen(
            onNavigateToUsageStats = { navController.navigateToDataUsage() },
            onNavigateToStatsPage = { navController.navigateToStatisticsPage() }
        )
        appUsageScreen(
            onNavigateToMain = { navController.popBackStack() }
        )
        appUserStatisticsPage(
            onNavigateToMain = { navController.popBackStack() }
        )
    }
}

