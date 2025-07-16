package com.example.abl.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.abl.presentation.viewmodel.BehaviouralProfileViewModel
import com.example.abl.presentation.viewmodel.LauncherViewModel
import com.example.abl.presentation.viewmodel.SearchViewModel

@Composable
fun HomeScreen(
    onNavigateToUsageStats: () -> Unit,
    onNavigateToStatsPage: () -> Unit
) {
    val launcherViewModel: LauncherViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val behaviouralProfileViewModel: BehaviouralProfileViewModel = hiltViewModel()

    val recommendedApps by launcherViewModel.recommendedApps.collectAsState()
    val riskyApps by launcherViewModel.riskyApps.collectAsState()
    val anomalyUiState by behaviouralProfileViewModel.anomalyDetectionStatus.collectAsState()

    LaunchedEffect(Unit) {
        launcherViewModel.loadRecommendedApps()
        launcherViewModel.loadRiskyApps()
        behaviouralProfileViewModel.onAppLaunch()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onNavigateToUsageStats,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.4f))
            ) {
                Text("App Usage")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNavigateToStatsPage,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.4f))
            ) {
                Text("Stats")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val statusColor = when (anomalyUiState.riskLevel) {
                        "CRITICAL" -> Color.Red
                        "HIGH" -> Color(0xFFFFA500) // Orange
                        "MEDIUM" -> Color.Yellow
                        "LOW" -> Color.Green
                        else -> Color.White
                    }
                    Text(
                        text = "Anomaly Status: ${anomalyUiState.riskLevel}",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            if (recommendedApps.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Recommended for you",
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Top
                        ) {
                            recommendedApps.forEach { app ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { searchViewModel.launchApp(app.packageName) }
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.appName,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(56.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(app.appName, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            if (riskyApps.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Top 3 Risky Apps",
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Top
                        ) {
                            riskyApps.forEach { app ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { searchViewModel.launchApp(app.packageName) }
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.appName,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(56.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(app.appName, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}