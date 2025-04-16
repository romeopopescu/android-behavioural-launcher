package com.example.abl.data.di

import android.content.Context
import androidx.room.Room
import com.example.abl.data.database.AppDatabase
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.dao.AppUsageDataDao
import com.example.abl.data.database.dao.RiskyAppDao
import com.example.abl.data.database.dao.UserProfileDao
import com.example.abl.data.repository.AppInformationRepositoryImpl
import com.example.abl.domain.repository.AppInformationRepository
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
    }
}