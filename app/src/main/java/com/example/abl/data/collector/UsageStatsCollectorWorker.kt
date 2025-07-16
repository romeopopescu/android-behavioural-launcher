package com.example.abl.data.collector

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UsageStatsCollectorWorker @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }

}