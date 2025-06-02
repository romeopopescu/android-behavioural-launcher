package com.example.abl.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.collector.RealtimeUsageSampler
import com.example.abl.data.collector.UsageStatsCollector
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord
import com.example.abl.domain.usecases.anomaly.AnomalyDetectionResult
import com.example.abl.domain.usecases.anomaly.AnomalyDetector
import com.example.abl.domain.usecases.profile.BehaviouralProfileManager
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

@HiltViewModel
class BehaviouralProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context, 
    private val usageStatsCollector: UsageStatsCollector,
    private val appUsageRecordDao: AppUsageRecordDao, 
    private val behaviouralProfileManager: BehaviouralProfileManager,
    private val realtimeUsageSampler: RealtimeUsageSampler,
    private val anomalyDetector: AnomalyDetector
) : ViewModel() {

    private val TAG = "BehaviouralProfileVM"
    private val REALTIME_SAMPLING_INTERVAL_MS = 1 * 60 * 1000L 
    private val SAMPLER_WINDOW_DURATION_MS = 15 * 60 * 1000L 
    private val HISTORICAL_DATA_COLLECTION_DAYS = 7 // Collect for the past 7 days

    private val _collectedUsageRecords = MutableStateFlow<List<AppUsageRecord>>(emptyList()) 
    val collectedUsageRecords: StateFlow<List<AppUsageRecord>> = _collectedUsageRecords.asStateFlow()

    private val _anomalyDetectionStatus = MutableStateFlow<AnomalyDetectionResult>(AnomalyDetectionResult.Normal)
    val anomalyDetectionStatus: StateFlow<AnomalyDetectionResult> = _anomalyDetectionStatus.asStateFlow()

    private var monitoringJob: Job? = null

    init {
        Log.d(TAG, "BehaviouralProfileViewModel initialized.")
        collectHistoricalDataAndGenerateProfile() 
        startRealtimeMonitoring()
    }

    private fun collectHistoricalDataAndGenerateProfile() {
        viewModelScope.launch {
            Log.d(TAG, "Starting historical AppUsageRecord collection for $HISTORICAL_DATA_COLLECTION_DAYS days and profile generation...")
            try {
                // Correctly calling the new method in UsageStatsCollector
                usageStatsCollector.collectAndStoreUsageDataForPastDays(HISTORICAL_DATA_COLLECTION_DAYS)
                Log.d(TAG, "Historical AppUsageRecord collection attempt complete for $HISTORICAL_DATA_COLLECTION_DAYS days.")
                
                behaviouralProfileManager.generateOrUpdateNormalProfile()
                Log.d(TAG, "Behaviour profile generation/update attempted.")

                // Optionally, load some records for display (e.g., last HISTORICAL_DATA_COLLECTION_DAYS days)
                loadCollectedAppUsageRecordsForDisplay()

            } catch (e: SecurityException) {
                Log.e(TAG, "Permission error during data collection. Ensure PACKAGE_USAGE_STATS is granted.", e)
                _collectedUsageRecords.value = emptyList() 
                _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Data collection permission denied"), 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error during historical collection or profile generation: ${e.localizedMessage}", e)
                _collectedUsageRecords.value = emptyList()
                 // Potentially set anomaly status to indicate an error with profile generation
                _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Profile generation error: ${e.localizedMessage}"), 0)
            }
        }
    }

    private fun loadCollectedAppUsageRecordsForDisplay() {
        viewModelScope.launch {
            Log.d(TAG, "Attempting to load collected app usage records for display from DAO for the last $HISTORICAL_DATA_COLLECTION_DAYS days.")
            val startTime = System.currentTimeMillis() - (HISTORICAL_DATA_COLLECTION_DAYS * 24 * 60 * 60 * 1000L)
            val endTime = System.currentTimeMillis()
            try {
                appUsageRecordDao.getAllRecords(startTime = startTime, endTime = endTime)
                    .collect { records ->
                        _collectedUsageRecords.value = records
                        Log.d(TAG, "Successfully loaded ${records.size} AppUsageRecord items for display.")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading records from AppUsageRecordDao for display", e)
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
                    val currentProfile = behaviouralProfileManager.getNormalProfile().firstOrNull()
                    if (currentProfile == null) {
                        Log.w(TAG, "No profile loaded yet, skipping anomaly check.")
                        if (_anomalyDetectionStatus.value !is AnomalyDetectionResult.Suspicious) { // Avoid constant logging if already suspicious
                           _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Profile not yet generated"), 0) 
                        }
                        delay(REALTIME_SAMPLING_INTERVAL_MS * 2) // Wait a bit longer if profile isn't ready
                        continue
                    }

                    val currentUsage = realtimeUsageSampler.sampleCurrentUsage(SAMPLER_WINDOW_DURATION_MS)
                    Log.d(TAG, "Sampled ${currentUsage.size} records for current usage.")

                    if (currentUsage.isNotEmpty()){ 
                        val result = anomalyDetector.checkForAnomalies(currentProfile, currentUsage, SAMPLER_WINDOW_DURATION_MS)
                        _anomalyDetectionStatus.value = result
                        // Log only if status changes or is not normal to reduce log spam
                        if (result !is AnomalyDetectionResult.Normal) {
                           Log.d(TAG, "Anomaly detection result: $result")
                        }
                    } else {
                        // If no recent usage, and status is not already normal, set to normal.
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
                     delay(REALTIME_SAMPLING_INTERVAL_MS) // Ensure delay even on other exceptions before retrying
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

    // Public method to allow manual refresh if needed from UI
    fun triggerHistoricalDataCollectionAndProfileUpdate() {
        Log.d(TAG, "Manual trigger for historical data collection and profile update.")
        collectHistoricalDataAndGenerateProfile()
    }
}