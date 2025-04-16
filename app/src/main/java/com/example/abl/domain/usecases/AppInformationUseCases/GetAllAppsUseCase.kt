package com.example.abl.domain.usecases.AppInformationUseCases

import com.example.abl.data.database.entity.AppInformation
import com.example.abl.data.model.StateResources
import com.example.abl.domain.repository.AppInformationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAppsUseCase @Inject constructor(
    private val appInformationRepository: AppInformationRepository
) {
    suspend operator fun invoke(): Flow<List<AppInformation>> {
        return appInformationRepository.getAllApps()
    }
}