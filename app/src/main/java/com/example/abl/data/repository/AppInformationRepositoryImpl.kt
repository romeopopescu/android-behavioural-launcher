package com.example.abl.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.model.StateResources
import com.example.abl.domain.repository.AppInformationRepository
import com.example.abl.presentation.viewmodel.AppInformationTest
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
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val currentApps = appInformationDao.getAllAppsSnapshot()
        val currentPackages = currentApps.map { it.packageName }.toSet()

        installedApps.forEach { appInfo ->
            val packageName = appInfo.packageName
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null && (!currentPackages.contains(packageName) || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)) {
                val app = AppInformation(
                    appId = 0, // Will be auto-generated
                    name = appInfo.loadLabel(packageManager).toString(),
                    packageName = packageName
                )
                appInformationDao.insert(app)
            }
        }
        
        val installedPackages = installedApps.map { it.packageName }.toSet()
        currentApps.forEach { app ->
            if (!installedPackages.contains(app.packageName)) {
                appInformationDao.deleteByPackageName(app.packageName)
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

    override suspend fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}