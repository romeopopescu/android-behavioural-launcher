@file:OptIn(ExperimentalFoundationApi::class)

package com.example.abl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.abl.presentation.theme.AndroidLauncherForBehavouralProfileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidLauncherForBehavouralProfileTheme {
                AppRoot()
            }
        }
    }
}
