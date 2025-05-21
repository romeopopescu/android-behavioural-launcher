package com.example.abl.workers

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.abl.domain.repository.AppUsageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log

@HiltWorker
class DetailedUsageStatsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appUsageRepository: AppUsageRepository
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.P) // Ensure this aligns with the repository method's requirement
    override suspend fun doWork(): Result {
        Log.d("DetailedUsageStatsWorker", "Starting detailed usage stats collection.")
        return try {
            appUsageRepository.recordDetailedUsageStats()
            Log.d("DetailedUsageStatsWorker", "Detailed usage stats collection successful.")
            Result.success()
        } catch (e: Exception) {
            Log.e("DetailedUsageStatsWorker", "Error collecting detailed usage stats", e)
            Result.failure()
        }
    }
} 