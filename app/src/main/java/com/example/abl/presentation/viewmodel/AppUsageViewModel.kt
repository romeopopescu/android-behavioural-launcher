package com.example.abl.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.domain.repository.AppUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext



@HiltViewModel
class AppUsageViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _appUsageList = MutableStateFlow<List<AppUsageDisplay>>(emptyList())
    val appUsageList: StateFlow<List<AppUsageDisplay>> = _appUsageList.asStateFlow()

    init {
        viewModelScope.launch {
            val flows = appUsageRepository.getAppUsageData()
            val result = mutableListOf<AppUsageDisplay>()
            flows.forEach { flow ->
                flow.collect { usageData ->
                    if (usageData.totalTimeInMinutes > 0 || usageData.totalTimeInHours > 0) {
                        val appName = appUsageRepository.getAllAppsSnapshot().find { it.appId == usageData.appId }?.name ?: "Unknown"
                        result.add(
                            AppUsageDisplay(
                                appName = appName,
                                totalTimeInHours = usageData.totalTimeInHours,
                                totalTimeInMinutes = usageData.totalTimeInMinutes,
                                lastTimeUsed = usageData.lastTimeUsed,
                                firstTimeUsed = usageData.firstTimeUsed,
                                icon = null
                            )
                        )
                    }
                }
            }
            _appUsageList.value = result
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            when (drawable) {
                is BitmapDrawable -> drawable.bitmap.asImageBitmap()
                is AdaptiveIconDrawable -> {
                    val size = 108
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(canvas)
                    bitmap.asImageBitmap()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun syncUsageData(userId: Int = 1) {
        viewModelScope.launch {
            appUsageRepository.syncAndInsertAppUsageData(userId)
            val flows = appUsageRepository.getAppUsageData()
            val allApps = appUsageRepository.getAllAppsSnapshot()
            val packageManager = context.packageManager
            val result = mutableListOf<AppUsageDisplay>()
            flows.forEach { flow ->
                flow.collect { usageData ->
                    if (usageData.totalTimeInMinutes > 0 || usageData.totalTimeInHours > 0) {
                        val appInfo = allApps.find { it.appId == usageData.appId }
                        val appName = appInfo?.name ?: "Unknown"
                        val packageName = appInfo?.packageName
                        val icon: ImageBitmap? = try {
                            if (packageName != null) {
                                val drawable = packageManager.getApplicationIcon(packageName)
                                drawableToImageBitmap(drawable)
                            } else null
                        } catch (e: Exception) { null }
                        result.add(
                            AppUsageDisplay(
                                appName = appName,
                                totalTimeInHours = usageData.totalTimeInHours,
                                totalTimeInMinutes = usageData.totalTimeInMinutes,
                                lastTimeUsed = usageData.lastTimeUsed,
                                firstTimeUsed = usageData.firstTimeUsed,
                                icon = icon
                            )
                        )
                    }
                }
            }
            _appUsageList.value = result
        }
    }

    fun formatTimestamp(timestamp: String): String {
        return try {
            val millis = timestamp.toLong()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(millis))
        } catch (e: Exception) {
            "N/A"
        }
    }
}
data class AppUsageDisplay(
    val appName: String,
    val totalTimeInHours: Long,
    val totalTimeInMinutes: Long,
    val lastTimeUsed: String,
    val firstTimeUsed: String,
    val icon: ImageBitmap?
)