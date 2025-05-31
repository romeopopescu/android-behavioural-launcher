package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.RiskyApp
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskyAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(riskyApp: RiskyApp)

    @Query("SELECT * FROM RiskyApp")
    suspend fun getAllRiskyApps(): List<RiskyApp>

    @Query("DELETE FROM RiskyApp")
    suspend fun deleteAll()

    @Query("DELETE FROM RiskyApp WHERE appId = :appId")
    suspend fun deleteByAppId(appId: Int)

    @Query("SELECT * FROM RiskyApp WHERE appId = :appId LIMIT 1")
    suspend fun getRiskyAppByAppId(appId: Int): RiskyApp?

    @Query("SELECT * FROM RiskyApp ORDER BY riskScore DESC LIMIT 3")
    suspend fun getTop3RiskyApps(): List<RiskyApp>

    @Query("SELECT * FROM RiskyApp ORDER BY riskScore DESC LIMIT 3")
    fun getTop3RiskyAppsFlow(): Flow<List<RiskyApp>>

}