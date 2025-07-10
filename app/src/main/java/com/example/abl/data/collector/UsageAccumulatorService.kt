package com.example.abl.data.collector

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.abl.data.database.dao.TodayUsageDao
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class UsageAccumulatorService: Service() {

    @Inject lateinit var todayUsageDao: TodayUsageDao
    @Inject lateinit var usageStatsManager: UsageStatsManager

    private val TAG = "AccumulatorService"
    private val NOTIFICATION_CHANNEL_ID = "UsageAccumulatorChannel"
    private val ACCUMULATION_INTERVAL_MS = 10 * 60 * 1000L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun accumulateRecentUsage() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - ACCUMULATION_INTERVAL_MS
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))


    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}