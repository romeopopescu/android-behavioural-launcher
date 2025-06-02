package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.abl.data.database.model.ProfileTypeConverters

@Entity(tableName = "normal_behaviour_profiles")
@TypeConverters(ProfileTypeConverters::class)
data class NormalBehaviourProfileEntity(
    @PrimaryKey val profileId: String = "user_default", // Default profile ID
    val lastGeneratedTimestamp: Long,
    // List<AppSpecificProfileEntity> will be handled via a relation, not a direct field
    val allowedInfrequentApps: Set<String>,     // Converted by ProfileTypeConverters
    val typicalDailyActiveHours: Set<Int>,      // Converted by ProfileTypeConverters
    val typicalDailyTotalUsageTimeMsStart: Long,
    val typicalDailyTotalUsageTimeMsEnd: Long
) 