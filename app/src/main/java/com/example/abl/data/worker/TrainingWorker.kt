package com.example.abl.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.abl.data.collector.UsageStatsAutoencoder
import com.example.abl.data.collector.UsageStatsCollector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TrainingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageStatsCollector: UsageStatsCollector,
    private val usageStatsAutoencoder: UsageStatsAutoencoder
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "TrainingWorker"
    private val HISTORICAL_DATA_COLLECTION_DAYS = 7

    override suspend fun doWork(): Result {
        Log.d(TAG, "Training worker started: Collecting data and training model.")

        return try {
            usageStatsCollector.collectAndStoreUsageDataForPastDays(HISTORICAL_DATA_COLLECTION_DAYS)
            Log.d(TAG, "Worker: Historical data collection complete.")

            usageStatsAutoencoder.sendDataToTrainModel()
            Log.d(TAG, "Worker: Autoencoder model training request sent.")

            Result.success()
        } catch (e: SecurityException) {
            Log.e(TAG, "Worker failed due to permissions. This requires user action.", e)
            // Retrying won't fix a permissions issue.
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed, will retry.", e)
            // Could be a temporary network issue, so we should retry.
            Result.retry()
        }
    }
}