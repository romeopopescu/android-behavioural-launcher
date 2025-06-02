package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_records")
data class AppUsageRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val queryStartTime: Long, // Start time of the interval for which this stat was queried
    val queryEndTime: Long,   // End time of the interval for which this stat was queried
    val totalTimeInForeground: Long, // Total time in foreground during the query interval
    val lastTimeUsedOverall: Long, // Last time the app was used (from UsageStats)
    val recordedAt: Long, // Timestamp when this record was fetched and stored
    val launchCount: Int,
    val hourOfDayUsed: Int,
    val dayOfWeekUsed: Int
)