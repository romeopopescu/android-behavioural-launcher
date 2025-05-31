package com.example.abl.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.dao.RiskyAppDao
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.database.entity.RiskyApp
import com.example.abl.domain.repository.RiskyAppRepository
import com.example.abl.utils.PermissionRiskScorer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskyAppRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val riskyAppDao: RiskyAppDao,
    private val appInformationDao: AppInformationDao
): RiskyAppRepository {

    private val packageManager: PackageManager = context.packageManager
    override suspend fun insertRiskyApps() {
        val allApps = appInformationDao.getAllAppsSnapshot()

        allApps.forEach { appInfo ->
            var riskScore = 0
            try {
                val packageInfo: PackageInfo = packageManager.getPackageInfo(appInfo.packageName,
                    PackageManager.GET_PERMISSIONS)
                val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

                requestedPermissions.forEach { permission ->
                    riskScore += PermissionRiskScorer.permissionScores[permission] ?: 0
                }

                val existingRiskyApp = riskyAppDao.getRiskyAppByAppId(appInfo.appId)

                if (riskScore > 0) {
                    if (existingRiskyApp != null) {
                        // Update existing entry only if score changed to avoid unnecessary writes
                        if (existingRiskyApp.riskScore != riskScore) {
                            riskyAppDao.insert(existingRiskyApp.copy(riskScore = riskScore))
                        }
                    } else {
                        // Insert new entry
                        riskyAppDao.insert(RiskyApp(
                            id = 0, // id is auto-generated, so 0 is fine for insertion
                            appId = appInfo.appId,
                            packageName = appInfo.packageName,
                            riskScore = riskScore
                        ))
                    }
                } else {
                    // Risk score is 0, remove if it exists
                    if (existingRiskyApp != null) {
                        riskyAppDao.deleteByAppId(appInfo.appId)
                    }
                }

            } catch (e: PackageManager.NameNotFoundException) {
                // App not found, likely uninstalled, ensure it's removed from RiskyApp table
                riskyAppDao.deleteByAppId(appInfo.appId)
            } catch (e: Exception) {
                // Handle other potential exceptions during permission checking or DB operation
                // Log the error or handle as appropriate for your app
                // For now, we'll just print to stack trace as an example, but you might want more robust logging
                e.printStackTrace()
            }
        }
    }

    override fun getTopRiskyApps(): Flow<List<RiskyApp>> {
        return riskyAppDao.getTop3RiskyAppsFlow()
    }
}