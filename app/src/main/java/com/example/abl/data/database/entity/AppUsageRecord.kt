package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_records")
data class AppUsageRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val queryStartTime: Long,
    val queryEndTime: Long,
    val totalTimeInForeground: Long,
    val recordedAt: Long,
    val launchCount: Int,
    val dayOfWeekUsed: Int,
    val firstHourUsed: Int,
    val lastHourUsed: Int
)