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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.repository.AppUsageRepositoryImpl

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageRepositoryImpl: AppUsageRepositoryImpl
): ViewModel() {
    private val _appUsage = MutableStateFlow<List<AppUsageData>>(emptyList())
    val appUsage = _appUsage.asStateFlow()
    private val TAG = "LauncherViewModel"

    private val _recommendedApps = MutableStateFlow<List<RecommendedApp>>(emptyList())
    val recommendedApps = _recommendedApps.asStateFlow()

    data class RecommendedApp(val packageName: String, val appName: String, val icon: ImageBitmap?)

    fun loadAppUsage() {
        viewModelScope.launch {
            val usageDataList: List<AppUsageData> = UsageStatsHelper
                .getAppUsageData(context)

        }
    }

    fun loadRecommendedApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val topApps = appUsageRepositoryImpl.getTopAppsThisHour(3)
            val recs = topApps.map { (packageName, appName) ->
                val icon: ImageBitmap? = try {
                    val drawable = pm.getApplicationIcon(packageName)
                    drawableToImageBitmap(drawable)
                } catch (e: Exception) { null }
                RecommendedApp(packageName, appName, icon)
            }
            _recommendedApps.value = recs
        }
    }

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
}