package com.example.abl.data.database.entity


data class AppUsageRecord(
    val packageName: String,
    val totalTimeInHours: Long,
    val totalTimeInMinutes: Long,
    val lastTimeUsed: String,
    val firstTimeUsed: String
)