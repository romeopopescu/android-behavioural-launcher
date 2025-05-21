package com.example.abl.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import android.util.Log
import com.example.abl.domain.repository.AppInformationRepository
import com.example.abl.domain.repository.AppUsageRepository

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageRepository: AppUsageRepository,
    private val appInformationRepository: AppInformationRepository
): ViewModel() {
    private val TAG = "LauncherViewModel"

    private val _recommendedApps = MutableStateFlow<List<RecommendedApp>>(emptyList())
    val recommendedApps = _recommendedApps.asStateFlow()

    data class RecommendedApp(val packageName: String, val appName: String, val icon: ImageBitmap?)

    fun loadRecommendedApps() {
        viewModelScope.launch {
            Log.d(TAG, "Loading recommended apps based on timed usage stats.")
            try {
                val topAppSummaries = appUsageRepository.getTopAppsForCurrentTimeSlot(3)
                Log.d(TAG, "Received ${topAppSummaries.size} app summaries from repository.")

                val recs = topAppSummaries.mapNotNull { summary ->
                    val appInfo = appInformationRepository.getAppByPackageName(summary.packageName)
                    if (appInfo != null) {
                        val iconDrawable = appInformationRepository.getAppIcon(summary.packageName)
                        val iconBitmap = iconDrawable?.let { drawableToImageBitmap(it) }
                        Log.d(TAG, "Mapping app: ${appInfo.name}, Icon found: ${iconBitmap != null}")
                        RecommendedApp(appInfo.packageName, appInfo.name, iconBitmap)
                    } else {
                        Log.w(TAG, "AppInfo not found for package: ${summary.packageName}")
                        null
                    }
                }
                _recommendedApps.value = recs
                Log.d(TAG, "Updated recommended apps. Count: ${recs.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recommended apps: ", e)
                _recommendedApps.value = emptyList()
            }
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
            Log.e(TAG, "Error converting drawable to ImageBitmap", e)
            null
        }
    }
}