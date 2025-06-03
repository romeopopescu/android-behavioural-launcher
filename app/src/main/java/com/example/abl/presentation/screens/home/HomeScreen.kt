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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.abl.presentation.viewmodel.LauncherViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.abl.domain.usecases.anomaly.AnomalyDetectionResult
import com.example.abl.presentation.viewmodel.BehaviouralProfileViewModel
import com.example.abl.presentation.viewmodel.SearchViewModel
import com.example.abl.presentation.viewmodel.SearchViewModel_HiltModules

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToUsageStats: () -> Unit,
    onNavigateToStatsPage: () -> Unit
) {
    val launcherViewModel: LauncherViewModel = hiltViewModel()
    val recommendedApps = launcherViewModel.recommendedApps.collectAsState().value
    val searchViewModel: SearchViewModel = hiltViewModel()
    val riskyApps = launcherViewModel.riskyApps.collectAsState().value
    val behaviouralProfileViewModel: BehaviouralProfileViewModel = hiltViewModel()
    val anomalyStatus by behaviouralProfileViewModel.anomalyDetectionStatus.collectAsState()
    var showHighAlertDialog by remember { mutableStateOf(false) }
    var highAlertReasons by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        launcherViewModel.loadRecommendedApps()
        launcherViewModel.loadRiskyApps()
    }

    LaunchedEffect(anomalyStatus) {
        if (anomalyStatus is AnomalyDetectionResult.HighAlert) {
            highAlertReasons = (anomalyStatus as AnomalyDetectionResult.HighAlert).reasons
            showHighAlertDialog = true
        }
    }

    if (showHighAlertDialog) {
        AlertDialog(
            onDismissRequest = { showHighAlertDialog = false },
            title = { Text("High Security Alert!") },
            text = {
                Column {
                    Text("Unusual activity detected. Reasons:")
                    highAlertReasons.forEach { reason ->
                        Text("- $reason")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHighAlertDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.padding(16.dp)
    ) {
        Button(
            onClick = onNavigateToUsageStats
        ) {
            Text("App Usage")
        }
        Button(
            onClick = onNavigateToStatsPage
        ) {
            Text("Stats")
        }
        Spacer(modifier = Modifier.height(8.dp))

        val statusText = when (val status = anomalyStatus) {
            is AnomalyDetectionResult.Normal -> "Status: Normal"
            is AnomalyDetectionResult.Suspicious -> "Status: Suspicious (Score: ${status.deviationScore})Reasons: ${status.reasons.joinToString()}"
            is AnomalyDetectionResult.HighAlert -> "Status: High Alert! (Score: ${status.deviationScore})Reasons: ${status.reasons.joinToString()}"
        }
        val statusColor = when (anomalyStatus) {
            is AnomalyDetectionResult.HighAlert -> Color.Red
            is AnomalyDetectionResult.Suspicious -> Color.DarkGray // Or another color like Orange
            else -> Color.Gray
        }
        Text(text = statusText, color = statusColor)

        Spacer(modifier = Modifier.height(16.dp)) // Adjusted spacer
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
        if (riskyApps.isNotEmpty()) {
            Text("Top 3 Risky Apps", color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                riskyApps.forEach { app ->
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Risk: ${app.riskScore}", color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
