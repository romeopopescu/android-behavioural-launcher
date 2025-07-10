package com.example.abl.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "today_usage_accumulator")
data class TodayUsage(
    @PrimaryKey
    val packageName: String,
    var launchCount: Int,
    var totalTimeInForeground: Long,
    var firstHourUsed: Int,
    var lastHourUsed: Int,
    val dayOfWeek: Int
)