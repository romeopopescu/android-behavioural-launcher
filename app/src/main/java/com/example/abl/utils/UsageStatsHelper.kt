package com.example.abl.utils

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import com.example.abl.data.database.entity.AppUsageData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object UsageStatsHelper {
    private const val TAG = "UsageStatsHelper"

    fun hasUsageStatsPermission(context: Context): Boolean {
        val packageManager = context.packageManager
        val appName = context.packageName
        val applicationInfo = packageManager
            .getApplicationInfo(appName, PackageManager.GET_META_DATA)
        val usageStatsManager =
            context.getSystemService(Context
                .USAGE_STATS_SERVICE) as UsageStatsManager

        val time = System.currentTimeMillis()
        val usageStatsList = usageStatsManager
            .queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000,
                time
            )
        Log.d(TAG, "usageStatsList: $usageStatsList")
        return !usageStatsList.isNullOrEmpty()
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    fun getUsageStats(context: Context): Map<String, UsageStats> {
        val usageStatsManager = context.getSystemService(Context
            .USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val startTime = calendar.timeInMillis

        val usageStatsMap = usageStatsManager
            .queryAndAggregateUsageStats(startTime, endTime)

        Log.d(TAG, "UsageStatsMap: $usageStatsMap")
        Log.d(TAG, "startTime: $startTime endTime: $endTime")

        return usageStatsMap ?: emptyMap()
    }

    fun getAppUsageData(context: Context): List<AppUsageDataTest> {
        val appUsageDataList = mutableListOf<AppUsageDataTest>()
        val usageStatsMap = getUsageStats(context)
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for ((packageName, usageStats) in usageStatsMap) {
            val totalTimeInForegroundMillis = usageStats
                .totalTimeInForeground
            val lastTimeUsed = usageStats.lastTimeUsed
            val firstTimeStamp = usageStats.firstTimeStamp

            val totalTimeInForegroundHours =
                totalTimeInForegroundMillis / (1000 * 60 * 60)
            val totalTimeInForegroundMinutes =
                totalTimeInForegroundMillis / (1000 * 60)

            val lastTimeUsedFormatted = dateFormat.format(Date(lastTimeUsed))
            val firstTimeUsedFormatted = dateFormat.format(Date(firstTimeStamp))

            val appUsageData = AppUsageDataTest(
                packageName,
                totalTimeInForegroundHours,
                totalTimeInForegroundMinutes,
                lastTimeUsedFormatted,
                firstTimeUsedFormatted
            )
            appUsageDataList.add(appUsageData)
            Log.d(TAG, "AppUsageData: $appUsageData")
        }
        return appUsageDataList
    }
}

//temporary data class
data class AppUsageDataTest(
    val packageName: String,
    val totalTimeInForegroundHours: Long,
    val totalTimeInForegroundMinutes: Long,
    val lastTimeUsedFormatted: String,
    val firstTimeUsedFormatted: String
)