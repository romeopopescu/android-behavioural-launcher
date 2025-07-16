package com.example.abl.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abl.data.database.entity.AppUsageData
import com.example.abl.utils.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import com.example.abl.data.database.entity.RiskyApp
import com.example.abl.data.repository.AppUsageRepositoryImpl
import com.example.abl.data.repository.RiskyAppRepositoryImpl
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageRepositoryImpl: AppUsageRepositoryImpl,
    private val riskyAppRepositoryImpl: RiskyAppRepositoryImpl
): ViewModel() {
    private val _appUsage = MutableStateFlow<List<AppUsageData>>(emptyList())
    val appUsage: StateFlow<List<AppUsageData>> = _appUsage.asStateFlow()
    private val TAG = "LauncherViewModel"

    data class RecommendedApp(val packageName: String, val appName: String, val icon: ImageBitmap?)
    private val _recommendedApps = MutableStateFlow<List<RecommendedApp>>(emptyList())
    val recommendedApps: StateFlow<List<RecommendedApp>> = _recommendedApps.asStateFlow()

    data class RiskyAppDisplay(val packageName: String, val appName: String, val riskScore: Int, val icon: ImageBitmap?)
    private val _riskyApps = MutableStateFlow<List<RiskyAppDisplay>>(emptyList())
    val riskyApps: StateFlow<List<RiskyAppDisplay>> = _riskyApps.asStateFlow()

    fun loadAppUsage() {
        viewModelScope.launch {
            val usageDataList: List<AppUsageData> = UsageStatsHelper.getAppUsageData(context)
            _appUsage.value = usageDataList
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadRiskyApps() {
        viewModelScope.launch {
            try {
                riskyAppRepositoryImpl.insertRiskyApps()
            } catch (e: NotImplementedError) {
                Log.w(TAG, "riskyAppRepositoryImpl.insertRiskyApps() is not yet implemented. Skipping.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error calling riskyAppRepositoryImpl.insertRiskyApps()", e)
            }

            val riskyAppEntities = riskyAppRepositoryImpl.getTopRiskyApps().firstOrNull() ?: emptyList()
            val pm = context.packageManager
            val displayableRiskyApps = riskyAppEntities.mapNotNull { entity ->
                try {
                    val appInfo = pm.getApplicationInfo(entity.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val iconDrawable = pm.getApplicationIcon(entity.packageName)
                    val iconBitmap = drawableToImageBitmap(iconDrawable)
                    RiskyAppDisplay(
                        packageName = entity.packageName,
                        appName = appName,
                        riskScore = entity.riskScore,
                        icon = iconBitmap
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e(TAG, "App not found while loading risky app: ${entity.packageName}", e)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading icon or name for risky app: ${entity.packageName}", e)
                    val appNameFallback = try { pm.getApplicationLabel(pm.getApplicationInfo(entity.packageName, 0)).toString() } catch (e2: Exception) { entity.packageName }
                    RiskyAppDisplay(
                        packageName = entity.packageName,
                        appName = appNameFallback,
                        riskScore = entity.riskScore,
                        icon = null
                    )
                }
            }
            _riskyApps.value = displayableRiskyApps
        }
    }

    //    init {
//        loadAppUsage()
//        loadRecommendedApps()
//        loadRiskyApps()
//    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadRecommendedApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val topApps = appUsageRepositoryImpl.getTopAppsThisHour(3)
            val recs = topApps.map { (packageName, appNamePair) ->
                val icon: ImageBitmap? = try {
                    val drawable = pm.getApplicationIcon(packageName)
                    drawableToImageBitmap(drawable)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Icon not found for recommended app: $packageName", e)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading icon for recommended app: $packageName", e)
                    null
                }
                RecommendedApp(packageName, appNamePair, icon)
            }
            _recommendedApps.value = recs
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
}