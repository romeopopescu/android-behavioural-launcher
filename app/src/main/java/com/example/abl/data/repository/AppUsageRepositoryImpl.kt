package com.example.abl.data.repository

import android.app.usage.UsageStats
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.domain.repository.AppUsageRepository
import kotlinx.coroutines.flow.Flow

class AppUsageRepositoryImpl: AppUsageRepository {
    override fun hasUsageStatsPermission(): Boolean {
        TODO("Not yet implemented")
    }

    override fun openUsageAccessSettings() {
        TODO("Not yet implemented")
    }

    override fun getUsageStats(): Map<String, UsageStats> {
        TODO("Not yet implemented")
    }

    override fun getAppUsageData(): List<Flow<AppUsageData>> {
        TODO("Not yet implemented")
    }


}