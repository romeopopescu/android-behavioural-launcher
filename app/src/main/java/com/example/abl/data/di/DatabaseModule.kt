package com.example.abl.data.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import com.example.abl.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "abl_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    // You now need to provide ALL DAOs that are used anywhere in your app.
    // I am adding them all here based on your previous file.
    @Provides
    @Singleton
    fun provideAppInformationDao(db: AppDatabase) = db.appInformationDao()

    @Provides
    @Singleton
    fun provideAppUsageDataDao(db: AppDatabase) = db.appUsageDataDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(db: AppDatabase) = db.userProfileDao()

    @Provides
    @Singleton
    fun provideRiskyAppDao(db: AppDatabase) = db.riskyAppDao()

    @Provides
    @Singleton
    fun provideAppUsageRecordDao(db: AppDatabase) = db.appUsageRecordDao()

    @Provides
    @Singleton
    fun provideNormalBehaviourProfileDao(db: AppDatabase) = db.normalBehaviourProfileDao()

    @Provides
    @Singleton
    fun provideAppSpecificProfileDao(db: AppDatabase) = db.appSpecificProfileDao()

    @Provides
    @Singleton
    fun provideTodayUsageDao(db: AppDatabase) = db.todayUsageDao()

    @Provides
    @Singleton
    fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager {
        return context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

}
