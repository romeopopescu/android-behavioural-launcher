package com.example.abl.utils

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import com.example.abl.data.database.entity.AppUsageData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object UsageStatsHelper {
    private const val TAG = "UsageStatsHelper"
    private const val DEFAULT_USER_ID = 1

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

    fun getAppUsageData(context: Context): List<AppUsageData> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val startTime = calendar.timeInMillis

        return try {
            val usageStatsMap =
                usageStatsManager.queryAndAggregateUsageStats(startTime, endTime) ?: emptyMap()

            usageStatsMap.values.mapNotNull { usageStats ->
                if (usageStats.totalTimeInForeground > 0) {
                    val dateFormat = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    )
                    val lastUsed = dateFormat.format(usageStats.lastTimeStamp)
                    val firstUsed = dateFormat.format(usageStats.firstTimeStamp)
                    val whenItWasUsed = Calendar.getInstance().apply {
                        timeInMillis = usageStats.lastTimeUsed
                    }.get(Calendar.HOUR_OF_DAY)

                    AppUsageData(
                        appId = 1,//TODO,
                        lastTimeUsed = lastUsed,
                        firstTimeUsed = firstUsed,
                        totalTimeInHours = formatDuration(100),
                        totalTimeInMinutes = formatDuration(101),
                        userId = 1,
                        id = 2
                    )
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            emptyList()
        }
    }
    private fun formatDuration(millis: Long): Long {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return hours
    }
}

data class AppUsageDataTest(
    val packageName: String,
    val totalTimeInForegroundHours: Long,
    val totalTimeInForegroundMinutes: Long,
    val lastTimeUsedFormatted: String,
    val firstTimeUsedFormatted: String
)