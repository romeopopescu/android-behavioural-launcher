package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.AppInformation

@Dao
interface AppInformationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppInformation)

    @Query("SELECT * FROM AppInformation")
    suspend fun getAllApps(): List<AppInformation>
}