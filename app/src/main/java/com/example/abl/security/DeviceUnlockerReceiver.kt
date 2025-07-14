package com.example.abl.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.abl.data.database.dao.TodayUsageDao
import com.example.abl.data.network.AnomalyDetectionRequest
import com.example.abl.data.network.ApiService
import com.example.abl.data.network.AppUsageDataApi
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceUnlockerReceiver: BroadcastReceiver() {

    @Inject
    lateinit var todayUsageDao: TodayUsageDao
    @Inject lateinit var apiService: ApiService

    private val TAG = "DeviceUnlockReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) {
            return
        }

        Log.d(TAG, "Device unlocked. Triggering anomaly detection.")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dailyUsage = todayUsageDao.getAll()
                if (dailyUsage.isEmpty()) {
                    Log.d(TAG, "No usage data accumulated today. Skipping detection.")
                    return@launch
                }

                val apiData = dailyUsage.map {
                    AppUsageDataApi(
                        app = it.packageName,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        firstHour = it.firstHourUsed,
                        lastHour = it.lastHourUsed,
                        launchCount = it.launchCount,
                        totalTimeInForeground = it.totalTimeInForeground / 1000
                    )
                }

                val request = AnomalyDetectionRequest(usageData = apiData)
                val response = apiService.detectAnomalies(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val isAnomaly = response.body()?.results?.any { it.isAnomaly } == true
                    if (isAnomaly) {
                        Log.w(TAG, "ANOMALY DETECTED! Overall Risk: ${response.body()?.overallRiskLevel}")
                        // Here you would post a high-priority notification to the user.
                    } else {
                        Log.i(TAG, "Behavior check passed. No anomalies detected.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform anomaly detection on unlock.", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}