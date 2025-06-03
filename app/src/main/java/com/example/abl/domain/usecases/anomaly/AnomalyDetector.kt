package com.example.abl.domain.usecases.anomaly

import android.util.Log
import com.example.abl.data.database.entity.AppUsageRecord // Current usage records
import com.example.abl.domain.model.NormalBehaviourProfile
import javax.inject.Inject
import java.util.Calendar // Added for current hour
import java.util.TimeZone // Added for UTC Calendar
import java.util.concurrent.TimeUnit // Added for expected time calculation

sealed class AnomalyDetectionResult {
    object Normal : AnomalyDetectionResult()
    data class Suspicious(val reasons: List<String>, val deviationScore: Int) : AnomalyDetectionResult()
    data class HighAlert(val reasons: List<String>, val deviationScore: Int) : AnomalyDetectionResult()
}

class AnomalyDetector @Inject constructor() {

    private val TAG = "AnomalyDetector"

    private val UNPROFILED_APP_USAGE_TIME_THRESHOLD_MS: Long = 1 * 60 * 1000
    private val TIME_DEVIATION_FACTOR_WARN = 2.5
    private val TIME_DEVIATION_FACTOR_ALERT = 4.0
    private val LAUNCH_DEVIATION_FACTOR_WARN = 3.0
    private val LAUNCH_DEVIATION_FACTOR_ALERT = 5.0
    private val MIN_EXPECTED_TIME_FOR_DEVIATION_CHECK_MS: Long = 1 * 60 * 1000
    private val MIN_EXPECTED_LAUNCHES_FOR_DEVIATION_CHECK = 0.5

    fun checkForAnomalies(
        currentProfile: NormalBehaviourProfile?,
        currentUsageRecords: List<AppUsageRecord>,
        sampleWindowDurationMs: Long
    ): AnomalyDetectionResult {
        if (currentProfile == null) {
            Log.w(TAG, "No normal behaviour profile available. Cannot perform anomaly detection.")
            return AnomalyDetectionResult.Suspicious(listOf("Profile not available for anomaly check."), 5)
        }

        val anomaliesFound = mutableListOf<String>()
        var totalDeviationScore = 0

        // Get current hour in UTC, as profile hours are stored in UTC
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentGlobalHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Rule 1: Check overall daily active hours
        if (currentProfile.typicalDailyActiveHours.isNotEmpty() && currentGlobalHour !in currentProfile.typicalDailyActiveHours) {
            val reason = "Activity outside typical daily active hours (current UTC: $currentGlobalHour, typical UTC: ${currentProfile.typicalDailyActiveHours})."
            anomaliesFound.add(reason)
            totalDeviationScore += 10 
            Log.d(TAG, reason)
        }
        
        // Aggregate current usage from the sample window
        // RealtimeUsageSampler now provides records with totalTimeInForeground and launchCount specific to the sample window.
        val currentUsageByPackage = currentUsageRecords.associateBy({ it.packageName }) {
            Pair(it.totalTimeInForeground, it.launchCount)
        }

        for ((pkgName, usagePair) in currentUsageByPackage) {
            val currentForegroundTimeInWindow = usagePair.first
            val currentLaunchCountInWindow = usagePair.second

            val appProfile = currentProfile.profiledApps.find { it.packageName == pkgName }

            if (appProfile == null) {
                // Rule 2: Significant usage of an unprofiled or non-allowed infrequent app in the current window
                if (pkgName !in currentProfile.allowedInfrequentApps && currentForegroundTimeInWindow > UNPROFILED_APP_USAGE_TIME_THRESHOLD_MS) {
                    val reason = "Significant usage of unprofiled app '$pkgName' in sample window: ${currentForegroundTimeInWindow / 1000}s."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 15
                    Log.d(TAG, reason)
                }
                continue // Skip further checks for this unprofiled app
            }

            // Rule 3: App used at an uncommon hour for that app
            // currentGlobalHour is already fetched
            if (appProfile.commonHoursOfDay.isNotEmpty() && currentGlobalHour !in appProfile.commonHoursOfDay) {
                 val reason = "App '$pkgName' used at uncommon hour (current UTC: $currentGlobalHour, common UTC for app: ${appProfile.commonHoursOfDay})."
                 anomaliesFound.add(reason)
                 totalDeviationScore += 5
                 Log.d(TAG, reason)
            }

            // Rule 4 & 5: Deviation in foreground time and launch count for the sample window
            // The profile stores typical *daily* usage. We need to scale this down to the sample window.
            // Profiled ranges are typical daily ranges (LongRange for time, IntRange for launches)
            val avgProfileDailyTime = (appProfile.typicalTotalForegroundTimePerDayMs.first + appProfile.typicalTotalForegroundTimePerDayMs.last) / 2
            val avgProfileDailyLaunches = (appProfile.typicalLaunchCountPerDay.first + appProfile.typicalLaunchCountPerDay.last) / 2.0

            // Expected values in the current sample window
            val expectedTimeInWindow = (avgProfileDailyTime.toDouble() / TimeUnit.DAYS.toMillis(1) * sampleWindowDurationMs).toLong()
            val expectedLaunchesInWindow = avgProfileDailyLaunches / (TimeUnit.DAYS.toMillis(1).toDouble() / sampleWindowDurationMs)
            
            Log.d(TAG, "App '$pkgName': CurrentTime=${currentForegroundTimeInWindow}ms, ExpectedTime=${expectedTimeInWindow}ms. CurrentLaunches=${currentLaunchCountInWindow}, ExpectedLaunches=${String.format("%.2f", expectedLaunchesInWindow)}")

            // Foreground Time Deviation Check
            if (expectedTimeInWindow >= MIN_EXPECTED_TIME_FOR_DEVIATION_CHECK_MS) { 
                 if (currentForegroundTimeInWindow > expectedTimeInWindow * TIME_DEVIATION_FACTOR_ALERT) {
                    val reason = "High foreground time deviation for '$pkgName': current ${currentForegroundTimeInWindow/1000}s, expected ~${expectedTimeInWindow/1000}s in window (alert)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 20
                    Log.d(TAG, reason)
                } else if (currentForegroundTimeInWindow > expectedTimeInWindow * TIME_DEVIATION_FACTOR_WARN) {
                    val reason = "Foreground time deviation for '$pkgName': current ${currentForegroundTimeInWindow/1000}s, expected ~${expectedTimeInWindow/1000}s in window (warn)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 10
                    Log.d(TAG, reason)
                }
            } else if (currentForegroundTimeInWindow > (MIN_EXPECTED_TIME_FOR_DEVIATION_CHECK_MS * TIME_DEVIATION_FACTOR_WARN) && currentForegroundTimeInWindow > avgProfileDailyTime) {
                 // If expected window time is very low, but actual usage is already high (e.g. > daily avg and > a fixed threshold)
                 val reason = "Unexpectedly high foreground time for '$pkgName' (${currentForegroundTimeInWindow/1000}s) given low expected window activity."
                 anomaliesFound.add(reason)
                 totalDeviationScore += 10
                 Log.d(TAG, reason)
            }

            // Launch Count Deviation Check
            if (expectedLaunchesInWindow >= MIN_EXPECTED_LAUNCHES_FOR_DEVIATION_CHECK) { 
                if (currentLaunchCountInWindow > expectedLaunchesInWindow * LAUNCH_DEVIATION_FACTOR_ALERT) {
                    val reason = "High launch count deviation for '$pkgName': current $currentLaunchCountInWindow, expected ~${String.format("%.2f", expectedLaunchesInWindow)} in window (alert)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 15
                    Log.d(TAG, reason)
                } else if (currentLaunchCountInWindow > expectedLaunchesInWindow * LAUNCH_DEVIATION_FACTOR_WARN) {
                    val reason = "Launch count deviation for '$pkgName': current $currentLaunchCountInWindow, expected ~${String.format("%.2f", expectedLaunchesInWindow)} in window (warn)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 7
                    Log.d(TAG, reason)
                }
            } else if (currentLaunchCountInWindow > (MIN_EXPECTED_LAUNCHES_FOR_DEVIATION_CHECK * LAUNCH_DEVIATION_FACTOR_WARN) && currentLaunchCountInWindow > avgProfileDailyLaunches) {
                 // If expected window launches are very low, but actual launches are already high
                 val reason = "Unexpectedly high launch count for '$pkgName' ($currentLaunchCountInWindow) given low expected window activity."
                 anomaliesFound.add(reason)
                 totalDeviationScore += 7
                 Log.d(TAG, reason)
            }
        }

        return when {
            totalDeviationScore >= 30 -> AnomalyDetectionResult.HighAlert(anomaliesFound.distinct(), totalDeviationScore) 
            totalDeviationScore > 0 -> AnomalyDetectionResult.Suspicious(anomaliesFound.distinct(), totalDeviationScore) 
            else -> AnomalyDetectionResult.Normal
        }
    }
} 