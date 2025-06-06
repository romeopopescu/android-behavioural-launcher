package com.example.abl

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
        scheduleTrainingJobs()
    }

    private fun scheduleTrainingJobs() {
        val workManager = WorkManager.getInstance(this)
        val prefs = getSharedPreferences("abl_prefs", Context.MODE_PRIVATE)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule the recurring weekly training
        val weeklyTrainingRequest =
            PeriodicWorkRequestBuilder<TrainingWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "WeeklyTrainingWork",
            ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if it's already scheduled
            weeklyTrainingRequest
        )
        Log.d("AblApplication", "Weekly training worker scheduled.")


        // If this is the first time the app is run, schedule an immediate training job
        val isInitialJobScheduled = prefs.getBoolean("initial_training_scheduled", false)
        if (!isInitialJobScheduled) {
            val initialTrainingRequest = OneTimeWorkRequestBuilder<TrainingWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                "InitialTrainingWork",
                ExistingWorkPolicy.KEEP,
                initialTrainingRequest
            )
            Log.d("AblApplication", "Initial one-time training worker scheduled.")
            prefs.edit().putBoolean("initial_training_scheduled", true).apply()
        }
    }
}