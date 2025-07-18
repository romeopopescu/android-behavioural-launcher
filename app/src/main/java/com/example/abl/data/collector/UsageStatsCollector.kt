package com.example.abl.data.collector

import android.app.usage.UsageEvents
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

private data class AppDailyActivityDetails(
    var firstTimestamp: Long = -1L,
    var lastTimestamp: Long = -1L,
    var launchCount: Int = 0,
    var totalTimeInForeground: Long = 0L
)

class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageRecordDao: AppUsageRecordDao
) {
    private val TAG = "UsageStatsCollector"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        Log.d(TAG, Locale.getDefault().toString())
    }

    private val debugging = "SettingTime"

    suspend fun collectAndStoreUsageDataForPastDays(daysToCollect: Int) {

        val overallEndTimestamp = System.currentTimeMillis()

        val tempUtcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        tempUtcCal.timeInMillis = overallEndTimestamp

        val daysToSubtract = -(daysToCollect - 1)

        tempUtcCal.add(Calendar.DAY_OF_YEAR, daysToSubtract)

        tempUtcCal.set(Calendar.HOUR_OF_DAY, 0)
        tempUtcCal.set(Calendar.MINUTE, 0)
        tempUtcCal.set(Calendar.SECOND, 0)
        tempUtcCal.set(Calendar.MILLISECOND, 0)

        val overallStartTimestamp = tempUtcCal.timeInMillis

        withContext(Dispatchers.IO) {
            appUsageRecordDao.deleteRecordsByQueryDateRange(overallStartTimestamp, overallEndTimestamp)
        }

        val allCollectedRecords = mutableListOf<AppUsageRecord>()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")) 

        for (i in 0 until daysToCollect) {
            calendar.timeInMillis = System.currentTimeMillis() 
            calendar.add(Calendar.DAY_OF_YEAR, -i) 
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStartTime = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val dayEndTime = calendar.timeInMillis

            val dailyRecords = fetchAndProcessUsageForPeriod(dayStartTime, dayEndTime)
            allCollectedRecords.addAll(dailyRecords)
        }

        if (allCollectedRecords.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    appUsageRecordDao.insertAll(allCollectedRecords) 
                    Log.i(TAG, "Successfully inserted ${allCollectedRecords.size} total usage records from $daysToCollect days into the database.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting combined usage records into database", e)
            }
        } else {
            Log.d(TAG, "No records collected across $daysToCollect days to insert into the database.")
        }
    }

    private suspend fun fetchAndProcessUsageForPeriod(dayStartTime: Long, dayEndTime: Long): List<AppUsageRecord> {

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val rawUsageStatsList: List<UsageStats>? = try {
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStartTime, dayEndTime)
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying usage stats for period.", e)
            null
        }

        if (rawUsageStatsList == null || rawUsageStatsList.isEmpty()) {
            Log.w(TAG, "queryUsageStats returned null or empty for period ${dateFormat.format(Date(dayStartTime))}.")
        }
        Log.d(TAG, "queryUsageStats for period ${dateFormat.format(Date(dayStartTime))} returned ${rawUsageStatsList?.size ?: 0} raw items.")

        val aggregatedUsageStatsMap = mutableMapOf<String, UsageStats>()
        rawUsageStatsList?.forEach { stat ->
            val existingStat = aggregatedUsageStatsMap[stat.packageName]
            if (existingStat == null) {
                aggregatedUsageStatsMap[stat.packageName] = stat
            } else {
                try {
                    val combinedStat = UsageStats(stat)
                    combinedStat.add(existingStat)
                    aggregatedUsageStatsMap[stat.packageName] = combinedStat
                     Log.w(TAG, "Aggregated duplicate UsageStat for ${stat.packageName} on ${dateFormat.format(Date(dayStartTime))}. New total time: ${combinedStat.totalTimeInForeground}")
                } catch (e: NoSuchMethodError) {
                    val newTotalTime = existingStat.totalTimeInForeground + stat.totalTimeInForeground
                    val newLastTimeUsed = maxOf(existingStat.lastTimeUsed, stat.lastTimeUsed)
                    if (stat.totalTimeInForeground > existingStat.totalTimeInForeground) {
                        aggregatedUsageStatsMap[stat.packageName] = stat
                    }
                    Log.w(TAG, "Could not fully aggregate UsageStat for ${stat.packageName} due to API level. Used heuristic. Combined time approx: $newTotalTime")
                }
            }
        }
        Log.d(TAG, "Aggregated UsageStats to ${aggregatedUsageStatsMap.size} items for period ${dateFormat.format(Date(dayStartTime))}.")

        val appEventDetails = mutableMapOf<String, AppDailyActivityDetails>()
        try {
            val usageEvents = usageStatsManager.queryEvents(dayStartTime, dayEndTime)
            if (usageEvents != null) {
                val event = UsageEvents.Event()
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.timeStamp < dayStartTime || event.timeStamp > dayEndTime) continue

                    val packageName = event.packageName ?: continue
                    Log.d(debugging, "Found event for package $packageName")
                    val timestamp = event.timeStamp
                    Log.d(debugging, "Event timestamp: $timestamp")
                    val details = appEventDetails.getOrPut(packageName) { AppDailyActivityDetails() }

                    when (event.eventType) {
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            details.launchCount++
                            if (details.firstTimestamp == -1L || timestamp < details.firstTimestamp) {
                                details.firstTimestamp = timestamp
                            }
                            if (timestamp > details.lastTimestamp) {
                                details.lastTimestamp = timestamp
                            }
                        }
                        UsageEvents.Event.MOVE_TO_BACKGROUND,
                        UsageEvents.Event.ACTIVITY_STOPPED,
                        UsageEvents.Event.DEVICE_SHUTDOWN,
                        UsageEvents.Event.USER_INTERACTION,
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                            if (timestamp > details.lastTimestamp) {
                                details.lastTimestamp = timestamp
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "queryEvents returned null for period ${dateFormat.format(Date(dayStartTime))}.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying or processing usage events for period ${dateFormat.format(Date(dayStartTime))}.", e)
        }
        Log.d(TAG, "Processed ${appEventDetails.size} packages from UsageEvents for period ${dateFormat.format(Date(dayStartTime))}.")


            val records = mutableListOf<AppUsageRecord>()
        val recordTimestamp = System.currentTimeMillis()

        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = dayStartTime
        val dayOfWeekForRecord = utcCalendar.get(Calendar.DAY_OF_WEEK)
        
        Log.d(debugging, "For day starting ${dateFormat.format(Date(dayStartTime))}, dayOfWeekForRecord set to: $dayOfWeekForRecord (1=Sun, 7=Sat)")

        val processedPackages = mutableSetOf<String>()

        appEventDetails.forEach { (packageName, eventData) ->
            processedPackages.add(packageName)
            val usageStat = aggregatedUsageStatsMap[packageName]

            var totalForegroundTime = usageStat?.totalTimeInForeground ?: 0L


            if (totalForegroundTime <= 0 && eventData.launchCount == 0 && eventData.firstTimestamp == -1L) {
                Log.d(TAG, "Skipping Pkg=$packageName for ${dateFormat.format(Date(dayStartTime))}: No UsageStats foreground time and no launch/first-use events.")
                return@forEach
            }
            
            val actualFirstTimestampForDay = if (eventData.firstTimestamp in dayStartTime..dayEndTime) eventData.firstTimestamp else -1L
            
            var actualLastTimestampForDay = -1L
            if (eventData.lastTimestamp in dayStartTime..dayEndTime) {
                actualLastTimestampForDay = eventData.lastTimestamp
            }

            if (actualFirstTimestampForDay != -1L && actualLastTimestampForDay != -1L && actualLastTimestampForDay < actualFirstTimestampForDay) {
                Log.w(TAG, "Correcting lastTimestamp for Pkg=$packageName on ${dateFormat.format(Date(dayStartTime))}. Original lastTS=${dateFormat.format(Date(actualLastTimestampForDay))} was before firstTS=${dateFormat.format(Date(actualFirstTimestampForDay))}. Setting lastTS = firstTS.")
                actualLastTimestampForDay = actualFirstTimestampForDay
            } else if (actualFirstTimestampForDay != -1L && actualLastTimestampForDay == -1L) {
                actualLastTimestampForDay = actualFirstTimestampForDay
            }


            val firstHour = if (actualFirstTimestampForDay != -1L) {
                utcCalendar.timeInMillis = actualFirstTimestampForDay
                utcCalendar.get(Calendar.HOUR_OF_DAY)
            } else -1

            val lastHour = if (actualLastTimestampForDay != -1L) {
                utcCalendar.timeInMillis = actualLastTimestampForDay
                utcCalendar.get(Calendar.HOUR_OF_DAY)
            } else -1

            if (firstHour != -1 && lastHour != -1 && firstHour > lastHour) {
                 Log.e(TAG, "CRITICAL HourInconsistency for Pkg=$packageName, Day=${dateFormat.format(Date(dayStartTime))}: FirstHour=${firstHour}, LastHour=${lastHour}. This should have been caught by timestamp logic.")
            }

                    records.add(
                        AppUsageRecord(
                    packageName = packageName,
                    queryStartTime = dayStartTime,
                    queryEndTime = dayEndTime,
                    totalTimeInForeground = totalForegroundTime,
                    recordedAt = recordTimestamp,
                    launchCount = eventData.launchCount,
                    dayOfWeekUsed = dayOfWeekForRecord,
                    firstHourUsed = firstHour,
                    lastHourUsed = lastHour
                )
            )
        }
        
        aggregatedUsageStatsMap.forEach { (packageName, usageStat) ->
            if (packageName in processedPackages) return@forEach

            if (usageStat.totalTimeInForeground <= 0) {
                Log.d(TAG, "Skipping Pkg=$packageName (from UsageStats only) for ${dateFormat.format(Date(dayStartTime))}: totalTimeInForeground <= 0.")
                return@forEach
            }
            
            var inferredLastHour = -1
            if (usageStat.lastTimeUsed in dayStartTime..dayEndTime) {
                utcCalendar.timeInMillis = usageStat.lastTimeUsed
                inferredLastHour = utcCalendar.get(Calendar.HOUR_OF_DAY)
            }

            records.add(
                AppUsageRecord(
                    packageName = packageName,
                    queryStartTime = dayStartTime,
                    queryEndTime = dayEndTime,
                    totalTimeInForeground = usageStat.totalTimeInForeground,
                    recordedAt = recordTimestamp,
                    launchCount = 0,
                    dayOfWeekUsed = dayOfWeekForRecord,
                    firstHourUsed = -1,
                    lastHourUsed = inferredLastHour
                )
            )
        }

        return records
    }

}