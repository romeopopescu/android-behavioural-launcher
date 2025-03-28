package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "AppUsagePattern",
    foreignKeys = [
        ForeignKey(
            entity = AppInformation::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"]), Index(value = ["userId"])]
)
data class AppUsagePattern (
    @PrimaryKey val id: Int,
    val userId: Int,
    val appId: Int,
    val dailyAverage: Int,
    val peakHours: String,
    val usageScore: Float
)