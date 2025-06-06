package com.example.abl.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.collector.RealtimeUsageSampler
import com.example.abl.data.collector.UsageStatsAutoencoder
import com.example.abl.data.collector.UsageStatsCollector
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord
import com.example.abl.domain.usecases.anomaly.AnomalyDetectionResult
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.abl.data.worker.TrainingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.*

@HiltViewModel
class BehaviouralProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context, 
    private val usageStatsCollector: UsageStatsCollector,
    private val appUsageRecordDao: AppUsageRecordDao, 
    private val realtimeUsageSampler: RealtimeUsageSampler,
    private val usageStatsAutoencoder: UsageStatsAutoencoder
) : ViewModel() {

    private val TAG = "BehaviouralProfileVM"
    private val REALTIME_SAMPLING_INTERVAL_MS = 1 * 60 * 1000L 
    private val SAMPLER_WINDOW_DURATION_MS = 15 * 60 * 1000L 
    private val HISTORICAL_DATA_COLLECTION_DAYS = 7

    private val _collectedUsageRecords = MutableStateFlow<List<AppUsageRecord>>(emptyList())
    val collectedUsageRecords: StateFlow<List<AppUsageRecord>> = _collectedUsageRecords.asStateFlow()

    private val _anomalyDetectionStatus = MutableStateFlow<AnomalyDetectionResult>(AnomalyDetectionResult.Normal)
    val anomalyDetectionStatus: StateFlow<AnomalyDetectionResult> = _anomalyDetectionStatus.asStateFlow()

    private var monitoringJob: Job? = null

    init {
        Log.d(TAG, "BehaviouralProfileViewModel initialized.")
        loadCollectedAppUsageRecordsForDisplay()
        startRealtimeMonitoring()
    }

    private fun collectHistoricalDataAndTrainModel() {
        // This function is now only for the manual trigger, which uses a worker.
        // The implementation is in triggerHistoricalDataCollectionAndProfileUpdate().
        // We can leave this empty or remove it if no longer referenced elsewhere.
    }

    private fun loadCollectedAppUsageRecordsForDisplay() {
        viewModelScope.launch {
            Log.d(TAG, "Attempting to load collected app usage records for display from DAO for the last $HISTORICAL_DATA_COLLECTION_DAYS days.")
            val endTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = endTime
            calendar.add(Calendar.DAY_OF_YEAR, -(HISTORICAL_DATA_COLLECTION_DAYS - 1))
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            try {
                appUsageRecordDao.getRecordsByQueryDateRange(rangeStart = startTime, rangeEnd = endTime)
                    .collect { records ->
                        _collectedUsageRecords.value = records.sortedByDescending { it.queryStartTime + it.lastHourUsed } // Sort for display
                        Log.d(TAG, "Successfully loaded ${records.size} AppUsageRecord items (by query date) for display.")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading records from AppUsageRecordDao for display (by query date)", e)
                _collectedUsageRecords.value = emptyList()
            }
        }
    }

    fun startRealtimeMonitoring() {
        if (monitoringJob?.isActive == true) {
            Log.d(TAG, "Realtime monitoring is already active.")
            return
        }
        monitoringJob = viewModelScope.launch {
            Log.d(TAG, "Starting realtime anomaly monitoring loop.")
            while (isActive) {
                try {
                    val currentUsageRecords = realtimeUsageSampler.sampleCurrentUsage(SAMPLER_WINDOW_DURATION_MS)
                    Log.d(TAG, "Sampled ${currentUsageRecords.size} records for current usage.")

                    if (currentUsageRecords.isNotEmpty()){
                        val anomalyResponse = usageStatsAutoencoder.detectAnomalies(currentUsageRecords)
                        if (anomalyResponse != null && anomalyResponse.success) {
                            val anomalousResults = anomalyResponse.results.filter { it.isAnomaly }
                            val overallRisk = anomalyResponse.overallRiskLevel.uppercase()
                            
                            if (anomalousResults.isNotEmpty()) {
                                val reasons = anomalousResults.map { "App: ${it.app}, Risk: ${it.riskLevel}, Score: %.2f".format(it.anomalyScore) }
                                val score = when(overallRisk) {
                                    "CRITICAL" -> 100
                                    "HIGH" -> 75
                                    "MEDIUM" -> 50
                                    else -> 25 // Corresponds to LOW
                                }

                                when (overallRisk) {
                                    "CRITICAL", "HIGH" -> {
                                        _anomalyDetectionStatus.value = AnomalyDetectionResult.HighAlert(reasons, score)
                                    }
                                    "MEDIUM" -> {
                                        _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(reasons, score)
                                    }
                                    else -> { // "LOW"
                                        // Even if an app has an anomaly score above threshold, if the overall risk is low,
                                        // we can treat it as normal for the UI to avoid over-alarming the user.
                                        _anomalyDetectionStatus.value = AnomalyDetectionResult.Normal
                                    }
                                }
                                Log.w(TAG, "Anomaly detected with overall risk: $overallRisk. Details: ${reasons.joinToString()}")
                            } else {
                                _anomalyDetectionStatus.value = AnomalyDetectionResult.Normal
                                Log.i(TAG, "No anomalies detected in current usage. Overall risk: ${anomalyResponse.overallRiskLevel}")
                            }
                        } else {
                            Log.e(TAG, "Anomaly detection API call failed or returned no success.")
                            _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Failed to get anomaly status from API"), 50)
                        }

                    } else {
                        if(_anomalyDetectionStatus.value !is AnomalyDetectionResult.Normal) {
                           _anomalyDetectionStatus.value = AnomalyDetectionResult.Normal
                           Log.d(TAG, "No recent usage detected in the sample window. Status set to Normal.")
                        }
                    }

                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission error during realtime sampling. Ensure PACKAGE_USAGE_STATS.", e)
                    _anomalyDetectionStatus.value = AnomalyDetectionResult.HighAlert(listOf("Realtime monitoring permission error"), 100)
                    delay(REALTIME_SAMPLING_INTERVAL_MS * 5)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in realtime monitoring loop: ${e.localizedMessage}", e)
                    _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Realtime monitoring error: ${e.message}"), 50)
                     delay(REALTIME_SAMPLING_INTERVAL_MS)
                }
                delay(REALTIME_SAMPLING_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Realtime monitoring job initiated.")
    }

    fun stopRealtimeMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "Realtime monitoring stopped.")
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeMonitoring()
        Log.d(TAG, "BehaviouralProfileViewModel cleared.")
    }

    fun triggerHistoricalDataCollectionAndProfileUpdate() {
        Log.d(TAG, "Manual trigger for training work requested.")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val trainingRequest = OneTimeWorkRequestBuilder<TrainingWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ManualTrainingWork",
            ExistingWorkPolicy.REPLACE, // Use REPLACE to start a new one if a manual one was already pending
            trainingRequest
        )
        Log.d(TAG, "Manual training work enqueued.")
    }
}