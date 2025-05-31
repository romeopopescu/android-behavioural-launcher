package com.example.abl.presentation.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserStatisticsPage(
    onNavigateToMain: () -> Unit
) {
    // Dummy data for the chart
//    val barData = listOf(
//        BarData(Random.nextFloat() * 10, "Mon"),
//        BarData(Random.nextFloat() * 10, "Tue"),
//        BarData(Random.nextFloat() * 10, "Wed"),
//        BarData(Random.nextFloat() * 10, "Thu"),
//        BarData(Random.nextFloat() * 10, "Fri"),
//        BarData(Random.nextFloat() * 10, "Sat"),
//        BarData(Random.nextFloat() * 10, "Sun")
//    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("User Statistics") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Weekly App Usage",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
//
//            BarChart(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(250.dp),
//                barData = barData,
//                color = Color.Blue,
//                onBarClick = { barData ->
//                    // Handle bar click if needed
//                }
//            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Other Interesting Stats",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticCard("Most Used App", "Placeholder App")
                StatisticCard("Total Screen Time", "Xh Ym")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticCard("App Launches", "N times")
                StatisticCard("Unlock Frequency", "M times")
            }


            Button(onClick = onNavigateToMain) {
                Text("Back to Main")
            }
        }
    }
}

@Composable
fun StatisticCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(150.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 14.sp)
        }
    }
}