package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.AppUsageData

@Dao
interface AppUsageDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appUsageData: AppUsageData)

    @Query("SELECT * FROM AppUsageData")
    suspend fun getAllAppUsageData(): List<AppUsageData>
}