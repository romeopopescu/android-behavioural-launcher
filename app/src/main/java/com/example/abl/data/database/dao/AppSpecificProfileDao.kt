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

    // Get a specific app profile by its own ID (if ever needed independently)
    @Query("SELECT * FROM app_specific_profiles WHERE id = :id")
    fun getAppSpecificProfileById(id: Long): Flow<AppSpecificProfileEntity?>

    // Get all app profiles for a given owner (alternative to the relation in NormalBehaviourProfileDao)
    @Query("SELECT * FROM app_specific_profiles WHERE ownerProfileId = :ownerProfileId")
    fun getAppSpecificProfilesForOwner(ownerProfileId: String): Flow<List<AppSpecificProfileEntity>>

    // Get all app profiles for a specific package name across all owners (if ever needed)
    @Query("SELECT * FROM app_specific_profiles WHERE packageName = :packageName")
    fun getAllProfilesForPackage(packageName: String): Flow<List<AppSpecificProfileEntity>>
    
    // Basic CUD operations, though typically managed via NormalBehaviourProfileDao for transactional integrity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appProfile: AppSpecificProfileEntity): Long

    @Update
    suspend fun update(appProfile: AppSpecificProfileEntity)

    @Delete
    suspend fun delete(appProfile: AppSpecificProfileEntity)

    // Clear all app specific profiles (use with caution, might be better to use NormalBehaviourProfileDao's clear methods)
    @Query("DELETE FROM app_specific_profiles")
    suspend fun clearAll()
}