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
                        if (existingRiskyApp.riskScore != riskScore) {
                            riskyAppDao.insert(existingRiskyApp.copy(riskScore = riskScore))
                        }
                    } else {
                        riskyAppDao.insert(RiskyApp(
                            id = 0,
                            appId = appInfo.appId,
                            packageName = appInfo.packageName,
                            riskScore = riskScore
                        ))
                    }
                } else {
                    if (existingRiskyApp != null) {
                        riskyAppDao.deleteByAppId(appInfo.appId)
                    }
                }

            } catch (e: PackageManager.NameNotFoundException) {
                riskyAppDao.deleteByAppId(appInfo.appId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getTopRiskyApps(): Flow<List<RiskyApp>> {
        return riskyAppDao.getTop3RiskyAppsFlow()
    }
}