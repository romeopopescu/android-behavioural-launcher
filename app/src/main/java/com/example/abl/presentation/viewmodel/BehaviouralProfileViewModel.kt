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
import com.example.abl.data.preferences.ProfileStatusManager
import com.example.abl.data.worker.TrainingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    private val usageStatsAutoencoder: UsageStatsAutoencoder,
    private val profileStatusManager: ProfileStatusManager
) : ViewModel() {

    private val TAG = "BehaviouralProfileVM"
    private val REALTIME_SAMPLING_INTERVAL_MS = 1 * 60 * 1000L
    private val SAMPLER_WINDOW_DURATION_MS = 15 * 60 * 1000L
    private val HISTORICAL_DATA_COLLECTION_DAYS = 7

    private val _collectedUsageRecords = MutableStateFlow<List<AppUsageRecord>>(emptyList())
    val collectedUsageRecords: StateFlow<List<AppUsageRecord>> = _collectedUsageRecords.asStateFlow()

    private val _anomalyDetectionStatus = MutableStateFlow(AnomalyUiState())
    val anomalyDetectionStatus: StateFlow<AnomalyUiState> = _anomalyDetectionStatus.asStateFlow()


    private var monitoringJob: Job? = null

    init {
        Log.d(TAG, "BehaviouralProfileViewModel initialized.")
        loadCollectedAppUsageRecordsForDisplay()
//        collectHistoricalDataAndTrainModel()
//        checkAndTriggerInitialTraining()
//        startRealtimeMonitoring()
    }
    fun onAppLaunch() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!profileStatusManager.isInitialTrainingDone()) {
                Log.i(TAG, "First launch: Collecting history and training model.")
                usageStatsCollector.collectAndStoreUsageDataForPastDays(7)
                usageStatsAutoencoder.sendDataToTrainModel()
                profileStatusManager.setInitialTrainingDone()
                Log.i(TAG, "Initial training complete.")
            }
            startRealtimeMonitoring()
        }
    }

    private fun startRealtimeMonitoring() {
        if (monitoringJob?.isActive == true) return
        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting real-time anomaly monitoring loop.")
            while (isActive) {
                try {
                    val currentUsage = realtimeUsageSampler.sampleCurrentUsage()
                    if (currentUsage.isNotEmpty()) {

                        val response = usageStatsAutoencoder.detectAnomalies(currentUsage)
                        Log.d(TAG, "Real-time detection result: $response")

                        if (response != null && response.success) {
                            _anomalyDetectionStatus.value = AnomalyUiState(
                                riskLevel = response.overallRiskLevel,
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in loop", e)
                }
                delay(60 * 1000L)
            }
        }
    }

    private fun checkAndTriggerInitialTraining() {
        viewModelScope.launch {
            if (!profileStatusManager.isInitialTrainingDone()) {
                Log.i(TAG, "Initial training not completed. Triggering...")

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val trainingRequest = OneTimeWorkRequestBuilder<TrainingWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "InitialTrainingWork",
                    ExistingWorkPolicy.KEEP,
                    trainingRequest
                )

                profileStatusManager.setInitialTrainingDone()
                Log.i(TAG, "Initial training DONE.")
            } else {
                Log.i(TAG, "Initial training already completed. Skipping .....")
            }
        }
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
//                _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Data collection permission denied"), 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error during historical collection or model training: ${e.localizedMessage}", e)
                _collectedUsageRecords.value = emptyList()
//                _anomalyDetectionStatus.value = AnomalyDetectionResult.Suspicious(listOf("Profile/model training error: ${e.localizedMessage}"), 0)
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

}
data class AnomalyUiState(
    val riskLevel: String = "PENDING",
)