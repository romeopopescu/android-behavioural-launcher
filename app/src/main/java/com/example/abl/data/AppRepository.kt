package com.example.abl.data

import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.data.database.entity.RiskyApp
import com.example.abl.data.database.entity.UserProfile

interface AppRepository {
    suspend fun getAllAppInformation(): List<AppInformation>
    suspend fun getAppInformationById(appId: Int): AppInformation?
    suspend fun insertAppInformation(appInformation: AppInformation)

    suspend fun insertAppUsageData(appUsageData: AppUsageData);

    suspend fun getUserProfile(userId: Int): UserProfile?
    suspend fun insertUserProfile(userProfile: UserProfile)

    suspend fun getAllRiskyApps(): List<RiskyApp>
    suspend fun insertRiskyApp(riskyApp: RiskyApp)
}