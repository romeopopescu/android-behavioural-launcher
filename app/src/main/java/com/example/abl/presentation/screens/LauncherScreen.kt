package com.example.abl.presentation.screens

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

@Composable
fun LauncherScreen(modifier: Modifier = Modifier) {
    var showAppDrawer by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

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
        HomeScreen()
        if (showAppDrawer) {
            AppDrawer(
                lazyListState = lazyListState,
                onDismiss = { showAppDrawer = false }
            )
        }
    }
}