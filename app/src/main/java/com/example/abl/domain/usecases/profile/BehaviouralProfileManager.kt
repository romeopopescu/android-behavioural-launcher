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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviouralProfileManager @Inject constructor(
    private val appUsageRecordDao: AppUsageRecordDao,
    private val behaviouralProfileRepository: BehaviouralProfileRepository
) {

    private val TAG = "BehaviouralProfileMgr"
    private val MIN_RECORDS_FOR_PROFILE = 50 // Minimum total records to attempt profiling
    private val MIN_DAYS_FOR_PROFILE = 3     // Minimum distinct days of data needed
    private val APP_PROFILE_MIN_USAGE_TIME_MS = TimeUnit.MINUTES.toMillis(5) // Min total usage for an app to be profiled
    private val APP_PROFILE_MIN_LAUNCHES = 3 // Min total launches for an app to be profiled


    suspend fun generateOrUpdateNormalProfile(forceRegeneration: Boolean = false) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Attempting to generate/update normal behaviour profile.")

            if (!forceRegeneration) {
                val existingProfile = behaviouralProfileRepository.getNormalBehaviourProfile().firstOrNull()
                if (existingProfile != null) {
                    // Potentially add logic here to check if profile is recent enough
                    // For now, if it exists and not forcing, we assume it's good.
                    Log.d(TAG, "Existing profile found and not forcing regeneration.")
                    // return // Uncomment if you want to avoid regeneration if profile exists
                }
            }

            // Fetch all historical data (consider a time window for very long-term data)
            // For simplicity, using a large window. Adjust as needed.
            val allRecords = appUsageRecordDao.getAllRecords(
                startTime = 0, // From the beginning of time
                endTime = System.currentTimeMillis()
            ).firstOrNull() ?: emptyList()

            if (allRecords.size < MIN_RECORDS_FOR_PROFILE) {
                Log.w(TAG, "Not enough records to generate a reliable profile (found ${allRecords.size}, need $MIN_RECORDS_FOR_PROFILE).")
                return@withContext
            }

            val distinctDays = allRecords.map { record ->
                val cal = Calendar.getInstance().apply { timeInMillis = record.recordedAt }
                cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
            }.distinct().size

            if (distinctDays < MIN_DAYS_FOR_PROFILE) {
                Log.w(TAG, "Not enough distinct days of data (found $distinctDays, need $MIN_DAYS_FOR_PROFILE).")
                return@withContext
            }

            val appSpecificProfiles = mutableListOf<AppSpecificProfile>()
            val recordsByPackage = allRecords.groupBy { it.packageName }

            for ((packageName, records) in recordsByPackage) {
                val totalUsageTime = records.sumOf { it.totalTimeInForeground }
                val totalLaunches = records.sumOf { it.launchCount } // Using the placeholder launch count

                if (totalUsageTime < APP_PROFILE_MIN_USAGE_TIME_MS && totalLaunches < APP_PROFILE_MIN_LAUNCHES) {
                    continue // Skip apps with very little usage
                }

                // Calculate average daily usage and launches
                // This is a simplified average; a more robust method would normalize by actual days of use for that app.
                val avgDailyUsageMs = if (distinctDays > 0) totalUsageTime / distinctDays else totalUsageTime
                val avgDailyLaunches = if (distinctDays > 0) totalLaunches / distinctDays else totalLaunches

                val commonHours = records.groupBy { it.hourOfDayUsed }.keys // Could add frequency threshold
                val commonDays = records.groupBy { it.dayOfWeekUsed }.keys   // Could add frequency threshold

                appSpecificProfiles.add(
                    AppSpecificProfile(
                        packageName = packageName,
                        // Define ranges based on averages - this is a very basic approach
                        // A more sophisticated method would use standard deviations or percentiles
                        typicalTotalForegroundTimePerDayMs = LongRange(avgDailyUsageMs / 2, avgDailyUsageMs * 2),
                        typicalLaunchCountPerDay = IntRange( (avgDailyLaunches / 2).toInt(), (avgDailyLaunches * 2).toInt().coerceAtLeast(1) ),
                        commonHoursOfDay = commonHours,
                        commonDaysOfWeek = commonDays
                    )
                )
            }

            if (appSpecificProfiles.isEmpty()) {
                Log.w(TAG, "No apps met the criteria for individual profiling.")
                 // Potentially still create a profile with only global metrics or abort
            }

            // Estimate overall user activity patterns (simplified)
            val typicalDailyActiveHours = allRecords.map { it.hourOfDayUsed }.distinct().toSet()
            val dailyTotalUsageMap = allRecords.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.recordedAt }
                cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
            }.mapValues { entry -> entry.value.sumOf { it.totalTimeInForeground } }
            
            val averageDailyTotalUsageMs = if (dailyTotalUsageMap.isNotEmpty()) dailyTotalUsageMap.values.average().toLong() else 0L

            val newProfile = NormalBehaviourProfile(
                lastGeneratedTimestamp = System.currentTimeMillis(),
                profiledApps = appSpecificProfiles.sortedByDescending { it.typicalTotalForegroundTimePerDayMs.first }, // Sort by most used
                allowedInfrequentApps = emptySet(), // To be populated by a different mechanism or manually
                typicalDailyActiveHours = typicalDailyActiveHours,
                typicalDailyTotalUsageTimeMs = LongRange(averageDailyTotalUsageMs / 2, averageDailyTotalUsageMs * 2)
            )

            behaviouralProfileRepository.saveNormalBehaviourProfile(newProfile)
            Log.d(TAG, "Successfully generated and saved new normal behaviour profile with ${newProfile.profiledApps.size} app profiles.")
        }
    }

    fun getNormalProfile(): Flow<NormalBehaviourProfile?> {
        return behaviouralProfileRepository.getNormalBehaviourProfile()
    }

    suspend fun clearProfile() {
        behaviouralProfileRepository.clearNormalBehaviourProfile()
        Log.d(TAG, "Cleared normal behaviour profile.")
    }
} 