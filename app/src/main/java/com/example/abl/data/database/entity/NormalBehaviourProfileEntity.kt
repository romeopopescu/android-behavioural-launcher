package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.abl.data.database.model.ProfileTypeConverters

@Entity(tableName = "normal_behaviour_profiles")
@TypeConverters(ProfileTypeConverters::class)
data class NormalBehaviourProfileEntity(
    @PrimaryKey val profileId: String = "user_default",
    val lastGeneratedTimestamp: Long,
    // List<AppSpecificProfileEntity>
    val allowedInfrequentApps: Set<String>,
    val typicalDailyActiveHours: Set<Int>,
    val typicalDailyTotalUsageTimeMsStart: Long,
    val typicalDailyTotalUsageTimeMsEnd: Long
) 