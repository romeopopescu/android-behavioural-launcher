package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_records")
data class AppUsageRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val queryStartTime: Long, // Start time of the daily interval this record is for
    val queryEndTime: Long,   // End time of the daily interval this record is for
    val totalTimeInForeground: Long, // Total time in foreground for this app on this day
    val recordedAt: Long, // Timestamp when this record was fetched by your app
    val launchCount: Int,     // Number of times the app was launched on this day
    val dayOfWeekUsed: Int,   // The day of the week this record pertains to (from queryStartTime)
    val firstHourUsed: Int,   // The first hour (0-23 UTC) the app was used on this day (-1 if no event)
    val lastHourUsed: Int     // The last hour (0-23 UTC) the app was used on this day (-1 if no event)
)