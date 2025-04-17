package com.example.abl.domain.repository

import android.app.usage.UsageStats
import android.content.Context
import com.example.abl.data.database.entity.AppUsageData
import kotlinx.coroutines.flow.Flow

interface AppUsageRepository {
    fun hasUsageStatsPermission(): Boolean
    fun openUsageAccessSettings()
    fun getUsageStats(): Map<String, UsageStats>
    fun getAppUsageData(): List<Flow<AppUsageData>>
}