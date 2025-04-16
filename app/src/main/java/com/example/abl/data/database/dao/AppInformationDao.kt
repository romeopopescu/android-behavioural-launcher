package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.AppInformation
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInformationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppInformation)

    @Query("SELECT * FROM AppInformation ORDER BY name COLLATE NOCASE ASC")
    fun getAllApps(): Flow<List<AppInformation>>

    @Query("SELECT * FROM AppInformation WHERE appId = :id")
    suspend fun getAppById(id: Int): AppInformation

    @Query("SELECT * FROM AppInformation")
    suspend fun getAllAppsSnapshot(): List<AppInformation>

    @Query("DELETE FROM AppInformation WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}