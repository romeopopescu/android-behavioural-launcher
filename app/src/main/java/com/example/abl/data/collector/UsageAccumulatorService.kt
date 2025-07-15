package com.example.abl.data.collector

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.abl.R
import com.example.abl.data.database.dao.TodayUsageDao
import com.example.abl.data.database.entity.TodayUsage
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class UsageAccumulatorService: Service() {

    @Inject lateinit var todayUsageDao: TodayUsageDao
    @Inject lateinit var usageStatsManager: UsageStatsManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val TAG = "AccumulatorService"
    private val NOTIFICATION_CHANNEL_ID = "UsageAccumulatorChannel"
    private val ACCUMULATION_INTERVAL_MS = 10 * 60 * 1000L
    private val NOTIFICATION_ID = 1


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        Log.d(TAG, "Service starting data accumulation loop.")

        scope.launch {
            while (isActive) {
                accumulateRecentUsage()
                delay(ACCUMULATION_INTERVAL_MS)
            }
        }
        return START_STICKY
    }


    private suspend fun accumulateRecentUsage() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - ACCUMULATION_INTERVAL_MS
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (stats.isNullOrEmpty()) return

            Log.d(TAG, "Found ${stats.size} usage stats entries in the last interval.")
            for (usageStat in stats) {
                if (usageStat.totalTimeInForeground <= 0) continue

                val existingRecord = todayUsageDao.getUsageForApp(usageStat.packageName)

                if (existingRecord != null) {
                    existingRecord.totalTimeInForeground += usageStat.totalTimeInForeground
                    existingRecord.lastHourUsed = getHourFromTimestamp(usageStat.lastTimeUsed, calendar)
                    todayUsageDao.insertOrUpdate(existingRecord)
                } else {
                    val newRecord = TodayUsage(
                        packageName = usageStat.packageName,
                        launchCount = 1,
                        totalTimeInForeground = usageStat.totalTimeInForeground,
                        firstHourUsed = getHourFromTimestamp(usageStat.firstTimeStamp, calendar),
                        lastHourUsed = getHourFromTimestamp(usageStat.lastTimeUsed, calendar),
                        dayOfWeek = currentDayOfWeek
                    )
                    todayUsageDao.insertOrUpdate(newRecord)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accumulating usage stats.", e)
        }
    }

    private fun getHourFromTimestamp(timestamp: Long, calendar: Calendar): Int {
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Usage Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        // You can add an intent here to open your app when the notification is tapped
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Behavioral Launcher Active")
            .setContentText("Securing your device by learning your usage patterns.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}