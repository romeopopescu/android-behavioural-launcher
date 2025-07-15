@file:OptIn(ExperimentalFoundationApi::class)

package com.example.abl

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.font.FontVariation.Setting
import androidx.compose.ui.text.font.FontVariation.Settings
import com.example.abl.data.collector.UsageAccumulatorService
import com.example.abl.presentation.theme.AndroidLauncherForBehaviouralProfileTheme
import com.example.abl.presentation.viewmodel.BehaviouralProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: BehaviouralProfileViewModel by viewModels()
    private var isServiceStarted = false

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The UI can be set up immediately
        setContent {
            AndroidLauncherForBehaviouralProfileTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // onResume is called when the user returns to the app,
        // making it the perfect place to check for the permission again.
        checkPermissionsAndStartProcess()
    }

    private fun checkPermissionsAndStartProcess() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            // Permission is granted, start the feature's logic flow.
            viewModel.onAppLaunch()
        } else {
            // Permission is NOT granted, send the user to settings.
            requestBatteryOptimizationExemption()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }
        // This will only be called if the permission is missing.
        startActivity(intent)
    }

    private fun startUsageAccumulatorService() {
        val serviceIntent = Intent(this, UsageAccumulatorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
