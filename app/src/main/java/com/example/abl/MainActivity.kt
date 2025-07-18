@file:OptIn(ExperimentalFoundationApi::class)

package com.example.abl

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
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

        setContent {
            AndroidLauncherForBehaviouralProfileTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStartProcess()
    }

    private fun checkPermissionsAndStartProcess() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.d("CHECKING", "is sending viewmodel")
            viewModel.onAppLaunch()
        } else {
            requestBatteryOptimizationExemption()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }
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
