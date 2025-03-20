package com.example.abl.utils

import android.content.Context
import android.content.pm.PackageManager

object UsageStatsHelper {
    private const val TAG = "UsageStatsHelper"

    fun hasUsageStatsPermission(context: Context): Boolean {
        val packageManager = context.packageManager
        val appName = context.packageName
        val applicationInfo = packageManager
            .getApplicationInfo(appName, PackageManager.GET_META_DATA)
        val usageStatsManager = context
            .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsHelper

        val time = System.currentTimeMillis()
//        val usageStatsList = usageStatsManager
//            .queryUsage
//
    }
}