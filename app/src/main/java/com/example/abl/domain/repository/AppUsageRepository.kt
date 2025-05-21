package com.example.abl.domain.repository

import android.app.usage.UsageStats
import android.content.Context
import com.example.abl.data.database.entity.AppUsageData
import kotlinx.coroutines.flow.Flow

interface AppUsageRepository {
    fun hasUsageStatsPermission(): Boolean
    fun openUsageAccessSettings()
    fun getUsageStats(): Map<String, UsageStats>
    suspend fun getAppUsageData(): List<Flow<AppUsageData>>
    suspend fun insertAppUsageData(appUsageData: AppUsageData)
    suspend fun syncAndInsertAppUsageData(userId: Int)
    suspend fun getAllAppsSnapshot(): List<com.example.abl.data.database.entity.AppInformation>

    // New method for recording detailed, timed usage statistics
    suspend fun recordDetailedUsageStats()

    // New method for getting top apps based on timed usage stats for the current time slot
    suspend fun getTopAppsForCurrentTimeSlot(limit: Int): List<com.example.abl.data.database.dao.TimedAppUsageStatDao.AppUsageTimeSlotSummary>
}