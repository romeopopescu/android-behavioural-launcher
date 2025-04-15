package com.example.abl.data.repository

import com.example.abl.data.database.dao.AppInformationDao
import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.model.StateResources
import com.example.abl.domain.repository.AppInformationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInformationRepositoryImpl @Inject constructor(
    private val appInformationDao: AppInformationDao
) : AppInformationRepository{
    override fun getAllApps(): Flow<StateResources<List<AppInformation>>> = flow {
        emit(StateResources.Loading)
        appInformationDao.getAllApps().collect { apps ->
            emit(StateResources.Success(apps))
        }
    }.catch { e ->
        emit(StateResources.Error(e.localizedMessage ?: "Unknown error!"))
    }

    override suspend fun insert(app: AppInformation) {
        try {
            appInformationDao.insert(app)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getAppById(id: Int): AppInformation {
        try {
            return appInformationDao.getAppById(id)
        } catch (e: Exception) {
            throw e
        }
    }
}