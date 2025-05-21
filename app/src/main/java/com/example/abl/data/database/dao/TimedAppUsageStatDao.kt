package com.example.abl.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.abl.data.database.entity.TimedAppUsageStat

@Dao
interface TimedAppUsageStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: TimedAppUsageStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<TimedAppUsageStat>)

    // This query will be used to get the top N package names based on
    // their total usage duration for a specific day of the week and hour of the day.
    // We'll sum the durations for each package, group by package name,
    // order by the total duration in descending order, and then take the top 'limit'.
    @Query("""
        SELECT packageName, SUM(durationMillis) as totalDuration
        FROM TimedAppUsageStat
        WHERE dayOfWeek = :dayOfWeek AND hourOfDay = :hourOfDay
        GROUP BY packageName
        ORDER BY totalDuration DESC
        LIMIT :limit
    """)
    suspend fun getTopAppsForTimeSlot(dayOfWeek: Int, hourOfDay: Int, limit: Int): List<AppUsageTimeSlotSummary>

    // Data class to hold the result of the aggregation query
    data class AppUsageTimeSlotSummary(
        val packageName: String,
        val totalDuration: Long
    )

    // Optional: A function to clear old data if the table grows too large
    @Query("DELETE FROM TimedAppUsageStat WHERE usageTimestamp < :timestamp")
    suspend fun clearOldStats(timestamp: Long)

    @Query("SELECT * FROM TimedAppUsageStat")
    suspend fun getAll(): List<TimedAppUsageStat> // For debugging or other potential uses
}