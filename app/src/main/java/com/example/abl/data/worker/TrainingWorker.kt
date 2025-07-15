package com.example.abl.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord // Make sure this is imported
import com.example.abl.data.network.ApiService
import com.example.abl.data.network.AppUsageDataApi
import com.example.abl.data.network.TrainRequestBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class TrainingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appUsageRecordDao: AppUsageRecordDao,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {

    private val TAG = "TrainingWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Training worker starting.")
            val historicalRecords = appUsageRecordDao.getAllAppsUsageForApi() // Assumes you added this to your DAO

            if (historicalRecords.isEmpty()) {
                Log.w(TAG, "No historical data to train model.")
                return Result.success()
            }

            val apiData = transformToAppUsageDataApi(historicalRecords)

            if (apiData.isEmpty()) {
                Log.w(TAG, "No transformed usage data to send.")
                return Result.success()
            }

            val requestBody = TrainRequestBody(
                    usageData = apiData,
                    epochs = 30,
                    validationSplit = 0.2f
                )
            val response = apiService.trainModel(requestBody)

            if (response.isSuccessful) {
                Log.i(TAG, "Successfully sent ${apiData.size} records for training.")
            } else {
                Log.e(TAG, "API call for training failed: ${response.code()} ${response.message()}")
                return Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Training worker failed with an exception.", e)
            Result.retry()
        }
    }

    private fun transformToAppUsageDataApi(records: List<AppUsageRecord>): List<AppUsageDataApi> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return records.mapNotNull { record ->

            if (record.firstHourUsed == -1 && record.lastHourUsed == -1 && record.launchCount == 0 && record.totalTimeInForeground == 0L) {
                return@mapNotNull null
            }

            AppUsageDataApi(
                app = record.packageName,
                date = dateFormat.format(Date(record.queryStartTime)),
                firstHour = record.firstHourUsed,
                lastHour = record.lastHourUsed,
                launchCount = record.launchCount,
                totalTimeInForeground = record.totalTimeInForeground / 1000
            )
        }
    }
}