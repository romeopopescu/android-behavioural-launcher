package com.example.abl.domain.usecases.anomaly // Or com.example.abl.domain.detection

import android.util.Log
import com.example.abl.data.database.entity.AppUsageRecord // Current usage records
import com.example.abl.domain.model.NormalBehaviourProfile
import javax.inject.Inject

// Represents the outcome of an anomaly check
sealed class AnomalyDetectionResult {
    object Normal : AnomalyDetectionResult()
    data class Suspicious(val reasons: List<String>, val deviationScore: Int) : AnomalyDetectionResult()
    data class HighAlert(val reasons: List<String>, val deviationScore: Int) : AnomalyDetectionResult()
}

class AnomalyDetector @Inject constructor() {

    private val TAG = "AnomalyDetector"

    // Thresholds - these would need careful tuning
    private val UNPROFILED_APP_USAGE_TIME_THRESHOLD_MS: Long = 5 * 60 * 1000 // 5 mins for an unprofiled app
    private val TIME_DEVIATION_FACTOR_WARN = 2.5 // e.g. 2.5x normal usage
    private val TIME_DEVIATION_FACTOR_ALERT = 4.0 // e.g. 4x normal usage
    private val LAUNCH_DEVIATION_FACTOR_WARN = 3.0
    private val LAUNCH_DEVIATION_FACTOR_ALERT = 5.0

    fun checkForAnomalies(
        currentProfile: NormalBehaviourProfile?,
        currentUsageRecords: List<AppUsageRecord>, // From RealtimeUsageSampler
        sampleWindowDurationMs: Long
    ): AnomalyDetectionResult {
        if (currentProfile == null) {
            Log.w(TAG, "No normal behaviour profile available. Cannot perform anomaly detection.")
            return AnomalyDetectionResult.Normal // Or a specific "ProfileNotReady" state
        }

        val anomaliesFound = mutableListOf<String>()
        var totalDeviationScore = 0

        // Rule 1: Check overall daily active hours (if current time falls outside)
        val currentHour = if (currentUsageRecords.isNotEmpty()) {
            currentUsageRecords.first().hourOfDayUsed // Assuming records are somewhat recent and consistent for hour
        } else {
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) // Fallback to system current hour
        }

        if (currentHour !in currentProfile.typicalDailyActiveHours) {
            val reason = "Activity outside typical daily active hours (current: $currentHour, typical: ${currentProfile.typicalDailyActiveHours})."
            anomaliesFound.add(reason)
            totalDeviationScore += 10 // Assign some score
            Log.d(TAG, reason)
        }
        
        val currentUsageByPackage = currentUsageRecords.groupBy { it.packageName }
            .mapValues { entry ->
                val records = entry.value
                Pair(
                    records.sumOf { it.totalTimeInForeground }, // Note: This is approximation from RealtimeUsageSampler
                    records.sumOf { it.launchCount } // Approximation
                )
            }


        for ((pkgName, usagePair) in currentUsageByPackage) {
            val currentForegroundTime = usagePair.first
            val currentLaunchCount = usagePair.second

            val appProfile = currentProfile.profiledApps.find { it.packageName == pkgName }

            if (appProfile == null) {
                // Rule 2: Significant usage of an unprofiled or non-allowed infrequent app
                if (pkgName !in currentProfile.allowedInfrequentApps && currentForegroundTime > UNPROFILED_APP_USAGE_TIME_THRESHOLD_MS) {
                    val reason = "Significant usage of unprofiled app '$pkgName': ${currentForegroundTime / 1000}s."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 15
                    Log.d(TAG, reason)
                }
                continue // Skip further checks for this unprofiled app
            }

            val profileTimeAvgPerDay = appProfile.typicalTotalForegroundTimePerDayMs.let { (it.first + it.last) / 2 }
            val profileLaunchesAvgPerDay = appProfile.typicalLaunchCountPerDay.let { (it.first + it.last) / 2 }
            
            val expectedTimeInWindow = (profileTimeAvgPerDay.toDouble() / (24 * 60 * 60 * 1000) * sampleWindowDurationMs).toLong()
            val expectedLaunchesInWindow = (profileLaunchesAvgPerDay.toDouble() / (24 * 60 * 60 * 1000) * sampleWindowDurationMs)

            if (currentHour !in appProfile.commonHoursOfDay) {
                 val reason = "App '$pkgName' used at uncommon hour (current: $currentHour, common: ${appProfile.commonHoursOfDay})."
                 anomaliesFound.add(reason)
                 totalDeviationScore += 5
                 Log.d(TAG, reason)
            }

            if (expectedTimeInWindow > 0) { 
                 if (currentForegroundTime > expectedTimeInWindow * TIME_DEVIATION_FACTOR_ALERT) {
                    val reason = "High foreground time deviation for '$pkgName': current ${currentForegroundTime/1000}s, expected ~${expectedTimeInWindow/1000}s (alert)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 20
                    Log.d(TAG, reason)
                } else if (currentForegroundTime > expectedTimeInWindow * TIME_DEVIATION_FACTOR_WARN) {
                    val reason = "Foreground time deviation for '$pkgName': current ${currentForegroundTime/1000}s, expected ~${expectedTimeInWindow/1000}s (warn)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 10
                    Log.d(TAG, reason)
                }
            }

             if (expectedLaunchesInWindow > 0.5) { 
                if (currentLaunchCount > expectedLaunchesInWindow * LAUNCH_DEVIATION_FACTOR_ALERT) {
                    val reason = "High launch count deviation for '$pkgName': current $currentLaunchCount, expected ~$expectedLaunchesInWindow (alert)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 15
                    Log.d(TAG, reason)
                } else if (currentLaunchCount > expectedLaunchesInWindow * LAUNCH_DEVIATION_FACTOR_WARN) {
                    val reason = "Launch count deviation for '$pkgName': current $currentLaunchCount, expected ~$expectedLaunchesInWindow (warn)."
                    anomaliesFound.add(reason)
                    totalDeviationScore += 7
                    Log.d(TAG, reason)
                }
            }
        }

        return when {
            totalDeviationScore >= 30 -> AnomalyDetectionResult.HighAlert(anomaliesFound, totalDeviationScore) 
            totalDeviationScore >= 10 -> AnomalyDetectionResult.Suspicious(anomaliesFound, totalDeviationScore) 
            else -> AnomalyDetectionResult.Normal
        }
    }
} 