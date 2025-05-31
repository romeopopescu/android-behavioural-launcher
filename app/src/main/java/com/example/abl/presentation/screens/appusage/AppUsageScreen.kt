package com.example.abl.presentation.screens.appusage

import android.R
import android.R.attr.onClick
import android.app.slice.Slice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.abl.presentation.viewmodel.AppUsageViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.graphics.drawable.toBitmap
import android.app.usage.UsageStatsManager
import com.example.abl.presentation.viewmodel.AppUsageDisplay
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(
    onNavigateToMain: () -> Unit
//    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val calendar = remember { Calendar.getInstance() }
    val endTime = remember { calendar.timeInMillis }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startTime = remember { calendar.timeInMillis }
    val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val usageStatsList = remember(startTime, endTime) { // startTime and endTime are stable for this composition
        usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
    }
    val myPackageName = context.packageName

    val appUsages = remember(usageStatsList, packageManager, myPackageName) {
        val processedStats = usageStatsList
            .filter { it.totalTimeInForeground > 0 && it.packageName != myPackageName }
            .mapNotNull { usageStats ->
                try {
                    val appInfo = packageManager.getApplicationInfo(usageStats.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val iconDrawable = packageManager.getApplicationIcon(usageStats.packageName)
                    val iconBitmap = drawableToImageBitmap(iconDrawable)

                    // Anonymous object to hold intermediate data
                    object {
                        val appName = appName
                        // val packageName = usageStats.packageName // Kept for potential future use (e.g. unique keys)
                        val totalTimeInForeground = usageStats.totalTimeInForeground
                        val lastTimeUsed = usageStats.lastTimeUsed
                        val firstTimeStamp = usageStats.firstTimeStamp
                        val icon = iconBitmap
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    null // Skip if app info cannot be fetched (e.g., app uninstalled during processing)
                }
            }

        // Group by appName and then select the entry with max usage within each group
        processedStats
            .groupBy { it.appName }
            .mapNotNull { (appName, group) -> // group is List of the anonymous objects
                // Find the entry with the maximum totalTimeInForeground in this group
                val primaryEntry = group.maxByOrNull { it.totalTimeInForeground }

                primaryEntry?.let { entry ->
                    val totalTime = entry.totalTimeInForeground // Use time from the primary entry
                    val totalMinutes = (totalTime / 60000) % 60
                    val totalHours = totalTime / 3600000

                    AppUsageDisplay(
                        appName = appName, // appName is the group key
                        totalTimeInHours = totalHours,
                        totalTimeInMinutes = totalMinutes,
                        lastTimeUsed = entry.lastTimeUsed.toString(), // Use from primary entry
                        firstTimeUsed = entry.firstTimeStamp.toString(), // Use from primary entry
                        icon = entry.icon // Use icon from primary entry
                    )
                }
            }
    }
    val totalMinutes = appUsages.sumOf { it.totalTimeInHours * 60 + it.totalTimeInMinutes }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("App Usage Time in Past 24hrs", fontWeight = FontWeight.Bold) })
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Most used apps",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Divider()
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appUsages.sortedByDescending { it.totalTimeInHours * 60 + it.totalTimeInMinutes }) { usage ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // App icon
                    val iconBitmap = usage.icon
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = usage.appName,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(usage.appName, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${usage.totalTimeInHours}h ${usage.totalTimeInMinutes}m",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        // Progress bar
                        val percent = if (totalMinutes > 0) (usage.totalTimeInHours * 60 + usage.totalTimeInMinutes).toFloat() / totalMinutes else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percent)
                                    .height(6.dp)
                                    .background(Color(0xFF3DDC84), shape = MaterialTheme.shapes.small)
                            )
                        }
                    }
                }
                Divider()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
    return try {
        when (drawable) {
            is BitmapDrawable -> drawable.bitmap.asImageBitmap()
            is AdaptiveIconDrawable -> {
                val size = 108 // px, adjust as needed
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