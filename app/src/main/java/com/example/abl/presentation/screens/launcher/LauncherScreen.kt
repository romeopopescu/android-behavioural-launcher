package com.example.abl.presentation.screens.launcher

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.abl.presentation.components.AppDrawer
import com.example.abl.presentation.screens.home.HomeScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.abl.presentation.viewmodel.SearchViewModel

@Composable
fun LauncherScreen(
    onNavigateToUsageData: () -> Unit,
    onNavigateToStatsPage: () -> Unit
) {
    var showAppDrawer by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val searchViewModel: SearchViewModel = hiltViewModel()

    fun closeDrawerAndResetSearch() {
        showAppDrawer = false
        searchViewModel.deleteSearch()
    }

    Box (
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (!showAppDrawer && dragAmount.y < -50) {
                        showAppDrawer = true
                    }
                }
            }
    ) {
        HomeScreen(onNavigateToUsageData, onNavigateToStatsPage)
        if (showAppDrawer) {
            AppDrawer(
                lazyListState = lazyListState,
                onDismiss = { closeDrawerAndResetSearch() },
                onAppOpen = { closeDrawerAndResetSearch() }
            )
        }
    }
}