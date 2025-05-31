package com.example.abl.domain.repository

import com.example.abl.data.database.entity.RiskyApp
import kotlinx.coroutines.flow.Flow

interface RiskyAppRepository {
    suspend fun insertRiskyApps()
    fun getTopRiskyApps(): Flow<List<RiskyApp>>
}