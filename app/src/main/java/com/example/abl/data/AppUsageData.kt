package com.example.abl.data

data class AppUsageData(
    val packageName: String,
    val totalTimeInHours: Long,
    val totalTimeInMinutes: Long,
    val lastTimeUsed: String,
    val firstTimeUsed: String
)