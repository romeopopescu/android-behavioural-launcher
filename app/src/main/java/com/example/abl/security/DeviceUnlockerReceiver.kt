package com.example.abl.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.abl.MainActivity
import com.example.abl.R
import com.example.abl.data.database.dao.TodayUsageDao
import com.example.abl.data.network.AnomalyDetectionRequest
import com.example.abl.data.network.ApiService
import com.example.abl.data.network.AppUsageDataApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DeviceUnlockReceiver : BroadcastReceiver() {

    @Inject
    lateinit var todayUsageDao: TodayUsageDao

    @Inject
    lateinit var apiService: ApiService

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
                    if (response.body()?.results?.any { it.isAnomaly } == true) {
                        Log.w(TAG, "ANOMALY DETECTED! Risk: ${response.body()?.overallRiskLevel}")
                        sendAnomalyNotification(context, response.body()?.overallRiskLevel ?: "UNKNOWN")
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

    private fun sendAnomalyNotification(context: Context, riskLevel: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "AnomalyDetectionChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Anomaly Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a warning icon
            .setContentTitle("Security Alert")
            .setContentText("Suspicious activity detected. Risk level: $riskLevel")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }
}