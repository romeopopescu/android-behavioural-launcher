package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.AppUsagePattern

@Dao
interface AppUsagePatternDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appUsagePattern: AppUsagePattern)

    @Query("SELECT * FROM AppUsagePattern")
    suspend fun getAllAppUsagePatterns(): List<AppUsagePattern>
}