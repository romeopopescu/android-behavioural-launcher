package com.example.abl.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.dao.TodayUsageDao
import com.example.abl.data.database.entity.AppUsageRecord
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*

@HiltWorker
class DailyRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val todayUsageDao: TodayUsageDao,
    private val historicalDao: AppUsageRecordDao
) : CoroutineWorker(context, params) {

    private val TAG = "DailyRolloverWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily rollover process.")
            val todayRecords = todayUsageDao.getAll()

            if (todayRecords.isNotEmpty()) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, -1)
                val yesterdayTimestamp = calendar.timeInMillis

                val historicalRecords = todayRecords.map {
                    AppUsageRecord(
                        packageName = it.packageName,
                        queryStartTime = yesterdayTimestamp,
                        queryEndTime = yesterdayTimestamp,
                        totalTimeInForeground = it.totalTimeInForeground,
                        recordedAt = System.currentTimeMillis(),
                        launchCount = it.launchCount,
                        dayOfWeekUsed = it.dayOfWeek,
                        firstHourUsed = it.firstHourUsed,
                        lastHourUsed = it.lastHourUsed
                    )
                }
                historicalDao.insertAll(historicalRecords)
                Log.d(TAG, "Archived ${historicalRecords.size} records to historical usage.")
            }

            todayUsageDao.clearAll()
            Log.d(TAG, "Cleared today's usage accumulator.")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Daily rollover failed.", e)
            Result.retry()
        }
    }
}