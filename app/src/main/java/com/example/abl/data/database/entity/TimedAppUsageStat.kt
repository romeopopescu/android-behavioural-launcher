package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TimedAppUsageStat")
data class TimedAppUsageStat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val usageTimestamp: Long, // Timestamp of when the usage occurred
    val durationMillis: Long, // How long the app was used in this session
    val dayOfWeek: Int,       // Day of the week (e.g., Calendar.DAY_OF_WEEK)
    val hourOfDay: Int        // Hour of the day (0-23)
)