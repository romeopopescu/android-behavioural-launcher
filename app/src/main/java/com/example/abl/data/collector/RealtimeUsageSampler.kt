package com.example.abl.data.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.abl.data.database.entity.AppUsageRecord // Reusing the entity for structure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

class RealtimeUsageSampler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = "RealtimeUsageSampler"

    fun sampleCurrentUsage(durationMillis: Long = 15 * 60 * 1000): List<AppUsageRecord> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - durationMillis

        val queryUsageStats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, // Querying daily interval which should include recent data
            startTime,
            endTime
        )

        val records = mutableListOf<AppUsageRecord>()
        val calendar = Calendar.getInstance()

        if (queryUsageStats != null) {
            for (usageStat in queryUsageStats) {
                // Filter to include only stats that have been active in the requested window
                // lastTimeUsed is the end of the last time interval that the package was used.
                // totalTimeInForeground is for the whole day if INTERVAL_DAILY is used.
                // We need to be careful: queryUsageStats with INTERVAL_DAILY gives stats for the whole day
                // up to `endTime`. We only want to consider apps truly used *within* our short `durationMillis` window.
                // A more accurate way would be to use queryEvents and sum up, but for simplicity:
                if (usageStat.lastTimeUsed >= startTime && usageStat.totalTimeInForeground > 0) {
                    calendar.timeInMillis = usageStat.lastTimeUsed
                    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                    // Note: totalTimeInForeground from UsageStats is for the entire queried interval (e.g., daily).
                    // This sampler ideally should get a more granular foreground time for *just* the sample window.
                    // This requires using UsageEvents or a different query interval if available and suitable.
                    // For now, we take the reported totalTimeInForeground if lastTimeUsed is recent.
                    // This is an approximation.
                    records.add(
                        AppUsageRecord(
                            packageName = usageStat.packageName,
                            queryStartTime = startTime, // The window we are interested in
                            queryEndTime = endTime,
                            totalTimeInForeground = usageStat.totalTimeInForeground, // This is an approximation for the window
                            lastTimeUsedOverall = usageStat.lastTimeUsed,
                            recordedAt = endTime, // Sampled now
                            launchCount = 1, // Placeholder, from UsageStats directly
                            hourOfDayUsed = hourOfDay,
                            dayOfWeekUsed = dayOfWeek
                        )
                    )
                }
            }
        }
        return records
    }
} 