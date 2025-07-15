package com.example.abl.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.database.dao.AppUsageRecordDao
import com.example.abl.data.database.entity.AppUsageRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class StatisticsUiState(
    val mostUsedApp: String = "Calculating...",
    val totalScreenTime: String = "Calculating...",
    val totalAppLaunches: String = "Calculating...",
    val mostActiveDay: String = "Calculating..."
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val appUsageRecordDao: AppUsageRecordDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            val records: List<AppUsageRecord> = appUsageRecordDao.getAllAppsUsageForApi()

            if (records.isEmpty()) {
                _uiState.value = StatisticsUiState(
                    mostUsedApp = "No Data",
                    totalScreenTime = "0h 0m",
                    totalAppLaunches = "0",
                    mostActiveDay = "No Data"
                )
                return@launch
            }

            val totalMillis = records.sumOf { it.totalTimeInForeground }
            val formattedTime = formatDuration(totalMillis)

            val totalLaunches = records.sumOf { it.launchCount }.toString()

            val mostUsedAppPackage = records
                .groupBy { it.packageName }
                .mapValues { (_, appRecords) -> appRecords.sumOf { it.totalTimeInForeground } }
                .maxByOrNull { it.value }?.key
            val mostUsedAppName = getAppName(mostUsedAppPackage)

            val mostActiveDay = records
                .groupBy { getDayFromTimestamp(it.queryStartTime) }
                .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.totalTimeInForeground } }
                .maxByOrNull { it.value }?.key ?: "N/A"

            _uiState.value = StatisticsUiState(
                mostUsedApp = mostUsedAppName,
                totalScreenTime = formattedTime,
                totalAppLaunches = totalLaunches,
                mostActiveDay = mostActiveDay
            )
        }
    }

    private fun getDayFromTimestamp(timestamp: Long): String {
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        return dayFormat.format(Date(timestamp))
    }

    private fun formatDuration(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        return "${hours}h ${minutes}m"
    }

    private fun getAppName(packageName: String?): String {
        if (packageName == null) return "N/A"
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}