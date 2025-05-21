package com.example.abl.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.abl.presentation.viewmodel.LauncherViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.abl.presentation.viewmodel.SearchViewModel
import com.example.abl.presentation.viewmodel.SearchViewModel_HiltModules

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToUsageStats: () -> Unit
) {
    val launcherViewModel: LauncherViewModel = hiltViewModel()
    val recommendedApps = launcherViewModel.recommendedApps.collectAsState().value
    val searchViewModel: SearchViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        launcherViewModel.loadRecommendedApps()
    }

    Column(
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top,
        modifier = Modifier.padding(16.dp)
    ) {
        Button(
            onClick = onNavigateToUsageStats
        ) {
            Text("App Usage")
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (recommendedApps.isNotEmpty()) {
            Text("Recommended for you", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                recommendedApps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { searchViewModel.launchApp(app.packageName) }
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.appName,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(app.appName, color = Color.Black)
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
