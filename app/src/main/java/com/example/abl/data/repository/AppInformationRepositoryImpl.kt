package com.example.abl.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.health.connect.datatypes.AppInfo
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.model.StateResources
import com.example.abl.domain.repository.AppInformationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInformationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appInformationDao: AppInformationDao
) : AppInformationRepository {

    override fun getAllApps(): Flow<List<AppInformation>> {
        return appInformationDao.getAllApps()
    }

    override suspend fun syncApps() {
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = appInformationDao.getAllAppsSnapshot()
        var isFound = false

        packageManager.queryIntentActivities(intent, 0).map {
            val app = AppInformation(
                appId = 0,
                name = it.activityInfo.loadLabel(packageManager).toString(),
                packageName = it.activityInfo.packageName
            )
            for (application in apps) {
                if (application.packageName.equals(app.packageName)) {
                    isFound = true
                }
            }
            if (!isFound) {
                appInformationDao.insert(app)
            } else {
                isFound = false
            }
        }
    }

    override fun getAppIcon(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override suspend fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}