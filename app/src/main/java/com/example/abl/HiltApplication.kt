package com.example.abl

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.abl.data.worker.DailyRolloverWorker
import com.example.abl.data.worker.TrainingWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication: Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun scheduleDailyRollover() {
        // Define constraints for the work
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Only run when battery is not low
            .build()

        // Create a periodic request to run once a day
        val rolloverRequest =
            PeriodicWorkRequestBuilder<DailyRolloverWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        // Enqueue the work as unique periodic work.
        // 'KEEP' policy ensures that if the work is already scheduled, it is not replaced.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyRolloverWork",
            ExistingPeriodicWorkPolicy.KEEP,
            rolloverRequest
        )
    }

}