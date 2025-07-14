package com.example.abl.data.database

import android.app.Application
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.dao.AppUsageDataDao
import com.example.abl.data.database.dao.AppUsagePatternDao
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.dao.RecommendationsDao
import com.example.abl.data.database.dao.RiskyAppDao
import com.example.abl.data.database.dao.UserProfileDao
import com.example.abl.data.database.dao.NormalBehaviourProfileDao
import com.example.abl.data.database.dao.AppSpecificProfileDao
import com.example.abl.data.database.dao.TodayUsageDao
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.data.database.entity.AppUsagePattern
import com.example.abl.data.database.entity.AppUsageRecord
import com.example.abl.data.database.entity.Recommendations
import com.example.abl.data.database.entity.RiskyApp
import com.example.abl.data.database.entity.UserProfile
import com.example.abl.data.database.entity.NormalBehaviourProfileEntity
import com.example.abl.data.database.entity.AppSpecificProfileEntity
import com.example.abl.data.database.entity.TodayUsage
import com.example.abl.data.database.model.ProfileTypeConverters
import dagger.hilt.android.HiltAndroidApp

@Database(
    entities = [
        AppInformation::class, AppUsageData::class,
        AppUsagePattern::class, Recommendations::class,
        RiskyApp::class, UserProfile::class, AppUsageRecord::class,
        NormalBehaviourProfileEntity::class,
        AppSpecificProfileEntity::class, TodayUsage::class
    ],
    version = 1
)
@TypeConverters(ProfileTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appInformationDao(): AppInformationDao
    abstract fun appUsageDataDao(): AppUsageDataDao
    abstract fun appUsagePatternDao(): AppUsagePatternDao
    abstract fun recommendationsDao(): RecommendationsDao
    abstract fun riskyAppDao(): RiskyAppDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun appUsageRecordDao(): AppUsageRecordDao
    abstract fun normalBehaviourProfileDao(): NormalBehaviourProfileDao
    abstract fun appSpecificProfileDao(): AppSpecificProfileDao
    abstract fun todayUsageDao(): TodayUsageDao
}