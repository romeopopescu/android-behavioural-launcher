package com.example.abl.domain.repository

import android.graphics.drawable.Drawable
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.model.StateResources
import kotlinx.coroutines.flow.Flow

interface AppInformationRepository {
    fun getAllApps(): Flow<List<AppInformation>>
    suspend fun syncApps()
    fun getAppIcon(packageName: String): Drawable?
    suspend fun launchApp(packageName: String): Boolean
}