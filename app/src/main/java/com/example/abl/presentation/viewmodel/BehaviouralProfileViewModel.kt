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
import java.util.*

@HiltViewModel
class BehaviouralProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context, 
    private val usageStatsCollector: UsageStatsCollector,
    private val appUsageRecordDao: AppUsageRecordDao, 
    private val behaviouralProfileManager: BehaviouralProfileManager,
    private val realtimeUsageSampler: RealtimeUsageSampler,
    private val anomalyDetector: AnomalyDetector,
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
        collectHistoricalDataAndTrainModel()
    }

    private fun collectHistoricalDataAndTrainModel() {
        viewModelScope.launch {
            Log.d(TAG, "Starting historical AppUsageRecord collection for $HISTORICAL_DATA_COLLECTION_DAYS days and autoencoder model training...")
            try {
                usageStatsCollector.collectAndStoreUsageDataForPastDays(HISTORICAL_DATA_COLLECTION_DAYS)
                Log.d(TAG, "Historical AppUsageRecord collection attempt complete for $HISTORICAL_DATA_COLLECTION_DAYS days.")
                
                usageStatsAutoencoder.sendDataToTrainModel()
                Log.d(TAG, "Autoencoder model training request sent.")

                loadCollectedAppUsageRecordsForDisplay()

            } catch (e: SecurityException) {
                Log.e(TAG, "Permission error during data collection. Ensure PACKAGE_USAGE_STATS is granted.", e)
                _collectedUsageRecords.value = emptyList() 
                _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Data collection permission denied"), 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error during historical collection or model training: ${e.localizedMessage}", e)
                _collectedUsageRecords.value = emptyList()
                _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Profile/model training error: ${e.localizedMessage}"), 0)
            }
        }
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
                        Log.w(TAG, "Realtime anomaly detection with API not yet implemented.")
                         _anomalyDetectionStatus.value = AnomalyDetectionResult.Normal

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
        Log.d(TAG, "Manual trigger for historical data collection and model training request.")
        collectHistoricalDataAndTrainModel()
    }
}