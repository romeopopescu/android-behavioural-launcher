package com.example.abl

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.abl.workers.DetailedUsageStatsWorker // Ensure this path is correct
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        setupPeriodicUsageStatsCollection()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            // Optionally, you can set a minimum logging level for WorkManager
            // .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    private fun setupPeriodicUsageStatsCollection() {
        val usageStatsWorkRequest =
            PeriodicWorkRequestBuilder<DetailedUsageStatsWorker>(1, TimeUnit.HOURS) // Run every hour
                // You can add constraints here (e.g., device charging, network type)
                // Example: .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DetailedUsageStatsCollectionWorker", // Unique name for the work
            ExistingPeriodicWorkPolicy.KEEP, // Or REPLACE if you want to update the worker definition
            usageStatsWorkRequest
        )
    }
}