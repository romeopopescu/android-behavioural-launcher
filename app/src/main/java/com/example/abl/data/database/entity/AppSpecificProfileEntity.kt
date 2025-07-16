package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.abl.data.database.model.ProfileTypeConverters

@Entity(
    tableName = "app_specific_profiles",
    foreignKeys = [
        ForeignKey(
            entity = NormalBehaviourProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["ownerProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["ownerProfileId"])]
)
@TypeConverters(ProfileTypeConverters::class)
data class AppSpecificProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerProfileId: String,
    val packageName: String,
    val typicalTotalForegroundTimePerDayMsStart: Long,
    val typicalTotalForegroundTimePerDayMsEnd: Long,
    val typicalLaunchCountPerDayStart: Int,
    val typicalLaunchCountPerDayEnd: Int,
    val commonHoursOfDay: Set<Int>,
    val commonDaysOfWeek: Set<Int>
) 