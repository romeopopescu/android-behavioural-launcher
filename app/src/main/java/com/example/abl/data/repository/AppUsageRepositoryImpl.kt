package com.example.abl.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.data.database.dao.AppUsageDataDao
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.dao.UserProfileDao
import com.example.abl.data.database.entity.UserProfile
import com.example.abl.domain.repository.AppUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject

class AppUsageRepositoryImpl @Inject constructor(
   @ApplicationContext private val context: Context,
   private val appUsageDataDao: AppUsageDataDao,
   private val appInformationDao: AppInformationDao,
    private val userProfileDao: UserProfileDao,
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
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStatsMap = usageStatsManager
            .queryAndAggregateUsageStats(startTime, endTime)
        Log.d("UsageStatsInfo", "UsageStatsMap: $usageStatsMap")
        Log.d("UsageStatsInfo", "startTime: $startTime endTime: $endTime")

        return usageStatsMap ?: emptyMap()
    }

    override suspend fun getAppUsageData(): List<Flow<AppUsageData>> {
        val usageList = appUsageDataDao.getAllAppUsageData()
        return usageList.map { usageData ->
            flow { emit(usageData) }
        }
    }

    override suspend fun insertAppUsageData(appUsageData: AppUsageData) {
        appUsageDataDao.insert(appUsageData)
    }

    override suspend fun syncAndInsertAppUsageData(userId: Int) {

        val userProfile = UserProfile(1, "What's your dogs name", "kiki")
        userProfileDao.insert(userProfile)
        val usageStatsMap = getUsageStats()
        val allApps = appInformationDao.getAllAppsSnapshot()
        usageStatsMap.values.forEach { usageStats ->
            val appInfo = allApps.find { it.packageName == usageStats.packageName }
            val appId = appInfo?.appId ?: return@forEach
            val totalTimeInMinutes = (usageStats.totalTimeInForeground / 60000) % 60
            val totalTimeInHours = usageStats.totalTimeInForeground / 3600000
            if (totalTimeInMinutes > 0 || totalTimeInHours > 0) {
                val appUsageData = com.example.abl.data.database.entity.AppUsageData(
                    id = 0, // auto-generate
                    userId = userId,
                    appId = appId,
                    totalTimeInHours = totalTimeInHours,
                    totalTimeInMinutes = totalTimeInMinutes,
                    lastTimeUsed = usageStats.lastTimeUsed.toString(),
                    firstTimeUsed = usageStats.firstTimeStamp.toString()
                )
                appUsageDataDao.insert(appUsageData)
            }
        }
    }

    override suspend fun getAllAppsSnapshot(): List<com.example.abl.data.database.entity.AppInformation> {
        val myPackageName = context.packageName
        return appInformationDao.getAllAppsSnapshot().filter { it.packageName != myPackageName }
    }

    suspend fun getTopAppsThisHour(topN: Int = 3): List<Pair<String, String>> { // returns List of (packageName, appName)
        val myPackageName = context.packageName
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        val allApps = appInformationDao.getAllAppsSnapshot().filter { it.packageName != myPackageName }
        return usageStatsList
            .filter { it.totalTimeInForeground > 0 && it.packageName != myPackageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(topN)
            .mapNotNull { usage ->
                val appInfo = allApps.find { it.packageName == usage.packageName }
                if (appInfo != null) appInfo.packageName to appInfo.name else null
            }
    }
}