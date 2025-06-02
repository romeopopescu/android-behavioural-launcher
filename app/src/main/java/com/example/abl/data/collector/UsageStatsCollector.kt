package com.example.abl.data.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageRecordDao: AppUsageRecordDao
) {
    private val TAG = "UsageStatsCollector"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        // Optionally, set timezone for logging if needed for clarity, though timestamps are UTC
    }

    // New method to collect for a number of past days, one day at a time
    suspend fun collectAndStoreUsageDataForPastDays(daysToCollect: Int) {
        Log.d(TAG, "Starting collection for the past $daysToCollect days.")
        val allCollectedRecords = mutableListOf<AppUsageRecord>()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")) // Use UTC calendar

        for (i in 0 until daysToCollect) {
            // Set calendar to the start of the target day (i days ago)
            calendar.timeInMillis = System.currentTimeMillis() // Reset to now
            calendar.add(Calendar.DAY_OF_YEAR, -i) // Go back i days
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStartTime = calendar.timeInMillis

            // Set calendar to the end of the target day
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val dayEndTime = calendar.timeInMillis

            Log.d(TAG, "Querying for day ${daysToCollect - i} (Date: ${dateFormat.format(Date(dayStartTime))}): ${dateFormat.format(Date(dayStartTime))} to ${dateFormat.format(Date(dayEndTime))}")
            val dailyRecords = fetchAndProcessUsageForPeriod(dayStartTime, dayEndTime)
            allCollectedRecords.addAll(dailyRecords)
        }

        if (allCollectedRecords.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    appUsageRecordDao.insertAll(allCollectedRecords) // Insert all collected daily records
                    Log.i(TAG, "Successfully inserted ${allCollectedRecords.size} total usage records from $daysToCollect days into the database.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting combined usage records into database", e)
            }
        } else {
            Log.d(TAG, "No records collected across $daysToCollect days to insert into the database.")
        }
    }

    // Renamed and refactored original method to process a specific period (typically one day)
    private fun fetchAndProcessUsageForPeriod(startTime: Long, endTime: Long): List<AppUsageRecord> {
        Log.d(TAG, "Fetching usage for period: ${dateFormat.format(Date(startTime))} to ${dateFormat.format(Date(endTime))}")

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val queryUsageStats: List<UsageStats>?
        try {
            queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, // INTERVAL_DAILY for a single day query is fine
                startTime,
                endTime
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException for period. Is PACKAGE_USAGE_STATS granted?", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying usage stats for period.", e)
            return emptyList()
        }

        if (queryUsageStats == null) {
            Log.w(TAG, "queryUsageStats returned null for period.")
            return emptyList()
        }

        Log.d(TAG, "queryUsageStats for period returned ${queryUsageStats.size} items.")

        val records = mutableListOf<AppUsageRecord>()
        val recordTimestamp = System.currentTimeMillis() // When this batch of records is being processed
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        for ((index, usageStat) in queryUsageStats.withIndex()) {
            // Ensure that the usageStat's timestamps roughly fall within the queried day to avoid double counting if API behaves unexpectedly
            // For a single day query with INTERVAL_DAILY, usageStat.firstTimeStamp and lastTimeStamp should be within that day.
            Log.d(TAG, "Processing UsageStat #$index: Pkg=${usageStat.packageName}, LastTimeUsed=${dateFormat.format(Date(usageStat.lastTimeUsed))}, TotalForegroundTime=${usageStat.totalTimeInForeground}, FirstTimeStamp=${dateFormat.format(Date(usageStat.firstTimeStamp))}, LastTimeStamp=${dateFormat.format(Date(usageStat.lastTimeStamp))}")

            if (usageStat.totalTimeInForeground > 0) {
                // For hour/day, use lastTimeUsed, as it reflects the point of activity.
                // If lastTimeUsed is outside the current day's processing window, it might be an aggregate from a wider system view.
                // However, for a single-day query, it should be within the day.
                calendar.timeInMillis = usageStat.lastTimeUsed 
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY=1 to Calendar.SATURDAY=7

                records.add(
                    AppUsageRecord(
                        packageName = usageStat.packageName,
                        // Store the actual day this record represents
                        queryStartTime = startTime, 
                        queryEndTime = endTime,
                        totalTimeInForeground = usageStat.totalTimeInForeground,
                        lastTimeUsedOverall = usageStat.lastTimeUsed,
                        recordedAt = recordTimestamp, // Time this specific record was processed and stored
                        launchCount = 1, // Placeholder: Improve with UsageEvents for accuracy
                        hourOfDayUsed = hourOfDay,
                        dayOfWeekUsed = dayOfWeek
                    )
                )
            } else {
                Log.d(TAG, "Skipping UsageStat for ${usageStat.packageName} (period) due to totalTimeInForeground <= 0.")
            }
        }
        Log.d(TAG, "Prepared ${records.size} AppUsageRecord items for period: ${dateFormat.format(Date(startTime))} to ${dateFormat.format(Date(endTime))}.")
        return records
    }

    // Keep the old method signature if it's directly called elsewhere, 
    // but deprecate or make it call the new one.
    // For now, I'll assume the ViewModel will call the new method.
    /*
    suspend fun collectAndStoreUsageData(startTime: Long, endTime: Long) {
        Log.w(TAG, "Legacy collectAndStoreUsageData(startTime, endTime) called. Consider switching to collectAndStoreUsageDataForPastDays.")
        // This old method logic would result in one aggregate record per app for the whole period.
        // To keep its old behavior if needed, or adapt it.
        // For this refactor, we'll focus on the daily collection via the new method.
        val processedRecords = fetchAndProcessUsageForPeriod(startTime, endTime)
        if (processedRecords.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    appUsageRecordDao.insertAll(processedRecords)
                    Log.i(TAG, "Successfully inserted ${processedRecords.size} usage records (single period) into the database.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting single-period usage records into database", e)
            }
        }
    }
    */
}