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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.font.FontVariation.Setting
import androidx.compose.ui.text.font.FontVariation.Settings
import com.example.abl.data.collector.UsageAccumulatorService
import com.example.abl.presentation.theme.AndroidLauncherForBehaviouralProfileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isServiceStarted = false

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
        checkPermissionsAndStartService()
    }

    private fun checkPermissionsAndStartService() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

        if (isIgnoringOptimizations) {
            // Permission is granted, start the service if it hasn't been started already.
            if (!isServiceStarted) {
                startUsageAccumulatorService()
                isServiceStarted = true
            }
        } else {
            // Permission is NOT granted, send the user to the settings screen.
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
