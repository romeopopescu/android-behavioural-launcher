package com.example.abl.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.domain.repository.AppUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

class AppUsageRepositoryImpl @Inject constructor(
   @ApplicationContext private val context: Context
) : AppUsageRepository {
    private val usageStatsManager = context
        .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun hasUsageStatsPermission(): Boolean {
        val packageManager = context.packageManager
        val appName = context.packageName
        val applicationInfo = packageManager
            .getApplicationInfo(appName, PackageManager.GET_META_DATA)

        val time = System.currentTimeMillis()
        val usageStatsList = usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000, time)
        Log.d("UsageStatsInfo", "usageStatsList: $usageStatsList")
        return !usageStatsList.isNullOrEmpty()
    }

    override fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    override fun getUsageStats(): Map<String, UsageStats> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val startTime = calendar.timeInMillis

        val usageStatsMap = usageStatsManager
            .queryAndAggregateUsageStats(startTime, endTime)
        Log.d("UsageStatsInfo", "UsageStatsMap: $usageStatsMap")
        Log.d("UsageStatsInfo", "startTime: $startTime endTime: $endTime")

        return usageStatsMap ?: emptyMap()
    }

    override fun getAppUsageData(): List<Flow<AppUsageData>> {
        TODO("Not yet implemented")
    }


}