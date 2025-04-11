package com.example.abl.data

import com.example.abl.data.database.AppDatabase
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.data.database.entity.RiskyApp
import com.example.abl.data.database.entity.UserProfile
import javax.inject.Inject

class DefaultAppRepository @Inject constructor(
    private val database: AppDatabase
) : AppRepository {
    override suspend fun getAllAppInformation(): List<AppInformation> {
        return database.appInformationDao().getAllApps();
    }

    override suspend fun getAppInformationById(appId: Int): AppInformation? {
        return database.appInformationDao().getAppById(appId)
    }

    override suspend fun insertAppInformation(appInformation: AppInformation) {
        database.appInformationDao().insert(appInformation)
    }

    override suspend fun insertAppUsageData(appUsageData: AppUsageData) {
        database.appUsageDataDao().insert(appUsageData)
    }

    override suspend fun getUserProfile(userId: Int): UserProfile? {
        return database.userProfileDao().getUserProfileById(userId)
    }

    override suspend fun insertUserProfile(userProfile: UserProfile) {
        database.userProfileDao().insert(userProfile)
    }

    override suspend fun getAllRiskyApps(): List<RiskyApp> {
        return database.riskyAppDao().getAllRiskyApps()
    }

    override suspend fun insertRiskyApp(riskyApp: RiskyApp) {
        database.riskyAppDao().insert(riskyApp)
    }
}