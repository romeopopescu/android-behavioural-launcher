package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.abl.data.database.entity.AppSpecificProfileEntity
import com.example.abl.data.database.entity.NormalBehaviourProfileEntity
import com.example.abl.data.database.model.NormalBehaviourProfileWithApps
import kotlinx.coroutines.flow.Flow

@Dao
interface NormalBehaviourProfileDao {

    @Transaction
    @Query("SELECT * FROM normal_behaviour_profiles WHERE profileId = :profileId")
    fun getNormalProfileWithApps(profileId: String = "user_default"): Flow<NormalBehaviourProfileWithApps?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNormalProfile(profile: NormalBehaviourProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSpecificProfiles(appProfiles: List<AppSpecificProfileEntity>)

    @Transaction
    suspend fun insertOrReplaceProfileWithApps(profile: NormalBehaviourProfileEntity, appSpecificProfiles: List<AppSpecificProfileEntity>) {
        deleteAppSpecificProfilesByOwnerId(profile.profileId)
        
        insertNormalProfile(profile)
        val correctlyOwnedAppProfiles = appSpecificProfiles.map { it.copy(ownerProfileId = profile.profileId) }
        if (correctlyOwnedAppProfiles.isNotEmpty()) {
            insertAppSpecificProfiles(correctlyOwnedAppProfiles)
        }
    }

    @Query("DELETE FROM app_specific_profiles WHERE ownerProfileId = :ownerProfileId")
    suspend fun deleteAppSpecificProfilesByOwnerId(ownerProfileId: String)

    @Query("DELETE FROM normal_behaviour_profiles WHERE profileId = :profileId")
    suspend fun deleteNormalProfile(profileId: String = "user_default")

    @Transaction
    suspend fun clearAllProfileData(profileId: String = "user_default") {
        deleteAppSpecificProfilesByOwnerId(profileId)
        deleteNormalProfile(profileId)
    }
}