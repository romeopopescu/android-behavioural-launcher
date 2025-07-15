package com.example.abl.data.collector

import android.content.Context
import android.util.Log
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord
import com.example.abl.data.network.ApiService
import com.example.abl.data.network.AnomalyDetectionRequest
import com.example.abl.data.network.AnomalyDetectionResponse
import com.example.abl.data.network.AppUsageDataApi
import com.example.abl.data.network.TrainRequestBody
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class UsageStatsAutoencoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageRecordDao: AppUsageRecordDao,
    private val apiService: ApiService
) {
    private val TAG = "UsageStatsAutoencoder"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun transformToAppUsageDataApi(records: List<AppUsageRecord>): List<AppUsageDataApi> {
        return records.mapNotNull { record ->
            if (record.firstHourUsed == -1 && record.lastHourUsed == -1 && record.launchCount == 0 && record.totalTimeInForeground == 0L) {
                Log.d(TAG, "Skipping record for ${record.packageName} on ${dateFormat.format(Date(record.queryStartTime))} due to no significant activity.")
                return@mapNotNull null
            }
            val firstHour = if (record.firstHourUsed != -1) record.firstHourUsed else 0
            val lastHour = if (record.lastHourUsed != -1) record.lastHourUsed else 23
            val consistentLastHour = if (lastHour < firstHour && record.lastHourUsed == -1 && record.firstHourUsed != -1) firstHour else lastHour

            AppUsageDataApi(
                app = record.packageName,
                date = dateFormat.format(Date(record.queryStartTime)),
                firstHour = firstHour,
                lastHour = consistentLastHour,
                launchCount = record.launchCount,
                totalTimeInForeground = record.totalTimeInForeground / 1000
            )
        }
    }

    suspend fun sendDataToTrainModel() {
        withContext(Dispatchers.IO) {
            try {
                val rawRecords: List<AppUsageRecord> = appUsageRecordDao.getAllAppsUsageForApi()
                if (rawRecords.isEmpty()) {
                    Log.w(TAG, "No usage data found to send for training.")
                    return@withContext
                }
                Log.d(TAG, "Fetched ${rawRecords.size} raw records for training.")

                val apiUsageData = transformToAppUsageDataApi(rawRecords)
                if (apiUsageData.isEmpty()) {
                    Log.w(TAG, "No transformed usage data to send (all records might have been filtered or empty initially).")
                    return@withContext
                }
                Log.d(TAG, "Transformed ${apiUsageData.size} records for API.")

                val requestBody = TrainRequestBody(
                    usageData = apiUsageData,
                    epochs = 30,
                    validationSplit = 0.2f
                )

                Log.d(TAG, "Sending training data to the API...")
                val response = apiService.trainModel(requestBody)

                if (response.isSuccessful) {
                    val trainingResponse = response.body()
                    if (trainingResponse != null && trainingResponse.success) {
                        Log.i(TAG, "Training request successful. Model ID: ${trainingResponse.modelId}, Threshold: ${trainingResponse.threshold}")
                    } else {
                        Log.w(TAG, "Training request sent, but API indicated failure or null response. Message: ${response.message()}, Body: ${response.errorBody()?.string()}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "Error sending training data. Code: ${response.code()}, Message: ${response.message()}, Error: $errorBody")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "API Exception when sending training data: ${e.code()} - ${e.message()}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Generic Exception when sending training data: ${e.message}", e)
            }
        }
    }

    suspend fun detectAnomalies(usageRecords: List<AppUsageRecord>): AnomalyDetectionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                if (usageRecords.isEmpty()) {
                    Log.d(TAG, "No usage records to send for anomaly detection.")
                    return@withContext null
                }
                Log.d(TAG, "Transforming ${usageRecords.size} records for anomaly detection.")
                val apiUsageData = transformToAppUsageDataApi(usageRecords)

                if (apiUsageData.isEmpty()) {
                    Log.w(TAG, "No transformed usage data to send for detection.")
                    return@withContext null
                }

                val requestBody = AnomalyDetectionRequest(usageData = apiUsageData)

                Log.i(TAG, "DETECTION REQUEST BODY: $requestBody")

                Log.d(TAG, "Sending detection data to the API...")
                val response = apiService.detectAnomalies(requestBody)

                if (response.isSuccessful) {
                    Log.i(TAG, "Anomaly detection data sent successfully. Response code: ${response.code()}")
                    response.body()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "Error sending detection data. Code: ${response.code()}, Message: ${response.message()}, Error: $errorBody")
                    null
                }
            } catch (e: HttpException) {
                Log.e(TAG, "API Exception during anomaly detection: ${e.code()} - ${e.message()}", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Generic Exception during anomaly detection: ${e.message}", e)
                null
            }
        }
    }
}