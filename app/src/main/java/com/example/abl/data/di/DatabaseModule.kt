package com.example.abl.data.di

import android.content.Context
import androidx.room.Room
import com.example.abl.data.database.AppDatabase
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.dao.AppUsageDataDao
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.dao.RiskyAppDao
import com.example.abl.data.database.dao.UserProfileDao
import com.example.abl.data.database.dao.NormalBehaviourProfileDao
import com.example.abl.data.database.dao.AppSpecificProfileDao
import com.example.abl.data.repository.AppInformationRepositoryImpl
import com.example.abl.data.repository.AppUsageRepositoryImpl
import com.example.abl.data.repository.BehaviouralProfileRepositoryImpl
import com.example.abl.data.repository.RiskyAppRepositoryImpl
import com.example.abl.domain.repository.AppInformationRepository
import com.example.abl.domain.repository.AppUsageRepository
import com.example.abl.domain.repository.BehaviouralProfileRepository
import com.example.abl.domain.repository.RiskyAppRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    @Binds
    @Singleton
    abstract fun bindAppInformationRepository(
        appInformationRepositoryImpl: AppInformationRepositoryImpl
    ): AppInformationRepository

    @Binds
    @Singleton
    abstract fun bindAppUsageRepository(
        impl: AppUsageRepositoryImpl
    ): AppUsageRepository

    @Binds
    @Singleton
    abstract fun bindRiskyAppRepository(
        riskyAppRepositoryImpl: RiskyAppRepositoryImpl
    ): RiskyAppRepository

    @Binds
    @Singleton
    abstract fun bindBehaviouralProfileRepository(
        behaviouralProfileRepositoryImpl: BehaviouralProfileRepositoryImpl
    ): BehaviouralProfileRepository

    companion object {

        @Singleton
        @Provides
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).build()
        }

        @Singleton
        @Provides
        fun provideAppInformationDao(database: AppDatabase): AppInformationDao {
            return database.appInformationDao()
        }

        @Singleton
        @Provides
        fun provideAppUsageDataDao(database: AppDatabase): AppUsageDataDao {
            return database.appUsageDataDao()
        }

        @Singleton
        @Provides
        fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
            return database.userProfileDao()
        }

        @Singleton
        @Provides
        fun provideRiskyAppDao(database: AppDatabase): RiskyAppDao {
            return database.riskyAppDao()
        }

        @Singleton
        @Provides
        fun provideAppUsageRecordDao(database: AppDatabase): AppUsageRecordDao {
            return database.appUsageRecordDao()
        }

        @Singleton
        @Provides
        fun provideNormalBehaviourProfileDao(database: AppDatabase): NormalBehaviourProfileDao {
            return database.normalBehaviourProfileDao()
        }

        @Singleton
        @Provides
        fun provideAppSpecificProfileDao(database: AppDatabase): AppSpecificProfileDao {
            return database.appSpecificProfileDao()
        }
    }
}