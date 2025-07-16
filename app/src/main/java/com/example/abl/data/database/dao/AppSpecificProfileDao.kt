package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Delete
import com.example.abl.data.database.entity.AppSpecificProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSpecificProfileDao {

    @Query("SELECT * FROM app_specific_profiles WHERE id = :id")
    fun getAppSpecificProfileById(id: Long): Flow<AppSpecificProfileEntity?>

    @Query("SELECT * FROM app_specific_profiles WHERE ownerProfileId = :ownerProfileId")
    fun getAppSpecificProfilesForOwner(ownerProfileId: String): Flow<List<AppSpecificProfileEntity>>

    @Query("SELECT * FROM app_specific_profiles WHERE packageName = :packageName")
    fun getAllProfilesForPackage(packageName: String): Flow<List<AppSpecificProfileEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appProfile: AppSpecificProfileEntity): Long

    @Update
    suspend fun update(appProfile: AppSpecificProfileEntity)

    @Delete
    suspend fun delete(appProfile: AppSpecificProfileEntity)

    @Query("DELETE FROM app_specific_profiles")
    suspend fun clearAll()
}