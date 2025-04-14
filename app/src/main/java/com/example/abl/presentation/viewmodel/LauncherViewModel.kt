package com.example.abl.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.utils.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context
): ViewModel() {
    private val _appUsage = MutableStateFlow<List<AppUsageData>>(emptyList())
    val appUsage = _appUsage.asStateFlow()
    private val TAG = "LauncherViewModel"

    fun loadAppUsage() {
        viewModelScope.launch {
            val usageDataList: List<AppUsageData> = UsageStatsHelper
                .getAppUsageData(context)

        }
    }
}