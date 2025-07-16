package com.example.abl.domain.model

data class AppSpecificProfile(
    val packageName: String,
    val typicalTotalForegroundTimePerDayMs: LongRange,
    val typicalLaunchCountPerDay: IntRange,
    val commonHoursOfDay: Set<Int>,
    val commonDaysOfWeek: Set<Int>
)

data class NormalBehaviourProfile(
    val profileId: String = "user_default",
    val lastGeneratedTimestamp: Long,
    val profiledApps: List<AppSpecificProfile>,
    val allowedInfrequentApps: Set<String>,
    val typicalDailyActiveHours: Set<Int>,
    val typicalDailyTotalUsageTimeMs: LongRange
)