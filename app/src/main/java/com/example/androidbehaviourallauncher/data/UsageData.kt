package com.example.androidbehaviourallauncher.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.text.font.FontVariation.Settings


fun hasUsageStatsPermission(context: Context): Boolean {
    val packageManager = context.packageManager
    val appName = context.packageName
    TODO("continue permission verification")
}

fun openUsageSettings(context: Context) {
//    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//
}