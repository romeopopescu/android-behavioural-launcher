package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.UserProfile

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userProfile: UserProfile)

    //this won't be used most likely
    @Query("SELECT * FROM UserProfile")
    suspend fun getAllUserProfiles(): List<UserProfile>

    @Query("SELECT * FROM UserProfile WHERE id = :userId")
    suspend fun getUserProfileById(userId: Int): UserProfile
}