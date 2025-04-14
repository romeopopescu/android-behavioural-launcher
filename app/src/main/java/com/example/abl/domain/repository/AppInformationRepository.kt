package com.example.abl.domain.repository

import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.model.StateResources
import kotlinx.coroutines.flow.Flow

interface AppInformationRepository {
    fun getAllApps(): Flow<StateResources<List<AppInformation>>>
    suspend fun insert(app: AppInformation)
    suspend fun getAppById(id: Int): AppInformation
}