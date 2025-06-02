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

    // Insert methods for profile and its app-specific parts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNormalProfile(profile: NormalBehaviourProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSpecificProfiles(appProfiles: List<AppSpecificProfileEntity>)

    /**
     * Inserts or replaces a complete NormalBehaviourProfileWithApps.
     * This method handles deleting old app-specific profiles for the given profileId
     * before inserting the new ones to ensure data consistency.
     */
    @Transaction
    suspend fun insertOrReplaceProfileWithApps(profile: NormalBehaviourProfileEntity, appSpecificProfiles: List<AppSpecificProfileEntity>) {
        // First, delete any existing app-specific profiles for this profile ID
        // This is important to handle updates correctly where some app profiles might be removed.
        // The ForeignKey constraint with onDelete = CASCADE on AppSpecificProfileEntity handles this if NormalBehaviourProfileEntity is deleted.
        // However, if we are just updating the list of AppSpecificProfileEntity for an *existing* NormalBehaviourProfileEntity,
        // we need to clear the old ones manually if their ownerProfileId is the same.
        deleteAppSpecificProfilesByOwnerId(profile.profileId)
        
        insertNormalProfile(profile) // Insert or replace the main profile
        // Ensure ownerProfileId is set for all app-specific profiles if it wasn't already
        val correctlyOwnedAppProfiles = appSpecificProfiles.map { it.copy(ownerProfileId = profile.profileId) }
        if (correctlyOwnedAppProfiles.isNotEmpty()) {
            insertAppSpecificProfiles(correctlyOwnedAppProfiles) // Insert the new app-specific profiles
        }
    }

    @Query("DELETE FROM app_specific_profiles WHERE ownerProfileId = :ownerProfileId")
    suspend fun deleteAppSpecificProfilesByOwnerId(ownerProfileId: String)

    @Query("DELETE FROM normal_behaviour_profiles WHERE profileId = :profileId")
    suspend fun deleteNormalProfile(profileId: String = "user_default")

    @Transaction
    suspend fun clearAllProfileData(profileId: String = "user_default") {
        deleteAppSpecificProfilesByOwnerId(profileId) // Delete app specifics first
        deleteNormalProfile(profileId)             // Then delete the main profile
    }
}