package com.example.abl.data.di

import com.example.abl.data.repository.*
import com.example.abl.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppInformationRepository(
        impl: AppInformationRepositoryImpl
    ): AppInformationRepository

    @Binds
    @Singleton
    abstract fun bindAppUsageRepository(
        impl: AppUsageRepositoryImpl
    ): AppUsageRepository

    @Binds
    @Singleton
    abstract fun bindRiskyAppRepository(
        impl: RiskyAppRepositoryImpl
    ): RiskyAppRepository

    @Binds
    @Singleton
    abstract fun bindBehaviouralProfileRepository(
        impl: BehaviouralProfileRepositoryImpl
    ): BehaviouralProfileRepository
}
