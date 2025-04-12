package com.example.abl.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abl.data.AppRepository
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.utils.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class LauncherViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val context: Context
): ViewModel() {
    private val _appUsage = MutableStateFlow<List<AppUsageData>>(emptyList())
    val appUsage = _appUsage.asStateFlow()
    private val TAG = "LauncherViewModel"

    fun loadAppUsage() {
        viewModelScope.launch {
            val usageDataList: List<AppUsageData> = UsageStatsHelper
                .getAppUsageData(context)

            for (app in usageDataList) {
                appRepository.insertAppUsageData(app)
            }
        }
    }
}
