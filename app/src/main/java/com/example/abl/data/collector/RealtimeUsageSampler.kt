package com.example.abl.data.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.abl.data.database.entity.AppUsageRecord // Reusing the entity for structure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import android.app.usage.UsageEvents
import android.util.Log
import java.util.TimeZone

private data class SampledAppActivityDetails(
    var firstTimestamp: Long = -1L,
    var lastTimestamp: Long = -1L,
    var launchCount: Int = 0,
    var totalTimeInForeground: Long = 0L,
    var currentForegroundStartTime: Long = -1L
)

class RealtimeUsageSampler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = "RealtimeUsageSampler"

    fun sampleCurrentUsage(durationMillis: Long = 15 * 60 * 1000): List<AppUsageRecord> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - durationMillis

        val records = mutableListOf<AppUsageRecord>()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = endTime

        val dayOfWeekForRecord = calendar.get(Calendar.DAY_OF_WEEK)
        val recordTimestamp = endTime

        val appEventDetails = mutableMapOf<String, SampledAppActivityDetails>()

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            if (usageEvents != null) {
                val event = UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    val packageName = event.packageName ?: continue
                    val timestamp = event.timeStamp

                    if (timestamp < startTime || timestamp > endTime) continue

                    val details = appEventDetails.getOrPut(packageName) { SampledAppActivityDetails() }

                    when (event.eventType) {
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            details.launchCount++
                            if (details.firstTimestamp == -1L || timestamp < details.firstTimestamp) {
                                details.firstTimestamp = timestamp
                            }
                            if (timestamp > details.lastTimestamp) {
                                details.lastTimestamp = timestamp
                            }
                            if (details.currentForegroundStartTime == -1L) {
                                details.currentForegroundStartTime = timestamp
                            }
                        }
                        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            if (timestamp > details.lastTimestamp) {
                                details.lastTimestamp = timestamp
                            }
                            if (details.currentForegroundStartTime != -1L) {
                                details.totalTimeInForeground += (timestamp - details.currentForegroundStartTime)
                                details.currentForegroundStartTime = -1L
                            }
                        }

                        UsageEvents.Event.ACTIVITY_STOPPED,
                        UsageEvents.Event.USER_INTERACTION,
                        UsageEvents.Event.ACTIVITY_RESUMED,
                        UsageEvents.Event.SCREEN_INTERACTIVE,
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                            if (timestamp > details.lastTimestamp) {
                                details.lastTimestamp = timestamp
                            }
                            if (event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                                if (details.currentForegroundStartTime != -1L) {
                                    details.totalTimeInForeground += (timestamp - details.currentForegroundStartTime)
                                    details.currentForegroundStartTime = -1L
                                }
                            }
                        }
                    }
                }
                appEventDetails.forEach { (_, details) ->
                    if (details.currentForegroundStartTime != -1L) {
                        details.totalTimeInForeground += (endTime - details.currentForegroundStartTime)
                        details.currentForegroundStartTime = -1L // Reset
                        if (details.lastTimestamp < endTime) {
                             details.lastTimestamp = endTime
                        }
                    }
                }


            } else {
                Log.w(TAG, "queryEvents returned null for sampling window.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying or processing usage events for sampling.", e)
        }

        appEventDetails.forEach { (packageName, details) ->
            if (details.launchCount > 0 || details.totalTimeInForeground > 0) {
                val firstHour = if (details.firstTimestamp != -1L) {
                    calendar.timeInMillis = details.firstTimestamp
                    calendar.get(Calendar.HOUR_OF_DAY)
                } else -1

                val lastHour = if (details.lastTimestamp != -1L) {
                    calendar.timeInMillis = details.lastTimestamp
                    calendar.get(Calendar.HOUR_OF_DAY)
                } else -1
                
                if (details.totalTimeInForeground == 0L && details.launchCount > 0 && details.firstTimestamp != -1L) {
                }


                records.add(
                    AppUsageRecord(
                        packageName = packageName,
                        queryStartTime = startTime,
                        queryEndTime = endTime,
                        totalTimeInForeground = details.totalTimeInForeground,
                        recordedAt = recordTimestamp,
                        launchCount = details.launchCount,
                        dayOfWeekUsed = dayOfWeekForRecord,
                        firstHourUsed = firstHour,
                        lastHourUsed = lastHour
                    )
                )
                 Log.d(TAG, "Sampled for Pkg=$packageName: Launches=${details.launchCount}, FgTime=${details.totalTimeInForeground}, FirstHour=$firstHour, LastHour=$lastHour")
            }
        }
        Log.d(TAG, "Realtime Sampler produced ${records.size} records for window ${System.currentTimeMillis() - startTime}ms")
        return records
    }
} 