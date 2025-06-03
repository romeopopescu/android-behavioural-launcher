package com.example.abl.domain.usecases.profile // Or com.example.abl.domain.manager

import android.util.Log
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord
import com.example.abl.domain.model.AppSpecificProfile
import com.example.abl.domain.model.NormalBehaviourProfile
import com.example.abl.domain.repository.BehaviouralProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviouralProfileManager @Inject constructor(
    private val appUsageRecordDao: AppUsageRecordDao,
    private val behaviouralProfileRepository: BehaviouralProfileRepository
) {

    private val TAG = "BehaviouralProfileMgr"
    private val MIN_RECORDS_FOR_PROFILE = 30 // Adjusted, focus on days more
    private val MIN_DISTINCT_DAYS_FOR_PROFILE = 5 // Minimum distinct days of data needed
    private val APP_PROFILE_MIN_TOTAL_USAGE_TIME_MS = TimeUnit.MINUTES.toMillis(10) // Min total usage over history for an app to be profiled
    private val APP_PROFILE_MIN_TOTAL_LAUNCHES = 5 // Min total launches over history for an app
    private val APP_PROFILE_MIN_ACTIVE_DAYS = 2 // Min distinct days an app was used to be profiled

    private val HOUR_SLOT_RESOLUTION = 1 // e.g., 1-hour slots for common hours

    suspend fun generateOrUpdateNormalProfile(forceRegeneration: Boolean = false) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Attempting to generate/update normal behaviour profile.")

            if (!forceRegeneration) {
                val existingProfile = behaviouralProfileRepository.getNormalBehaviourProfile().firstOrNull()
                if (existingProfile != null && System.currentTimeMillis() - existingProfile.lastGeneratedTimestamp < TimeUnit.DAYS.toMillis(1) /* e.g., refresh daily */) {
                    Log.d(TAG, "Existing profile found, is recent, and not forcing regeneration.")
                    return@withContext
                }
            }

            // Fetch all historical data using queryStartTime for date accuracy.
            // Collect for a reasonable period, e.g., last 30 days, or all time if appropriate.
            // For now, assuming UsageStatsCollector has populated for HISTORICAL_DATA_COLLECTION_DAYS.
            // We should fetch a window relevant for profiling. Let's assume last 30 days of records for profile.
            val profileWindowEnd = System.currentTimeMillis()
            val profileWindowStartCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            profileWindowStartCal.timeInMillis = profileWindowEnd
            profileWindowStartCal.add(Calendar.DAY_OF_MONTH, -30) // Consider records from the last 30 days
            profileWindowStartCal.set(Calendar.HOUR_OF_DAY, 0)
            profileWindowStartCal.set(Calendar.MINUTE, 0)
            profileWindowStartCal.set(Calendar.SECOND, 0)
            profileWindowStartCal.set(Calendar.MILLISECOND, 0)
            val profileWindowStart = profileWindowStartCal.timeInMillis
            
            Log.d(TAG, "Fetching records for profile generation from ${Date(profileWindowStart)} to ${Date(profileWindowEnd)}")

            val allRecords = appUsageRecordDao.getRecordsByQueryDateRange(
                rangeStart = profileWindowStart,
                rangeEnd = profileWindowEnd
            ).firstOrNull() ?: emptyList()

            if (allRecords.isEmpty()) {
                 Log.w(TAG, "No records found in the last 30 days to generate a profile.")
                return@withContext
            }
            Log.d(TAG, "Fetched ${allRecords.size} records for profile generation.")


            val distinctDaysOfData = allRecords.map { record ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = record.queryStartTime }
                cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
            }.distinct().size

            if (distinctDaysOfData < MIN_DISTINCT_DAYS_FOR_PROFILE) {
                Log.w(TAG, "Not enough distinct days of data (found $distinctDaysOfData, need $MIN_DISTINCT_DAYS_FOR_PROFILE).")
                return@withContext
            }

            val appSpecificProfiles = mutableListOf<AppSpecificProfile>()
            val recordsByPackage = allRecords.groupBy { it.packageName }

            val overallUserDailyActivity = mutableMapOf<Pair<Int, Int>, MutableSet<Int>>() // DayOfYear+Year to Set<Hour>

            for ((packageName, packageRecords) in recordsByPackage) {
                val totalUsageTime = packageRecords.sumOf { it.totalTimeInForeground }
                val totalLaunches = packageRecords.sumOf { it.launchCount }
                
                val distinctDaysForApp = packageRecords.map { record ->
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = record.queryStartTime }
                    cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
                }.distinct().size

                if (totalUsageTime < APP_PROFILE_MIN_TOTAL_USAGE_TIME_MS || 
                    totalLaunches < APP_PROFILE_MIN_TOTAL_LAUNCHES ||
                    distinctDaysForApp < APP_PROFILE_MIN_ACTIVE_DAYS) {
                    Log.d(TAG, "Skipping app $packageName: totalUsage=$totalUsageTime, totalLaunches=$totalLaunches, activeDays=$distinctDaysForApp")
                    continue 
                }

                // Per-day stats for this app
                val dailyStatsForApp = packageRecords.groupBy {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it.queryStartTime }
                    cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
                }.mapValues { entry ->
                    Pair(entry.value.sumOf { it.totalTimeInForeground }, entry.value.sumOf { it.launchCount })
                }

                val dailyForegroundTimes = dailyStatsForApp.values.map { it.first } // list of daily foreground times
                val dailyLaunchCounts = dailyStatsForApp.values.map { it.second } // list of daily launch counts
                
                val avgDailyUsageMs = dailyForegroundTimes.average().toLong()
                val avgDailyLaunches = dailyLaunchCounts.average().toInt()

                // Calculate common hours of day based on firstHourUsed and lastHourUsed
                val hourFrequencies = mutableMapOf<Int, Int>()
                packageRecords.forEach { record ->
                    if (record.firstHourUsed != -1 && record.lastHourUsed != -1) {
                        for (hour in record.firstHourUsed..record.lastHourUsed) {
                            hourFrequencies[hour] = hourFrequencies.getOrDefault(hour, 0) + 1
                        }
                    }
                    // Update overall user daily activity
                    val dayKey = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = record.queryStartTime }.let { it.get(Calendar.DAY_OF_YEAR) to it.get(Calendar.YEAR) }
                    val dailyHoursSet = overallUserDailyActivity.getOrPut(dayKey) { mutableSetOf() }
                     if (record.firstHourUsed != -1 && record.lastHourUsed != -1) {
                        for (hour in record.firstHourUsed..record.lastHourUsed) {
                            dailyHoursSet.add(hour)
                        }
                    }
                }
                // Consider hours "common" if they appear on at least, e.g., 25% of the days the app was active or a minimum number of times
                val commonHoursThreshold = (distinctDaysForApp * 0.25).toInt().coerceAtLeast(1)
                val commonHours = hourFrequencies.filter { it.value >= commonHoursThreshold }.keys.toSet()


                val dayOfWeekFrequencies = packageRecords.groupBy { it.dayOfWeekUsed }.mapValues { it.value.size }
                val commonDaysThreshold = (distinctDaysForApp * 0.25).toInt().coerceAtLeast(1)
                val commonDays = dayOfWeekFrequencies.filter { it.value >= commonDaysThreshold }.keys.toSet()

                // Use percentiles or mean +/- stddev for ranges for more robust ranges
                val typicalFgTimeRange = calculateRange(dailyForegroundTimes, avgDailyUsageMs)
                val typicalLaunchRange = calculateRange(dailyLaunchCounts.map { it.toLong() }, avgDailyLaunches.toLong()).let {
                    IntRange(it.first.toInt(), it.last.toInt().coerceAtLeast(1))
                }


                appSpecificProfiles.add(
                    AppSpecificProfile(
                        packageName = packageName,
                        typicalTotalForegroundTimePerDayMs = typicalFgTimeRange,
                        typicalLaunchCountPerDay = typicalLaunchRange,
                        commonHoursOfDay = commonHours,
                        commonDaysOfWeek = commonDays
                    )
                )
                Log.d(TAG, "Profiled App $packageName: fgTime=${typicalFgTimeRange}, launches=${typicalLaunchRange}, hours=${commonHours}, days=${commonDays}")
            }

            if (appSpecificProfiles.isEmpty() && distinctDaysOfData >= MIN_DISTINCT_DAYS_FOR_PROFILE) {
                // Still try to create a global profile if enough general data exists
                Log.w(TAG, "No apps met criteria for individual profiling, but attempting global profile.")
            } else if (appSpecificProfiles.isEmpty()) {
                 Log.w(TAG, "No apps met criteria for individual profiling and not enough distinct days of data for global profile either.")
                return@withContext
            }


            // Estimate overall user activity patterns
            val globalHourFrequencies = mutableMapOf<Int, Int>()
            overallUserDailyActivity.values.forEach { dailyHoursSet ->
                dailyHoursSet.forEach { hour ->
                    globalHourFrequencies[hour] = globalHourFrequencies.getOrDefault(hour, 0) + 1
                }
            }
            val globalCommonHoursThreshold = (distinctDaysOfData * 0.3).toInt().coerceAtLeast(1) // e.g., active during this hour on 30% of days
            val typicalDailyActiveHours = globalHourFrequencies.filter { it.value >= globalCommonHoursThreshold }.keys.toSet()


            val dailyTotalUsageMap = allRecords.groupBy {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it.queryStartTime }
                cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
            }.mapValues { entry -> entry.value.sumOf { it.totalTimeInForeground } }
            
            val dailyTotalUsageValues = dailyTotalUsageMap.values.toList()
            val averageDailyTotalUsageMs = if (dailyTotalUsageValues.isNotEmpty()) dailyTotalUsageValues.average().toLong() else 0L
            val typicalDailyTotalUsageRange = calculateRange(dailyTotalUsageValues, averageDailyTotalUsageMs)


            val newProfile = NormalBehaviourProfile(
                profileId = "user_default", // Ensure this matches DAO expectations if fixed
                lastGeneratedTimestamp = System.currentTimeMillis(),
                profiledApps = appSpecificProfiles.sortedByDescending { it.typicalTotalForegroundTimePerDayMs.last },
                allowedInfrequentApps = emptySet(), 
                typicalDailyActiveHours = typicalDailyActiveHours,
                typicalDailyTotalUsageTimeMs = typicalDailyTotalUsageRange
            )

            behaviouralProfileRepository.saveNormalBehaviourProfile(newProfile)
            Log.i(TAG, "Successfully generated and saved new normal behaviour profile with ${newProfile.profiledApps.size} app profiles. Typical active hours: ${newProfile.typicalDailyActiveHours}")
        }
    }
    
    // Helper function to calculate a range (e.g., mean +/- 1 std dev, or percentile based)
    // Basic version: average +/- 50% or ensure a minimum range. More sophisticated: use stddev.
    private fun calculateRange(values: List<Long>, average: Long): LongRange {
        if (values.isEmpty()) return LongRange(0,0)
        
        val stdDev = if (values.size > 1) {
            val mean = values.average()
            val sumOfSquares = values.sumOf { (it - mean) * (it - mean) }
            kotlin.math.sqrt(sumOfSquares / values.size).toLong()
        } else {
            (average * 0.5).toLong() // Fallback for single value or if stddev is preferred
        }

        val lowerBound = (average - stdDev).coerceAtLeast(0)
        val upperBound = (average + stdDev).coerceAtLeast(average) // Ensure upper bound is at least the average
                                            .coerceAtLeast(lowerBound + 1) // Ensure range is not zero or negative

        return LongRange(lowerBound, upperBound.coerceAtLeast(values.minOrNull() ?: 0L).coerceAtMost(values.maxOrNull() ?: Long.MAX_VALUE) )
    }


    fun getNormalProfile(): Flow<NormalBehaviourProfile?> {
        return behaviouralProfileRepository.getNormalBehaviourProfile()
    }

    suspend fun clearProfile() {
        behaviouralProfileRepository.clearNormalBehaviourProfile()
        Log.d(TAG, "Cleared normal behaviour profile.")
    }