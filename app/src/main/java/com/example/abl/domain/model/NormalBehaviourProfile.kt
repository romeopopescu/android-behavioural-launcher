package com.example.abl.domain.model

// Represents the typical usage pattern for a single app
data class AppSpecificProfile(
    val packageName: String,
    val typicalTotalForegroundTimePerDayMs: LongRange, // e.g., 300_000L (5min) .. 1_800_000L (30min)
    val typicalLaunchCountPerDay: IntRange,           // e.g., 1..5
    val commonHoursOfDay: Set<Int>,                   // Hours (0-23) when app is typically used
    val commonDaysOfWeek: Set<Int>                    // Days (1-7, Mon-Sun) when app is typically used
)

// Represents the overall normal behaviour profile for the user
data class NormalBehaviourProfile(
    val profileId: String = "user_default", // Could be enhanced for multi-user scenarios
    val lastGeneratedTimestamp: Long,
    val profiledApps: List<AppSpecificProfile>, // Profile for most frequently/importantly used apps
    val allowedInfrequentApps: Set<String>,     // Package names of apps used rarely but legitimately
    val typicalDailyActiveHours: Set<Int>,      // General hours user is active on the device
    val typicalDailyTotalUsageTimeMs: LongRange // Overall device usage time per day
) 