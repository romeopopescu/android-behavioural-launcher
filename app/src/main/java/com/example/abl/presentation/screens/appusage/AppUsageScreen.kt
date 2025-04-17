package com.example.abl.presentation.screens.appusage

import android.R
import android.R.attr.onClick
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppUsageScreen(
    onNavigateToMain: () -> Unit,
    onNavigateBack: () -> Unit
) {
//    Scaffold(
//        topBar = CenterAlignedTopAppBar(
//            onClick = onNavigateBack
//        )
//    ) {
//
//    }
    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text("App usage screen")
    }
}