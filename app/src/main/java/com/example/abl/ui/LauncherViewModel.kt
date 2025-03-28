package com.example.abl.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.utils.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherViewModel(private val context: Context): ViewModel() {
    private val _appUsage = MutableStateFlow<List<AppUsageData>>(emptyList())
    val appUsage = _appUsage.asStateFlow()
    private val TAG = "LauncherViewModel"

    init {
        loadAppUsage()
    }

    private fun loadAppUsage() {
        val appUsage = UsageStatsHelper
            .getAppUsageData(context)
        _appUsage.value = appUsage
        Log.d(TAG, "AppUsageList: $appUsage")
    }
}
