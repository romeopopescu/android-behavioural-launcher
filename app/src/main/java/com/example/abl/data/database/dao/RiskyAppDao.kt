package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.RiskyApp

@Dao
interface RiskyAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(riskyApp: RiskyApp)

    @Query("SELECT * FROM RiskyApp")
    suspend fun getAllRiskyApps()
}