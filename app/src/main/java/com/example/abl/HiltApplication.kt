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

    override fun onCreate() {
        super.onCreate()
        scheduleInitialTraining()
        scheduleDailyRollover()
        scheduleWeeklyTraining()
    }

    private fun scheduleInitialTraining() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isInitialTrainingScheduled = prefs.getBoolean("initial_training_scheduled", false)

        if (!isInitialTrainingScheduled) {
            Log.i("HiltApplication", "First launch detected. Scheduling initial model training.")

            val trainingRequest = OneTimeWorkRequestBuilder<TrainingWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "InitialTrainingWork",
                ExistingWorkPolicy.KEEP, // KEEP ensures it only runs once
                trainingRequest
            )

            prefs.edit().putBoolean("initial_training_scheduled", true).apply()
        }
    }


    private fun scheduleDailyRollover() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val rolloverRequest =
            PeriodicWorkRequestBuilder<DailyRolloverWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyRolloverWork",
            ExistingPeriodicWorkPolicy.KEEP,
            rolloverRequest
        )
    }

    private fun scheduleWeeklyTraining() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        val trainingRequest =
            PeriodicWorkRequestBuilder<TrainingWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklyTrainingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            trainingRequest
        )

        Log.i("HiltApplication", "Weekly model training has been scheduled.")
    }
}